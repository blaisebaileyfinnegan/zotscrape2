package zotscrape.writer

import akka.actor._
import zotscrape.catalogue.{CatalogueInfo, CatalogueService}
import zotscrape.collector.CollectorService
import zotscrape.WebSoc

import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.meta.MTable

object WriterService {
  case object CreateSchema
  case object Done
  case object Ready
  case object Start
  case object TryShutdown
  case object WriteHistory

  case class DocumentDone(quarter: String, department: String, failed: Boolean)
}

class WriterService(jdbcUrl: String,
                    username: String,
                    password: String,
                    timestamp: java.sql.Timestamp) extends Actor with ActorLogging {
  import zotscrape.writer.Writer._

  var awaiting: Long = 0
  var pendingCatalogueInfos: List[CatalogueInfo] = Nil
  var canShutdown: Boolean = false

  implicit val session: MySQLDriver.backend.Session =
    MySQLDriver.simple.Database.forURL(jdbcUrl, username, password).createSession()

  def receive = {
    case WriterService.TryShutdown => {
      if (awaiting == 0 && canShutdown && pendingCatalogueInfos.isEmpty) {
        log.info("Shutting down writer service.")
        context.parent ! WriterService.Done
      }
    }

    case CollectorService.Done => {
      canShutdown = true
      self ! WriterService.TryShutdown
    }

    case CatalogueService.Done => {
      canShutdown = true
      self ! WriterService.TryShutdown
    }

    case CatalogueInfo(name, description) => {
      log.info("Received " + CatalogueInfo(name, description))
    }

    case WriterService.CreateSchema => session asDynamicSession {
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

      self ! WriterService.WriteHistory
    }

    case WriterService.WriteHistory => session asDynamicSession {
      if (Schema.history.filter(_.timestamp === timestamp).exists.run) {
        throw new Error("Timestamp " + timestamp + " already found in database. Cannot insert.")
      } else {
        log.info("Writing " + timestamp + " to history.")
        insertHistory(timestamp)

        context.parent ! WriterService.Ready
      }
    }

    case CollectorService.Document(quarter, department, websoc) => session asDynamicSession {
      awaiting += 1

      if (websoc.term.isEmpty) {
        self ! WriterService.DocumentDone(quarter, department, failed = true)
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
            section.restrictions foreach (_.foreach { restriction => insertSectionRestriction(restriction)(sectionId) })
            section.meetings filter { m =>
              m.time.nonEmpty &&
              m.building.nonEmpty &&
              m.room.nonEmpty
            } foreach { meeting => insertMeeting(meeting)(sectionId) }
            section.secFinal filter (_.date.nonEmpty) foreach { finale => insertFinal(finale)(sectionId) }
            section.instructors foreach { instructor => insertInstructor(instructor)(sectionId) }
            section.enrollment foreach { enrollment => insertEnrollment(enrollment)(sectionId) }
        }

        self ! WriterService.DocumentDone(quarter, department, failed = false)
      }
    }

    case WriterService.DocumentDone(quarter, department, failed) => {
      awaiting -= 1

      log.info("Saved " + department + " in " + quarter)
      if (failed) {
        log.error(department + " in " + quarter + " failed.")
      }

      self ! WriterService.TryShutdown
    }

    case WriterService.Start => session asDynamicSession {
      if (MTable.getTables("history").list.isEmpty) {
        self ! WriterService.CreateSchema
      } else {
        self ! WriterService.WriteHistory
      }
    }

  }
}
