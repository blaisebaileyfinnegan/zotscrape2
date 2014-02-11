import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.{ExecutionContext, Future}
import scala.xml._
import WebSoc._
import scala.language.implicitConversions
import ExecutionContext.Implicits.global

object DocumentParser extends Logging {

  implicit def convertOptionStringToInt(s: Option[String]): Option[Int] = s.map(Integer.valueOf(_))

  implicit def convertNodeSeqToString(s: Seq[Node]): Option[String] = {
    if (s.isEmpty) None
    else if (s.size > 1) throw new Error("Too many nodes found.")
    else if (s.head.text == "TBA") None
    else Some(s.head.text)
  }

  implicit def convertNodeSeqToInt(s: Seq[Node]): Option[Int] = {
    if (s.isEmpty) None
    else if (s.size > 1) throw new Error("Too many nodes found.")
    else if (s.head.text.toLowerCase == "n/a") None
    else Some(Integer.valueOf(s.head.text))
  }

  def getMetadata(document: Elem): Metadata = {
    val attributes = document.attributes.asAttrMap
    val generated = attributes.get("generated")
    val author = attributes.get("author")

    Metadata(generated, author)
  }

  def parseEnrollment(n: Node): Enrollment = {
    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)
    val maxEnrolled = groups("sec_max_enroll")
    val enrolled = groups("sec_enrolled")
    val req = groups("sec_enroll_requests")
    val newOnly = groups("sec_new_only_reserved")
    val waitList = groups("sec_waitlist")
    val waitCap = groups("sec_wait_cap")
    val xlist = groups("sec_xlist_subenrolled")

    Enrollment(maxEnrolled, enrolled, req, newOnly, waitList, waitCap, xlist)
  }

  def parseGraded(n: Node): Option[Boolean] = n.text match {
    case "1" => Some(true)
    case "0" => Some(false)
    case _ => None
  }

  def parseDays(n: Node): Option[Days] = {
    if (n.text == "TBA") None
    else {
      val regex = """[A-Z][a-z]?""".r
      val days = regex.findAllIn(n.text).toSet

      Some(Days(days("Su"), days("M"), days("Tu"), days("W"), days("Th"), days("F"), days("Sa")))
    }
  }

  def parseTimeInstants(begin: String, end: String): Interval = {
    val index = end.indexOf("p")
    val endsInPm = index > -1

    val beginRaw = begin
    val endRaw =
      if (endsInPm) end.substring(0, index)
      else if (end.endsWith("am")) end.substring(0, end.size - 2)
      else end

    val beginSplits = beginRaw.split(":")
    val endSplits = endRaw.split(":")

    val beginHourRaw = beginSplits(0)
    val beginMinuteRaw = beginSplits(1)
    val endHourRaw = endSplits(0)
    val endMinuteRaw = endSplits(1)

    val beginMilitaryRaw = Integer.valueOf(beginHourRaw + beginMinuteRaw)
    val endMilitaryRaw = Integer.valueOf(endHourRaw + endMinuteRaw)

    val beginMilitaryAdjustedForPm: Int =
      if (endsInPm && (beginMilitaryRaw < endMilitaryRaw) && (endMilitaryRaw < 1200)) beginMilitaryRaw + 1200
      else beginMilitaryRaw
    val endMilitaryAdjustedForPm: Int =
      if (endsInPm && (endMilitaryRaw < 1200)) endMilitaryRaw + 1200
      else endMilitaryRaw

    (beginMilitaryAdjustedForPm, endMilitaryAdjustedForPm)
  }

  def parseTime(n: Node): Option[Interval] =
    if (n.text == "TBA") None
    else {
      val instants = n.text.split("-").map(_.trim)
      if (instants.size != 2) None
      else Some(parseTimeInstants(instants(0), instants(1)))
    }

  def parseMeeting(n: Node): Meeting = {
    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)

    val days = groups("sec_days").headOption.flatMap(parseDays)
    val time = groups("sec_time").headOption.flatMap(parseTime)
    val building = groups("sec_bldg")
    val room = groups("sec_room")
    val roomLink = groups("sec_room_link")

    Meeting(days, time, building, room, roomLink)
  }

  def parseFinal(n: Node): Final = {
    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)

    val date = groups("sec_final_date").headOption.map(_.text.trim)
    val day = groups("sec_final_day").headOption.map(_.text.trim)
    val time = groups("sec_final_time").headOption.flatMap(parseTime)

    Final(date, day, time)
  }

  def parseSection(n: Node): Section = {
    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)
    val code = groups("course_code")
    val sectionType = groups("sec_type")
    val num = groups("sec_num")
    val units = groups("sec_units")
    val instructors = groups("sec_instructors").headOption.map(_.child.map(_.text.trim).filter(_.size > 0)).getOrElse(Seq.empty)
    val meetings = groups("sec_meetings").flatMap(_.child.filter(_.text.trim.size > 0).map(parseMeeting))
    println(groups("sec_final"))
    val sectionFinal = groups("sec_final").headOption.map(parseFinal)
    val enrollment = groups("sec_enrollment").headOption.map(parseEnrollment)
    val restrictions = groups("sec_restrictions")
      .headOption
      .map(_.text.split("and").map(_.trim).toSet)
    val booksLink = groups("sec_books").headOption.map(_.text.trim)
    val graded = groups("sec_graded").headOption.flatMap(parseGraded)
    val status = groups("sec_status")

    Section(code,
      sectionType,
      num,
      units,
      instructors,
      meetings,
      sectionFinal,
      enrollment,
      restrictions,
      booksLink,
      graded,
      status)
  }

  def parseCourse(n: Node): Course = {
    val attributes = n.attributes.asAttrMap
    val number = attributes.get("course_number")
    val title = attributes.get("course_title")

    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)
    val prereqLinks = groups("course_prereq_link") map (_.text.trim)
    val sections = groups("section") map parseSection

    if (groups.size > 2) logger.warn("Course " + n + " has unrecognized children.")

    val course = Course(number, title, prereqLinks, sections)

    course
  }

  def parseDepartment(n: Node): Department = {
    val attributes = n.attributes.asAttrMap
    val code = attributes.get("dept_code")
    val name = attributes.get("dept_name")

    val courses = n.child.filter(_.label == "course").map(parseCourse)

    if (courses.size != n.child.size) logger.warn("Department " + n + " has unrecognized children.")

    Department(code, name, courses)
  }

  def parseSchool(n: Node): School = {
    val attributes = n.attributes.asAttrMap
    val code = attributes.get("school_code")
    val name = attributes.get("school_name")

    val departments = n.child.filter(_.label == "department").map(parseDepartment)
    if (departments.size != n.child.size) logger.warn("School " + n + " has unrecognized children.")

    School(code, name, departments)
  }

  def parseTerm(n: Node): Term = {
    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)

    if (groups.size > 4) logger.warn("Term " + n + " has more children than expected.")

    Term(groups("term_yyyyst"), groups("term_year"), groups("term_name"), groups("term_status_msg"))
  }

  def parseCourseList(n: Node): CourseList = {
    CourseList(n.child.filter(_.label == "school").map(parseSchool))
  }

  def parseRestrictionCodes(n: Node): Seq[Restriction] = {
    val (restrictions, other) = n.child.partition(_.label == "restriction")

    if (!other.isEmpty) logger.warn("Non restriction nodes found in restriction codes.")

    restrictions.map(parseRestriction)
  }

  def parseRestriction(n: Node): Restriction = {
    val groups = n.child.groupBy(_.label).withDefaultValue(Seq.empty)

    if (groups.size > 2) logger.warn("Restriction " + n + " has more children then expected.")

    val code = groups("restriction_code").headOption.map(_.text.trim)
    val restrictionDef = groups("restriction_def").headOption.map(_.text.trim)

    Restriction(code, restrictionDef)
  }

  def apply(e: Elem): Future[WebSoc] = Future {
    val groups = e.child.groupBy(_.label).withDefaultValue(Seq.empty)

    if (groups.size > 4) logger.warn("Document " + e + " has more children then expected.")

    val metadata = getMetadata(e)
    val term = groups("term").headOption.map(parseTerm)
    val courseList = groups("course_list").headOption.map(parseCourseList)
    val restrictionCodes = groups("restriction_codes").headOption.map(parseRestrictionCodes)

    WebSoc(metadata, term, courseList, restrictionCodes)
  }
}
