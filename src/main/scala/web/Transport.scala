package web

import zotscrape.WebSoc

object Transport {
  case class Term(id: Int, yyyyst: String, year: Int, quarterName: String, termStatusMsg: String)
  case class School(id: Int, code: String, name: String)
  case class Department(id: Int, schoolId: Int, code: String, name: String)
  case class Course(id: Int, departmentId: Int, number: String, title: String, prereqLink: String)
  case class Section(id: Int,
                     courseId: Int,
                     termId: Int,
                     timestamp: java.sql.Timestamp,
                     ccode: Int,
                     typ: Option[String],
                     num: Option[String],
                     units: Option[String],
                     booksLink: Option[String],
                     graded: Option[Boolean],
                     status: Option[String])
  case class Restriction(code: String, definition: Option[String])
  case class SectionRestriction(sectionId: Int, restrictionCode: String)
  case class Meeting(id: Int,
                     sectionId: Int,
                     begin: Option[Int],
                     end: Option[Int],
                     building: Option[String],
                     room: Option[String],
                     roomLink: Option[String],
                     days: WebSoc.Days)
  case class Final(id: Int, sectionId: Int, date: Option[String], day: Option[String], begin: Option[Int], end: Option[Int])
  case class Instructor(id: Int, sectionId: Int, name: String)
  case class Enrollment(id: Int, sectionId: Int, enrollment: WebSoc.Enrollment)


}
