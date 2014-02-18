package antbutter.controllers

import com.twitter.finatra.Controller
import antbutter.{App, PayloadPacker, Provider}
import scala.util.{Failure, Success}
import java.sql.Timestamp

class Sections extends Controller {
  get("/section/:id") { request =>
    val section = extractIntParam(request, "id").map(
      _.map(Provider.sectionById))

    (section match {
      case Some(Success(Some(json))) => render.json(PayloadPacker.section(json))
      case Some(Success(None)) => render.status(404).plain("")
      case _: Any => render.status(400).plain("")
    }).toFuture
  }

  get("/section/:ccode/:termId/:timestamp") { request =>
    val ccode = extractIntParam(request, "ccode")
    val termId = extractIntParam(request, "termId")
    implicit val timestamp = extractLongParam(request, "timestamp")
      .map(_.map(time => new Timestamp(time)))
      .getOrElse(Failure(new Error()))
      .getOrElse(App.currTimestamp)

    ((ccode, termId) match {
      case (_, None) => render.status(400).plain("Invalid term id")
      case (None, _) => render.status(400).plain("Invalid ccode")
      case (Some(Success(parsedCcode)), Some(Success(parsedTermId))) =>
        render.json(Provider.sectionByCcode(parsedCcode, parsedTermId)(timestamp) map {
          case (section, course, department, school) =>
            Map(
              "section" -> PayloadPacker.section(section),
              "course" -> PayloadPacker.course(course),
              "department" -> PayloadPacker.department(department),
              "school" -> PayloadPacker.school(school)
            )
        })
      case _: Any => render.status(400).plain("")
    }).toFuture
  }
}