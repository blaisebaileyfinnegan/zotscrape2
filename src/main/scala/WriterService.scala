import akka.actor._
import scala.concurrent.Future
import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.concurrent.ExecutionContext.Implicits.global

object WriterService {
  case object CreateSchema
  case object WriteHistory
  case object Done
  case object Ready

  case class WriteDocument(quarter: String, department: String, websoc: WebSoc)
  case class DocumentDone(quarter: String, department: String, failed: Boolean = false)
}

class WriterService extends Actor with ActorLogging {
  import Manager._
  import WriterService._
  import Writer._

  var timestamp: Option[java.sql.Timestamp] = None
  var awaiting: Long = 0
  var collectorDone: Boolean = false
  implicit var session: Option[MySQLDriver.backend.Session] = None

  def receive = {
    case CollectorService.Done => {
      collectorDone = true
      if (awaiting == 0) context.parent ! Done
    }

    case CreateSchema => session.get asDynamicSession {
      implicit val _ = session.get
      (Schema.history.ddl ++
        Schema.terms.ddl ++
        Schema.schools.ddl ++
        Schema.departments.ddl ++
        Schema.courses.ddl ++
        Schema.sections.ddl ++
        Schema.restrictions.ddl ++
        Schema.sectionRestrictions.ddl ++
        Schema.meetings.ddl ++
        Schema.finals.ddl ++
        Schema.instructors.ddl ++
        Schema.enrollments.ddl).create

      self ! WriteHistory
    }

    case WriteHistory => session.get asDynamicSession {
      implicit val _ = session.get

      val timestamp = this.timestamp.getOrElse(throw new Error("Timestamp is none."))
      if (Schema.history.where(_.timestamp === timestamp).exists.run) {
        throw new Error("Timestamp " + timestamp + " already found in database. Cannot insert.")
      } else {
        log.info("Writing " + timestamp + " to history.")
        insertHistory(timestamp)

        context.parent ! Ready
      }
    }

    case WriteDocument(quarter, department, websoc) => session.get asDynamicSession {
      awaiting += 1
      log.info("Saving " + department + " in " + quarter + ".")
      implicit val _ = session.get

      if (websoc.term.isEmpty || websoc.courseList.isEmpty) {
        self ! DocumentDone(quarter, department, failed = true)
      } else {
        val termIdInsertion = Writer.insertIgnoreTerm(websoc.term.get)
        val restrictions = Future.sequence(websoc.codes.getOrElse(Seq.empty).map(insertIgnoreRestriction))

        val schools = websoc.courseList.get.schools
        val schoolIdsToSchool = Future.sequence(schools map {
          school => Writer.insertIgnoreSchool(school) map ((_, school))
        })

        val departmentIdsToDepartment = schoolIdsToSchool map {
          _.flatMap {
            case (schoolId, school) => school.departments.map {
              department => {
                Writer.insertIgnoreDepartment(department)(schoolId) map { (_, department) }
              }
            }
          }
        } flatMap { f => Future.sequence(f) }

        val courseIdsToCourse = departmentIdsToDepartment flatMap {
          seq => Future.traverse(seq) {
            case (departmentId, dept) => Future.traverse(dept.courses) {
              course => {
                Writer.insertIgnoreCourse(course)(departmentId) map { (_, course) }
              }
            }
          }.map(_.flatten)
        }

        termIdInsertion onSuccess {
          case termId => {
            val sectionIdsToSection = courseIdsToCourse flatMap {
              seq => Future.traverse(seq) {
                case (courseId, course) => Future.traverse(course.sections) {
                  section => {
                    Writer.insertSection(section)(courseId, termId, this.timestamp.get) map { (_, section) }
                  }
                }
              }.map(_.flatten)
            }

            restrictions andThen {
              case _ => {
                sectionIdsToSection foreach (_.foreach {
                  case (id, section) => section.restrictions.foreach(_.foreach {
                    r => insertSectionRestriction(id, this.timestamp.get)(r)
                  })
                })
              }
            } andThen {
              case _ => {
                sectionIdsToSection foreach (_.foreach {
                  case (id, section) =>
                    Future.sequence(List(Future.traverse(section.meetings)(m => insertMeeting(m)(id, this.timestamp.get)),
                    section.secFinal.map(f => insertFinal(f)(id, this.timestamp.get)).getOrElse(Future(None)),
                    Future.traverse(section.instructors)(i => insertInstructor(i)(id, this.timestamp.get)),
                    section.enrollment.map(e => insertEnrollment(e)(id, this.timestamp.get)).getOrElse(Future(None)))) onComplete {
                      case _ => {
                        self ! DocumentDone(quarter, department)
                      }
                    }
                })
              }
            }
          }
        }
      }
    }

    case DocumentDone(quarter, department, failed) => {
      awaiting -= 1

      if (failed)
        log.error(department + " in " + quarter + " failed.")
      else
        log.info(department + " in " + quarter + " saved.")

      if (awaiting == 0 && collectorDone) context.parent ! Done
    }

    case StartWriterService(t) => {
      this.timestamp = Some(t)

      session = Some(MySQLDriver.simple.Database.forURL("jdbc:mysql://localhost/zotscrape2", "root").createSession())
      implicit val _ = session.get
      session.get asDynamicSession {
        if (MTable.getTables("history").list().isEmpty)
          self ! CreateSchema
        else
          self ! WriteHistory
      }
    }

    case unrecognized => log.error("Unexpected message " + unrecognized + " sent to writer service.")
  }
}
