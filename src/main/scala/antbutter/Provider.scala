package antbutter

import slick.driver.MySQLDriver.simple._
import zotscrape.Schema
import Database.dynamicSession

object Provider {
  lazy val db = Database.forURL(Config.jdbcUrl, Config.username, Config.password)

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
    }
  }
}
