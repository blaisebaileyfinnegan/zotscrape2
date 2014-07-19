package zotscrape.writer

import web.Transport
import zotscrape.WebSoc

import scala.slick.driver.MySQLDriver
import scala.slick.driver.MySQLDriver.simple._

object Writer {
  class TermExistsException extends Exception

  def insertHistory(timestamp: java.sql.Timestamp)(implicit session: Session) = {
    Schema.history += timestamp
  }

  def insertIgnoreTerm(term: WebSoc.Term)(implicit session: Session) = {
    Schema.terms.filter(t => t.yyyyst === term.yyyyst).map(_.id).firstOption getOrElse {
      Schema.terms.map(t => (t.id, t.yyyyst, t.year, t.quarterName, t.termStatusMsg))
        .returning(Schema.terms.map(_.id))
        .insert((0, term.yyyyst.get, term.year.get, term.quarterName.get, term.termStatusMsg.get))
    }
  }

  def insertIgnoreSchool(school: WebSoc.School)(implicit session: Session) = {
    Schema.schools
      .filter(s => s.code === school.code && s.name === school.name)
      .map(_.id).firstOption getOrElse {
      Schema.schools.map(s => (s.id, s.code, s.name))
        .returning(Schema.schools.map(_.id))
        .insert((0, school.code.get, school.name.get))
    }
  }

  def insertIgnoreDepartment(department: WebSoc.Department)(schoolId: Int)(implicit session: Session) = {
    Schema.departments
      .filter(d => d.schoolId === schoolId && d.code === department.code && d.name === department.name)
      .map(_.id).firstOption getOrElse {
      Schema.departments.map(d => (d.id, d.schoolId, d.code, d.name))
        .returning(Schema.departments.map(_.id))
        .insert((0, schoolId, department.code.get, department.name.get))
    }
  }

  def insertIgnoreCourse(course: WebSoc.Course)(departmentId: Int)(implicit session: Session) = {
    Schema.courses
      .filter(c => c.departmentId === departmentId && c.number === course.number && c.title === course.title)
      .map(_.id).firstOption getOrElse {
      Schema.courses.map(c => (c.id, c.departmentId, c.number, c.title, c.prereqLink))
        .returning(Schema.courses.map(_.id))
        .insert((0, departmentId, course.number.get, course.title.get, course.prereqLinks.headOption.getOrElse("")))
    }
  }

  def insertSection(section: WebSoc.Section)(courseId: Int, termId: Int, timestamp: java.sql.Timestamp)(implicit session: Session) = {
    Schema.sections.map(s => (s.id, s.courseId, s.termId, s.timestamp, s.ccode, s.typ, s.num, s.units, s.booksLink, s.graded, s.status))
      .returning(Schema.sections.map(_.id))
      .insert((0, courseId, termId, timestamp, section.ccode.get, section.typ, section.num, section.units, section.booksLink, section.graded, section.status))
  }

  def insertIgnoreRestriction(restriction: WebSoc.Restriction)(implicit session: Session) = {
    try {
      Schema.restrictions += Transport.Restriction(restriction.code.get, restriction.definition)
    } catch {
      case _: Exception => ()
    }
  }

  def insertSectionRestriction(code: String)(sectionId: Int)(implicit session: Session) = {
    try {
      Schema.sectionRestrictions += Transport.SectionRestriction(sectionId, code)
    } catch {
      case _: Exception => ()
    }
  }

  def insertMeeting(meeting: WebSoc.Meeting)(sectionId: Int)(implicit session: Session) = {
    val (b, e) = meeting.time.map {
      case (begin, end) => (Some(begin), Some(end))
    } getOrElse ((None, None))

    val days = meeting.days.getOrElse(
      WebSoc.Days(sunday = false, monday = false, tuesday = false, wednesday = false, thursday = false, friday = false, saturday = false))

    (Schema.meetings returning Schema.meetings.map(_.id)) +=
      Transport.Meeting(0, sectionId, b, e, meeting.building, meeting.room, meeting.roomLink, days)
  }

  def insertFinal(sectionFinal: WebSoc.Final)(sectionId: Int)(implicit session: Session) = {
    val (b, e) = sectionFinal.time.map {
      case (begin, end) => (Some(begin), Some(end))
    } getOrElse((None, None))

    (Schema.finals returning Schema.finals.map(_.id)) +=
      Transport.Final(0, sectionId, sectionFinal.date, sectionFinal.day, b, e)
  }

  def insertInstructor(instructor: String)(sectionId: Int)(implicit session: Session) = {
    (Schema.instructors returning Schema.instructors.map(_.id)) +=
      Transport.Instructor(0, sectionId, instructor)
  }

  def insertEnrollment(enrollment: WebSoc.Enrollment)(sectionId: Int)(implicit session: Session) = {
    (Schema.enrollments returning Schema.enrollments.map(_.id)) +=
      Transport.Enrollment(0, sectionId, enrollment)
  }
}
