import scala.concurrent.Future
import scala.slick.backend._
import scala.slick.driver.MySQLDriver.simple._
import scala.concurrent.ExecutionContext.Implicits.global

object Writer {
  class TermExistsException extends Exception

  def insertHistory(timestamp: java.sql.Timestamp)(implicit session: Session) = Future {
    Schema.history += timestamp
  }

  def insertIgnoreTerm(term: WebSoc.Term)(implicit session: Session) = Future {
    val result = Schema.terms.where(_.yyyyst === term.yyyyst.get).run.headOption map {
      case (id, _, _, _, _) => id
    }

    result.getOrElse((Schema.terms returning Schema.terms.map(_.id)) +=
      (0, term.yyyyst.get, term.year.get, term.quarterName.get, term.termStatusMsg.get))
  }

  def insertIgnoreSchool(school: WebSoc.School)(implicit session: Session) = Future {
    val result = Schema.schools.where {
      q => q.code === school.code.get && q.name === school.name.get
    }.run.headOption map {
      case (id, _, _) => id
    }

    result.getOrElse((Schema.schools returning Schema.schools.map(_.id)) +=
      (0, school.code.get, school.name.get))
  }

  def insertIgnoreDepartment(department: WebSoc.Department)(schoolId: Int)(implicit session: Session) = Future {
    val result = Schema.departments.where {
      q => q.schoolId === schoolId && q.code === department.code.get && q.name === department.name.get
    }.run.headOption map {
      case (id, _, _, _) => id
    }

    result.getOrElse((Schema.departments returning Schema.departments.map(_.id)) +=
      (0, schoolId, department.code.get, department.name.get))
  }

  def insertIgnoreCourse(course: WebSoc.Course)(departmentId: Int)(implicit session: Session) = Future {
    val result = Schema.courses.where {
      q => q.departmentId === departmentId && q.number === course.number.get && q.title === course.title.get
    }.run.headOption map {
      case (id, _, _, _, _) => id
    }

    result.getOrElse((Schema.courses returning Schema.courses.map(_.id)) +=
      (0, departmentId, course.number.get, course.title.get, course.prereqLinks.headOption.getOrElse("")))
  }

  def insertSection(section: WebSoc.Section)(courseId: Int, termId: Int, timestamp: java.sql.Timestamp)(implicit session: Session) = Future {
    (Schema.sections returning Schema.sections.map(_.id)) +=
      (0, courseId, termId, timestamp, section.ccode.get, section.typ, section.num, section.units, section.booksLink, section.graded, section.status)
  }

  def insertIgnoreRestriction(restriction: WebSoc.Restriction)(implicit session: Session) = Future {
    val result = Schema.restrictions.where {
      q => q.code === restriction.code
    }.exists.run

    if (!result)
      Schema.restrictions += (restriction.code.get, restriction.definition)
  }

  def insertSectionRestriction(sectionId: Int, timestamp: java.sql.Timestamp)(code: String)(implicit session: Session) = Future {
    Schema.sectionRestrictions += (sectionId, timestamp, code)
  }

  def insertMeeting(meeting: WebSoc.Meeting)(sectionId: Int, timestamp: java.sql.Timestamp)(implicit session: Session) = Future {
    val (b, e) = meeting.time.map {
      case (begin, end) => (Some(begin), Some(end))
    } getOrElse ((None, None))

    val days = meeting.days.getOrElse(
      WebSoc.Days(sunday = false, monday = false, tuesday = false, wednesday = false, thursday = false, friday = false, saturday = false))

    (Schema.meetings returning Schema.meetings.map(_.id)) +=
      (0, sectionId, timestamp, b, e, meeting.building, meeting.room, meeting.roomLink, days)
  }

  def insertFinal(sectionFinal: WebSoc.Final)(sectionId: Int, timestamp: java.sql.Timestamp)(implicit session: Session) = Future {
    val (b, e) = sectionFinal.time.map {
      case (begin, end) => (Some(begin), Some(end))
    } getOrElse((None, None))

    (Schema.finals returning Schema.finals.map(_.id)) +=
      (0, sectionId, timestamp, sectionFinal.date, sectionFinal.day, b, e)
  }

  def insertInstructor(instructor: String)(sectionId: Int, timestamp: java.sql.Timestamp)(implicit session: Session) = Future {
    (Schema.instructors returning Schema.instructors.map(_.id)) +=
      (0, sectionId, timestamp, instructor)
  }

  def insertEnrollment(enrollment: WebSoc.Enrollment)(sectionId: Int, timestamp: java.sql.Timestamp)(implicit session: Session) = Future {
    (Schema.enrollments returning Schema.enrollments.map(_.id)) +=
      (0, sectionId, timestamp, enrollment)
  }
}
