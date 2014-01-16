import org.joda.time.Interval
import WebSoc._

case class WebSoc(metadata: Metadata, term: Term, courseList: CourseList, codes: List[Restriction])

object WebSoc {
  case class Metadata(generated: String, author: String)

  case class Term(yyyyst: String, year: Int, quarterName: String, termStatusMsg: String)
  case class Restriction(code: String, definition: String)
  case class CourseList(schools: List[School])


  case class School(code: String, name: String, departments: List[Department])
  case class Department(code: String, name: String, courses: List[Course])
  case class Course(number: String, title: String, prereqLink: String, sections: List[Section])
  case class Section(ccode: Int,
                     typ: String,
                     num: String,
                     units: String,
                     instructors: List[String],
                     meetings: List[Meeting],
                     enrollment: Enrollment,
                     restrictions: Set[String],
                     booksLink: String,
                     graded: Boolean,
                     status: String)

  case class Final(time: Interval)

  case class Enrollment(max: Int,
                        enrolled: Int,
                        req: Int,
                        newOnly: Int,
                        waitList: Int,
                        waitCap: Int,
                        xlist: Int)

  case class Meeting(days: Days, time: Interval, building: String, room: String, roomLink: String)

  case class Days(sunday: Boolean,
                  monday: Boolean,
                  tuesday: Boolean,
                  wednesday: Boolean,
                  thursday: Boolean,
                  friday: Boolean,
                  saturday: Boolean)
}
