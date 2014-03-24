package antbutter

import slick.driver.MySQLDriver.simple._
import Database.dynamicSession
import scala.concurrent.{BlockContext, Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import zotscrape.Writer.Schema

trait RepoProvider { this: ConfigProvider =>

  val repoService: Repo

  class Repo {
    lazy val db = Database.forURL(configService.Jdbc.url, configService.Jdbc.username, configService.Jdbc.password)

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

    def sectionsByCourse(id: Int) = db withDynSession {
      Schema.sections.where(_.courseId === id).run
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
      Schema.sections.where { s =>
        s.ccode === ccode && s.termId === termId && s.timestamp === timestamp
      }.firstOption
    }

    def termByYyyyst(yyyyst: String) = db withDynSession {
      Schema.terms.where(_.yyyyst === yyyyst).firstOption
    }

    def schoolByCode(code: String) = db withDynSession {
      Schema.schools.where(_.code === code).firstOption
    }

    def termById(id: Int) = db withDynSession {
      Schema.terms.where(_.id === id).firstOption
    }

    def schoolById(id: Int) = db withDynSession {
      Schema.schools.where(_.id === id).firstOption
    }

    def departmentById(id: Int) = db withDynSession {
      Schema.departments.where(_.id === id).firstOption
    }

    def departmentsBySchoolId(id: Int) = db withDynSession {
      Schema.departments.where(_.schoolId === id).sortBy(_.name.asc).run
    }

    def coursesByDepartmentId(id: Int) = db withDynSession {
      Schema.courses.where(_.departmentId === id).sortBy(_.number.asc).run
    }
  }
}
