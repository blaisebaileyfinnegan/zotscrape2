package zotscrape

import akka.actor._
import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable
import scala.concurrent._
import ExecutionContext.Implicits.global

object WriterService {
  case object CreateSchema
  case object WriteHistory
  case object Done
  case object Ready
  case object TryShutdown

  case class WriteDocument(quarter: String, department: String, websoc: WebSoc)
  case class DocumentDone(quarter: String, department: String, failed: Boolean)
}

class WriterService(jdbcUrl: String, username: String, password: String, timestamp: java.sql.Timestamp) extends Actor with ActorLogging {
  import Manager._
  import WriterService._
  import Writer._

  var awaiting: Long = 0
  var collectorDone: Boolean = false
  implicit val session: MySQLDriver.backend.Session =
    MySQLDriver.simple.Database.forURL(jdbcUrl, username, password).createSession()

  def receive = {
    case TryShutdown => if (awaiting == 0 && collectorDone) context.parent ! Done

    case CollectorService.Done =>
      collectorDone = true
      self ! TryShutdown

    case CreateSchema => session asDynamicSession {
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

    case WriteHistory => session asDynamicSession {
      if (Schema.history.where(_.timestamp === timestamp).exists.run) {
        throw new Error("Timestamp " + timestamp + " already found in database. Cannot insert.")
      } else {
        log.info("Writing " + timestamp + " to history.")
        insertHistory(timestamp)

        context.parent ! Ready
      }
    }

    case WriteDocument(quarter, department, websoc) => session asDynamicSession Future {
      awaiting += 1
      log.info("Saving " + department + " in " + quarter + ".")

      if (websoc.term.isEmpty) {
        self ! DocumentDone(quarter, department, failed = true)
      } else {
        val termId = Writer.insertIgnoreTerm(websoc.term.get)
        websoc.codes.getOrElse(Seq.empty).foreach(insertIgnoreRestriction)

        val schools = websoc.courseList.getOrElse(WebSoc.CourseList(Seq.empty)).schools
        val schoolMap = schools map { school => (Writer.insertIgnoreSchool(school), school) }
        val departmentMap = schoolMap flatMap {
          case (schoolId, school) => school.departments.map {
            dept => (Writer.insertIgnoreDepartment(dept)(schoolId), dept)
          }
        }

        val courseMap = departmentMap flatMap {
          case (deptId, dept) => dept.courses.map {
            course => (Writer.insertIgnoreCourse(course)(deptId), course)
          }
        }

        val sectionMap = courseMap flatMap {
          case (courseId, course) => course.sections.map {
            section => (Writer.insertSection(section)(courseId, termId, timestamp), section)
          }
        }

        sectionMap foreach {
          case (sectionId, section) =>
            section.restrictions foreach (_.foreach { restriction => insertSectionRestriction(sectionId, timestamp)(restriction) })
            section.meetings foreach { meeting => insertMeeting(meeting)(sectionId, timestamp) }
            section.secFinal foreach { finale => insertFinal(finale)(sectionId, timestamp) }
            section.instructors foreach { instructor => insertInstructor(instructor)(sectionId, timestamp) }
            section.enrollment foreach { enrollment => insertEnrollment(enrollment)(sectionId, timestamp) }
        }

        self ! DocumentDone(quarter, department, failed = false)
      }
    }

    case DocumentDone(quarter, department, failed) =>
      awaiting -= 1

      if (failed)
        log.error(department + " in " + quarter + " failed.")
      else
        log.info(department + " in " + quarter + " saved.")

      self ! TryShutdown


    case StartWriterService => session asDynamicSession {
      if (MTable.getTables("history").list().isEmpty) self ! CreateSchema
      else self ! WriteHistory
    }


    case unrecognized => log.error("Unexpected message " + unrecognized + " sent to writer service.")
  }
}