package zotscrape.writer

import zotscrape.WebSoc

import scala.slick.driver.MySQLDriver.simple._

object Schema {
  import web.Transport

  class History(tag: Tag) extends Table[(java.sql.Timestamp)](tag, "history") {
    def timestamp = column[java.sql.Timestamp]("timestamp", O.PrimaryKey)

    def * = timestamp
  }

  val history = TableQuery[History]

  class Term(tag: Tag) extends Table[Transport.Term](tag, "terms") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def yyyyst = column[String]("yyyyst")
    def year = column[Int]("year")
    def quarterName = column[String]("quarter_name")
    def termStatusMsg = column[String]("term_status_message")

    def uniq = index("yyyyst_unique", yyyyst, unique = true)

    def * = (id, yyyyst, year, quarterName, termStatusMsg) <> (Transport.Term.tupled, Transport.Term.unapply)
  }

  val terms = TableQuery[Term]

  class School(tag: Tag) extends Table[Transport.School](tag, "schools") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def code = column[String]("code")
    def name = column[String]("name")

    def uniq = index("school_unique", (code, name), unique = true)

    def * = (id, code, name) <> (Transport.School.tupled, Transport.School.unapply)
  }

  val schools = TableQuery[School]

  class Department(tag: Tag) extends Table[Transport.Department](tag, "departments") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def schoolId = column[Int]("school_id")
    def code = column[String]("code")
    def name = column[String]("name")

    def school = foreignKey("school_fk", schoolId, schools)(_.id)
    def uniq = index("department_unique", (schoolId, code, name), unique = true)

    def * = (id, schoolId, code, name) <> (Transport.Department.tupled, Transport.Department.unapply)
  }

  val departments = TableQuery[Department]

  class Course(tag: Tag) extends Table[Transport.Course](tag, "courses") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def departmentId = column[Int]("dept_id")
    def number = column[String]("number")
    def title = column[String]("title")
    def prereqLink = column[String]("prereq_link")

    def department = foreignKey("department_fk", departmentId, departments)(_.id)
    def uniq = index("course_unique", (departmentId, number, title), unique = true)

    def * = (id, departmentId, number, title, prereqLink) <>
      (Transport.Course.tupled, Transport.Course.unapply)
  }

  val courses = TableQuery[Course]


  class Section(tag: Tag) extends Table[Transport.Section](tag, "sections") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def courseId = column[Int]("course_id")
    def termId = column[Int]("term_id")
    def timestamp = column[java.sql.Timestamp]("timestamp")
    def ccode = column[Int]("ccode")
    def typ = column[Option[String]]("type")
    def num = column[Option[String]]("num")
    def units = column[Option[String]]("units")
    def booksLink = column[Option[String]]("books_link")
    def graded = column[Option[Boolean]]("graded")
    def status = column[Option[String]]("status")

    def course = foreignKey("course_fk", courseId, courses)(_.id)
    def term = foreignKey("term_fk", termId, terms)(_.id)
    def timestamps = foreignKey("section_history_fk", timestamp, history)(_.timestamp)
    def uniq = index("section_unique", (timestamp, termId, ccode), unique = true)

    def * = (id, courseId, termId, timestamp, ccode, typ, num, units, booksLink, graded, status) <>
      (Transport.Section.tupled, Transport.Section.unapply)
  }

  val sections = TableQuery[Section]

  class Restriction(tag: Tag) extends Table[Transport.Restriction](tag, "restrictions") {
    def code = column[String]("code", O.PrimaryKey)
    def definition = column[Option[String]]("definition")

    def * = (code, definition) <> (Transport.Restriction.tupled, Transport.Restriction.unapply)
  }

  val restrictions = TableQuery[Restriction]

  class SectionRestriction(tag: Tag) extends Table[Transport.SectionRestriction](tag, "section2restrictions") {
    def sectionId = column[Int]("section_id")
    def restrictionCode = column[String]("restriction_code")

    def pk = primaryKey("section2restrictions_pk", (sectionId, restrictionCode))
    def section = foreignKey("section2restrictions_section_fk", sectionId, sections)(_.id)
    def restriction = foreignKey("section2restrictions_restriction_fk", restrictionCode, restrictions)(_.code)

    def * = (sectionId, restrictionCode) <>
      (Transport.SectionRestriction.tupled, Transport.SectionRestriction.unapply)
  }

  val sectionRestrictions = TableQuery[SectionRestriction]

  class Meeting(tag: Tag) extends Table[Transport.Meeting](tag, "meetings") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def sectionId = column[Int]("section_id")
    def begin = column[Option[Int]]("begin")
    def end = column[Option[Int]]("end")
    def building = column[Option[String]]("building")
    def room = column[Option[String]]("room")
    def roomLink = column[Option[String]]("room_link")
    def sunday = column[Boolean]("sunday")
    def monday = column[Boolean]("monday")
    def tuesday = column[Boolean]("tuesday")
    def wednesday = column[Boolean]("wednesday")
    def thursday = column[Boolean]("thursday")
    def friday = column[Boolean]("friday")
    def saturday = column[Boolean]("saturday")

    def section = foreignKey("section_meeting_fk", sectionId, sections)(_.id)

    def * = (id, sectionId, begin, end, building, room, roomLink,
      (sunday, monday, tuesday, wednesday, thursday, friday, saturday) <>
        (WebSoc.Days.tupled, WebSoc.Days.unapply)) <>
      (Transport.Meeting.tupled, Transport.Meeting.unapply)

  }

  val meetings = TableQuery[Meeting]

  class Final(tag: Tag) extends Table[Transport.Final](tag, "finals") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def sectionId = column[Int]("section_id")
    def date = column[Option[String]]("date")
    def day = column[Option[String]]("day")
    def begin = column[Option[Int]]("begin")
    def end = column[Option[Int]]("end")

    def section = foreignKey("section_final_fk", sectionId, sections)(_.id)

    def * = (id, sectionId, date, day, begin, end) <> (Transport.Final.tupled, Transport.Final.unapply)
  }

  val finals = TableQuery[Final]

  class Instructor(tag: Tag) extends Table[Transport.Instructor](tag, "instructors") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def sectionId = column[Int]("section_id")
    def name = column[String]("name")

    def section = foreignKey("section_instructor_fk", sectionId, sections)(_.id)

    def * = (id, sectionId, name) <> (Transport.Instructor.tupled, Transport.Instructor.unapply)
  }

  val instructors = TableQuery[Instructor]

  class Enrollment(tag: Tag) extends Table[Transport.Enrollment](tag, "enrollments") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def sectionId = column[Int]("section_id")
    def max = column[Option[Int]]("max")
    def enrolled = column[Option[Int]]("enrolled")
    def req = column[Option[Int]]("req")
    def newOnly = column[Option[Int]]("new")
    def waitList = column[Option[Int]]("waitlist")
    def waitCap = column[Option[Int]]("waitcap")
    def xlist = column[Option[Int]]("xlist")

    def section = foreignKey("section_enrollment_fk", sectionId, sections)(_.id)

    def * = (id, sectionId,
      (max, enrolled, req, newOnly, waitList, waitCap, xlist) <>
        (WebSoc.Enrollment.tupled, WebSoc.Enrollment.unapply)) <>
      (Transport.Enrollment.tupled, Transport.Enrollment.unapply)
  }

  val enrollments = TableQuery[Enrollment]

}
