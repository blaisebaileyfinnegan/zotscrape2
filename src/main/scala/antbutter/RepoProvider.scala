package antbutter

import slick.driver.MySQLDriver.simple._
import zotscrape.Schema
import Database.dynamicSession
import scala.concurrent.{BlockContext, Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait RepoProvider { this: ConfigProvider =>

  val repoService: Repo

  class Repo {
    lazy val db = Database.forURL(configService.jdbcUrl, configService.username, configService.password)

    def maxTimestamp = db withDynSession {
      Schema.history.map(_.timestamp).max.run
    }

    def timestamps = db withDynSession {
      Schema.history.sortBy(_.timestamp.desc).run
    }

    def terms = db withDynSession {
      Schema.terms.sortBy(_.yyyyst.desc).run
    }

    def schools = db withDynSession {
      Schema.schools.sortBy(_.name.asc).run
    }

    def departments = db withDynSession {
      Schema.departments.sortBy(_.name.asc).run
    }

    def courses = db withDynSession {
      Schema.courses.sortBy(_.title.desc).run
    }

    def meetingsBySectionId(id: Int) = db withDynSession {
      Schema.meetings.where(_.sectionId === id).run
    }

    def finalBySectionId(id: Int) = db withDynSession {
      Schema.finals.where(_.sectionId === id).firstOption
    }

    def restrictionsBySectionId(id: Int) = db withDynSession {
      val read = Schema.sectionRestrictions.where(_.sectionId === id) innerJoin
        Schema.restrictions on (_.restrictionCode === _.code)


      read.run map {
        case (s, r) => r
      }
    }

    def instructorsBySectionId(id: Int) = db withDynSession {
      Schema.instructors.where(_.sectionId === id).run
    }

    def enrollmentBySectionId(id: Int) = db withDynSession {
      Schema.enrollments.where(_.sectionId === id).firstOption
    }

    def sectionById(id: Int) = db withDynSession {
      Schema.sections.where(_.id === id).firstOption
    }

    def sectionByCcode(ccode: Int, termId: Int)(timestamp: java.sql.Timestamp) = db withDynSession {
      val read = Schema.sections where {
        s => s.ccode === ccode && s.termId === termId && s.timestamp === timestamp
      } innerJoin Schema.courses on {
        (s, c) => s.courseId === c.id
      } innerJoin Schema.departments on {
        case ((s, c), d) => c.departmentId === d.id
      } innerJoin Schema.schools on {
        case (((s, c), d), school) => d.schoolId === school.id
      }

      read.firstOption map {
        case (((section, course), department), school) => (section, course, department, school)
      } map {
        case (section, course, department, school) => {
          val sectionId = section._1
          val enrollment = Await.result(Future(enrollmentBySectionId(sectionId)), 3 seconds)
          val instructors = Await.result(Future(instructorsBySectionId(sectionId)), 3 seconds)
          val restrictions = Await.result(Future(restrictionsBySectionId(sectionId)), 3 seconds)
          val fina = Await.result(Future(finalBySectionId(sectionId)), 3 seconds)
          val meetings = Await.result(Future(meetingsBySectionId(sectionId)), 3 seconds)

          (section, course, department, school, meetings, fina, instructors, enrollment, restrictions)
        }
      }
    }
  }
}
