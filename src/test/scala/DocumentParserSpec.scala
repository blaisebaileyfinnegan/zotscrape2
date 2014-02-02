import org.scalatest._
import scala.concurrent.ExecutionContext.Implicits.global

class DocumentParserSpec extends FlatSpec with Matchers {
  import DocumentParser._
  import WebSoc._

  "Parsing a school tag with no departments" should "return a school" in {
    val node = <school school_code="60" school_name="School of Humanities"></school>

    parseSchool(node) should be (School(Some("60"), Some("School of Humanities"), Seq.empty))
  }

  "Parsing a school tag with some departments" should "return a school" in {
    val node =
      <school school_code="AA" school_name="Goodbye!">
        <department dept_code="HOW" dept_name="DO YOU DO"></department>
        <department dept_code="wow" dept_name="such dept"></department>
      </school>

    parseSchool(node) match {
      case School(code, name, depts) => {
        code should be (Some("AA"))
        name should be (Some("Goodbye!"))
        depts should have size 2
      }
      case _ => fail()
    }
  }

  "Parsing a school tag with a missing attribute" should "return none" in {
    val node =
      <school school_code="B"></school>

    parseSchool(node) should be (School(Some("B"), None, Seq.empty))
  }

  "Parsing a department with no courses" should "return a department" in {
    val node =
      <department dept_code="a" dept_name="b"></department>

    parseDepartment(node) should be (
      Department(
        Some("a"),
        Some("b"),
        Seq.empty
      )
    )
  }

  "Parsing a department with some courses" should "return a department" in {
    val node =
      <department dept_code="c" dept_name="fake">
        <course course_number="who" course_title="knows"></course>
        <course course_number="who" course_title="knows"></course>
        <course course_number="who" course_title="knows"></course>
        <course course_number="who" course_title="knows"></course>
        <course course_number="who" course_title="knows"></course>
      </department>

    val result = parseDepartment(node)
    result.code should be (Some("c"))
    result.name should be (Some("fake"))
    result.courses should have size 5
  }

  "Parsing a course with sections" should "return a course" in {
    val node =
      <course course_number="20B" invalid_attribute="WOOO!!!">
        <course_prereq_link>abcd</course_prereq_link>
        <course_prereq_link>dcba</course_prereq_link>
        <section>
        </section>
      </course>

    val course = parseCourse(node)
    course.number should be (Some("20B"))
    course.prereqLinks should contain only ("abcd", "dcba")
    course.sections should have size 1
    course.title should be (None)
  }

  "DocumentParser apply" should "parse a document" in {
    val document =
      <websoc_result>
        <search_criteria>
        </search_criteria>
        <term>
        </term>
        <course_list>
          <school></school>
        </course_list>
        <restriction_codes>
          <restriction>
          </restriction>
        </restriction_codes>
      </websoc_result>

    val future = DocumentParser(document)

    future onSuccess {
      case result =>
        result.metadata should be (Metadata(None, None))
        result.term should be (Some(Term(None, None, None, None)))
        result.courseList shouldBe a [Some[CourseList]]
        result.courseList.get.schools should contain only School(None, None, Seq.empty)
        result.codes.get should contain only Restriction(None, None)
    }
  }

  "parseRestriction" should "parse restrictions" in {
    val xml =
      <restriction>
        <restriction_code>B</restriction_code>
        <restriction_def>Authorization required</restriction_def>
      </restriction>

    val result = parseRestriction(xml)

    result.code should be (Some("B"))
    result.definition should be (Some("Authorization required"))
  }

  "parseEnrollment" should "parse enrollment" in {
    val xml =
      <sec_enrollment>
        <sec_max_enroll>18</sec_max_enroll>
        <sec_enrolled>18</sec_enrolled>
        <sec_enroll_requests>19</sec_enroll_requests>
        <sec_new_only_reserved>20024</sec_new_only_reserved>
        <sec_waitlist>n/a</sec_waitlist>
        <sec_wait_cap>0</sec_wait_cap>
      </sec_enrollment>

    val result = parseEnrollment(xml)

    result.max should be (Some(18))
    result.enrolled should be (Some(18))
    result.req should be (Some(19))
    result.newOnly should be (Some(20024))
    result.waitList should be (None)
    result.waitCap should be (Some(0))
  }

  "parseDays" should "parse some days" in {
    val xml = <sec_days>MWF</sec_days>

    val result = parseDays(xml)

    result should be (Some(Days(false, true, false, true, false, true, false)))
  }

  "parseDays" should "parse all days" in {
    val xml = <sec_days>SuMTuWThFSa</sec_days>

    val result = parseDays(xml)

    result should be (Some(Days(true, true, true, true, true, true, true)))
  }

  "parseTimeInstants" should "work" in {
    parseTimeInstants("3:00", "3:01p") should be ((1500, 1501))
    parseTimeInstants("11:50", "12:30p") should be ((1150, 1230))
    parseTimeInstants("12:30", "1:50p") should be ((1230, 1350))
    parseTimeInstants("5:00", "7:50") should be ((500, 750))
  }

  "parseTime" should "parse time nodes" in {
    val xml = <sec_time>5:00- 6:20p</sec_time>

    val result = parseTime(xml)
    result should be (Some((1700, 1820)))
  }

  "parseMeeting" should "parse meeting" in {
    val xml =
      <sec_meet>
        <sec_days>TBA</sec_days>
        <sec_time>5:00- 6:20p</sec_time>
        <sec_bldg>HH</sec_bldg>
        <sec_room>220</sec_room>
        <sec_room_link>TBA</sec_room_link>
      </sec_meet>

    val result = parseMeeting(xml)

    result.days should be (None)
    result.time should be (Some((1700, 1820)))
    result.building should be (Some("HH"))
    result.room should be (Some("220"))
    result.roomLink should be (None)
  }

  "parseSection" should "parse section" in {
    val xml =
      <section>
        <course_code>20075</course_code>
        <sec_type>Lec</sec_type>
        <sec_num>P</sec_num>
        <sec_units>5</sec_units>
        <sec_instructors>
          <instructor>KAUFEL, F.</instructor>
        </sec_instructors>
        <sec_meetings>
          <sec_meet>
            <sec_days>TuTh</sec_days>
            <sec_time>3:30- 4:50p</sec_time>
            <sec_bldg>HH</sec_bldg>
            <sec_room>210</sec_room>
            <sec_room_link>http://www.classrooms.uci.edu/GAC/HH210.html</sec_room_link>
          </sec_meet>
        </sec_meetings>
        <sec_final>
          <sec_final_date>Mar 15</sec_final_date>
          <sec_final_day>Sat</sec_final_day>
          <sec_final_time>10:30-12:30pm</sec_final_time>
        </sec_final>
        <sec_enrollment>
          <sec_max_enroll>18</sec_max_enroll>
          <sec_enrolled>17</sec_enrolled>
          <sec_enroll_requests>20</sec_enroll_requests>
          <sec_new_only_reserved>20075</sec_new_only_reserved>
          <sec_waitlist>n/a</sec_waitlist>
          <sec_wait_cap>0</sec_wait_cap>
        </sec_enrollment>
        <sec_restrictions>B and D</sec_restrictions>
        <sec_books>
          http://book.uci.edu/ePOS?this_category=76&nbsp;store=446&nbsp;form=shared3/gm/main.html&nbsp;design=446
        </sec_books>
        <sec_graded>1</sec_graded>
        <sec_linkage>
          <sec_backward_ptr>20060</sec_backward_ptr>
          <sec_group_backward_ptr>00000</sec_group_backward_ptr>
          <sec_forward_ptr>20076</sec_forward_ptr>
          <sec_group_forward_ptr>20092</sec_group_forward_ptr>
        </sec_linkage>
        <sec_status>OPEN</sec_status>
        <sec_active>1</sec_active>
      </section>

    val result = parseSection(xml)

    result.ccode should be (Some(20075))
    result.typ should be (Some("Lec"))
    result.num should be (Some("P"))
    result.units should be (Some("5"))
    result.instructors should contain only "KAUFEL, F."
    result.meetings should contain only Meeting(
      Some(Days(false, false, true, false, true, false, false)),
      Some((1530, 1650)),
      Some("HH"),
      Some("210"),
      Some("http://www.classrooms.uci.edu/GAC/HH210.html")
    )
    result.secFinal should be (Some(Final(Some("Mar 15"), Some("Sat"), Some((1030, 1230)))))
    result.enrollment should be (Some(Enrollment(Some(18), Some(17), Some(20), Some(20075), None, Some(0), None)))
    result.restrictions.get should contain only ("B", "D")
    result.booksLink should be (Some("http://book.uci.edu/ePOS?this_category=76&nbsp;store=446&nbsp;form=shared3/gm/main.html&nbsp;design=446"))
    result.graded should be (Some(true))
    result.status should be (Some("OPEN"))
  }
}
