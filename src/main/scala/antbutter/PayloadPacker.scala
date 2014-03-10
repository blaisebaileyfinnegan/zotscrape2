package antbutter

import zotscrape.Schema._
import spray.json._
import zotscrape.WebSoc.Days

object PayloadPacker extends DefaultJsonProtocol {
  def term(t: Term#TableElementType) = t match {
    case (id, yyyyst, year, quarterName, termStatusMsg) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "yyyyst" -> JsString(yyyyst),
      "year" -> JsNumber(year),
      "name" -> JsString(quarterName),
      "msg" -> JsString(termStatusMsg))
  }

  def school(s: School#TableElementType) = s match {
    case (id, code, schoolName) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "code" -> JsString(code),
      "name" -> JsString(schoolName)
    )
  }

  def department(d: Department#TableElementType) = d match {
    case (id, schoolId, code, departmentName) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "schoolId" -> JsNumber(schoolId),
      "code" -> JsString(code),
      "name" -> JsString(departmentName)
    )
  }

  def course(c: Course#TableElementType) = c match {
    case (id, departmentId, number, title, prereqLink) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "departmentId" -> JsNumber(departmentId),
      "number" -> JsString(number),
      "title" -> JsString(title),
      "prereqLink" -> JsString(prereqLink)
    )
  }

  def section(s: Section#TableElementType) = s match {
    case (id, courseId, termId, timestamp, ccode, typ, num, units, booksLink, graded, status) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "courseId" -> JsNumber(courseId),
      "termId" -> JsNumber(termId),
      "timestamp" -> JsNumber(timestamp.getTime),
      "ccode" -> JsNumber(ccode),
      "typ" -> typ.map(JsString(_)).getOrElse(JsNull),
      "num" -> num.map(JsString(_)).getOrElse(JsNull),
      "units" -> units.map(JsString(_)).getOrElse(JsNull),
      "booksLink" -> booksLink.map(JsString(_)).getOrElse(JsNull),
      "graded" -> graded.map(JsBoolean(_)).getOrElse(JsNull),
      "status" -> status.map(JsString(_)).getOrElse(JsNull)
    )
  }

  def restriction(r: Restriction#TableElementType) = r match {
    case (code, definition) => Map[String, JsValue](
      "code" -> JsString(code),
      "definition" -> definition.map(JsString(_)).getOrElse(JsNull)
    )
  }

  def sectionRestriction(sr: SectionRestriction#TableElementType) = sr match {
    case (sectionId, restrictionCode) => Map[String, JsValue](
      "sectionId" -> JsNumber(sectionId),
      "restrictionCode" -> JsString(restrictionCode)
    )
  }

  implicit val daysFormat = jsonFormat7(Days)
  def meeting(m: Meeting#TableElementType) = m match {
    case (id, sectionId, begin, end, building, room, roomLink, days) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "sectionId" -> JsNumber(sectionId),
      "begin" -> begin.map(JsNumber(_)).getOrElse(JsNull),
      "end" -> end.map(JsNumber(_)).getOrElse(JsNull),
      "building" -> building.map(JsString(_)).getOrElse(JsNull),
      "room" -> room.map(JsString(_)).getOrElse(JsNull),
      "roomLink" -> roomLink.map(JsString(_)).getOrElse(JsNull),
      "days" -> days.toJson
    )
  }

  def fina(f: Final#TableElementType) = f match {
    case (id, sectionId, date, day, begin, end) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "sectionId" -> JsNumber(sectionId),
      "date" -> date.map(JsString(_)).getOrElse(JsNull),
      "day" -> day.map(JsString(_)).getOrElse(JsNull),
      "begin" -> begin.map(JsNumber(_)).getOrElse(JsNull),
      "end" -> end.map(JsNumber(_)).getOrElse(JsNull)
    )
  }

  def instructor(i: Instructor#TableElementType) = i match {
    case (id, sectionId, name) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "sectionId" -> JsNumber(sectionId),
      "name" -> JsString(name)
    )
  }

  def enrollment(e: Enrollment#TableElementType) = e match {
    case (id, sectionId, enrollment) => Map[String, JsValue](
      "id" -> JsNumber(id),
      "sectionId" -> JsNumber(sectionId),
      "max" -> enrollment.max.map(JsNumber(_)).getOrElse(JsNull),
      "enrolled" -> enrollment.enrolled.map(JsNumber(_)).getOrElse(JsNull),
      "req" -> enrollment.req.map(JsNumber(_)).getOrElse(JsNull),
      "newOnly" -> enrollment.newOnly.map(JsNumber(_)).getOrElse(JsNull),
      "waitList" -> enrollment.waitList.map(JsNumber(_)).getOrElse(JsNull),
      "waitCap" -> enrollment.waitCap.map(JsNumber(_)).getOrElse(JsNull),
      "xlist" -> enrollment.xlist.map(JsNumber(_)).getOrElse(JsNull)
    )
  }
}
