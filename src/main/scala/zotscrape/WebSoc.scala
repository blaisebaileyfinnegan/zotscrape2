package zotscrape

import zotscrape.WebSoc._

object WebSoc {
  type Interval = (Int, Int)

  case class Metadata(generated: Option[String],
                      author: Option[String])

  case class Term(yyyyst: Option[String],
                  year: Option[Int],
                  quarterName: Option[String],
                  termStatusMsg: Option[String])
  case class Restriction(code: Option[String],
                         definition: Option[String])
  case class CourseList(schools: Seq[School])


  case class School(code: Option[String],
                    name: Option[String],
                    departments: Seq[Department])
  case class Department(code: Option[String],
                        name: Option[String],
                        courses: Seq[Course])
  case class Course(number: Option[String],
                    title: Option[String],
                    prereqLinks: Seq[String],
                    sections: Seq[Section])
  case class Section(ccode: Option[Int],
                     typ: Option[String],
                     num: Option[String],
                     units: Option[String],
                     instructors: Seq[String],
                     meetings: Seq[Meeting],
                     secFinal: Option[Final],
                     enrollment: Option[Enrollment],
                     restrictions: Option[Set[String]],
                     booksLink: Option[String],
                     graded: Option[Boolean],
                     status: Option[String])

  case class Final(date: Option[String],
                   day: Option[String],
                   time: Option[Interval])

  case class Enrollment(max: Option[Int],
                        enrolled: Option[Int],
                        req: Option[Int],
                        newOnly: Option[Int],
                        waitList: Option[Int],
                        waitCap: Option[Int],
                        xlist: Option[Int])

  case class Meeting(days: Option[Days],
                     time: Option[Interval],
                     building: Option[String],
                     room: Option[String],
                     roomLink: Option[String])

  case class Days(sunday: Boolean,
                  monday: Boolean,
                  tuesday: Boolean,
                  wednesday: Boolean,
                  thursday: Boolean,
                  friday: Boolean,
                  saturday: Boolean)
}

case class WebSoc(metadata: Metadata,
                  term: Option[Term],
                  courseList: Option[CourseList],
                  codes: Option[Seq[Restriction]])
