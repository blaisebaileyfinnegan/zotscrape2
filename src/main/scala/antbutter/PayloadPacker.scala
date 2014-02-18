package antbutter

import zotscrape.Schema._

object PayloadPacker {
  def term(t: Term#TableElementType) = t match {
    case (id, yyyyst, year, quarterName, termStatusMsg) => Map(
      "id" -> id,
      "yyyyst" -> yyyyst,
      "year" -> year,
      "name" -> quarterName,
      "msg" -> termStatusMsg)
  }

  def school(s: School#TableElementType) = s match {
    case (id, code, schoolName) => Map(
      "id" -> id,
      "code" -> code,
      "name" -> schoolName
    )
  }

  def department(d: Department#TableElementType) = d match {
    case (id, schoolId, code, departmentName) => Map(
      "id" -> id,
      "schoolId" -> schoolId,
      "code" -> code,
      "name" -> departmentName
    )
  }

  def course(c: Course#TableElementType) = c match {
    case (id, departmentId, number, title, prereqLink) => Map(
      "id" -> id,
      "departmentId" -> departmentId,
      "number" -> number,
      "title" -> title,
      "prereqLink" -> prereqLink
    )
  }

  def section(s: Section#TableElementType) = s match {
    case (id, courseId, termId, timestamp, ccode, typ, num, units, booksLink, graded, status) => Map(
      "id" -> id,
      "courseId" -> courseId,
      "termId" -> termId,
      "timestamp" -> timestamp,
      "ccode" -> ccode,
      "typ" -> typ,
      "num" -> num,
      "units" -> units,
      "booksLink" -> booksLink,
      "graded" -> graded,
      "status" -> status
    )
  }

  def restriction(r: Restriction#TableElementType) = r match {
    case (code, definition) => Map(
      "code" -> code,
      "definition" -> definition
    )
  }

  def sectionRestriction(sr: SectionRestriction#TableElementType) = sr match {
    case (sectionId, restrictionCode) => Map(
      "sectionId" -> sectionId,
      "restrictionCode" -> restrictionCode
    )
  }

  def meeting(m: Meeting#TableElementType) = m match {
    case (id, sectionId, begin, end, building, room, roomLink, days) => Map(
      "id" -> id,
      "sectionId" -> sectionId,
      "begin" -> begin,
      "end" -> end,
      "building" -> building,
      "room" -> room,
      "roomLink" -> roomLink,
      "days" -> days
    )
  }

  def fina(f: Final#TableElementType) = f match {
    case (id, sectionId, date, day, begin, end) => Map(
      "id" -> id,
      "sectionId" -> sectionId,
      "date" -> date,
      "day" -> day,
      "begin" -> begin,
      "end" -> end
    )
  }

  def instructor(i: Instructor#TableElementType) = i match {
    case (id, sectionId, name) => Map(
      "id" -> id,
      "sectionId" -> sectionId,
      "name" -> name
    )
  }

  def enrollment(e: Enrollment#TableElementType) = e match {
    case (id, sectionId, enrollment) => Map(
      "id" -> id,
      "sectionId" -> sectionId,
      "max" -> enrollment.max,
      "enrolled" -> enrollment.enrolled,
      "req" -> enrollment.req,
      "newOnly" -> enrollment.newOnly,
      "waitList" -> enrollment.waitList,
      "waitCap" -> enrollment.waitCap,
      "xlist" -> enrollment.xlist
    )
  }
}
