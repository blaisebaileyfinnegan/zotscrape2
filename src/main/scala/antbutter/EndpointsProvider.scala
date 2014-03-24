package antbutter

import akka.actor.{Actor, ActorRefFactory}
import java.sql.Timestamp
import spray.routing.{RoutingSettings, ExceptionHandler, HttpService}
import spray.json._
import spray.http.StatusCodes

import zotscrape.Manager

object EndpointsProvider extends ConfigProvider with RepoProvider with BigPayloads {
  override val repoService: EndpointsProvider.Repo = new Repo
  override val configService: EndpointsProvider.Config = new Config

  var maxTimestamp: Timestamp = repoService.maxTimestamp.getOrElse(throw new Error)

  class EndpointsActor extends Actor with Endpoints {
    override implicit def actorRefFactory: ActorRefFactory = context
    implicit def routingSettings = RoutingSettings.default(actorRefFactory)
    implicit def handler = ExceptionHandler {
      case e: Exception => requestUri { uri =>
        complete(StatusCodes.BadRequest)
      }
    }

    override def receive = {
      case Manager.Done => maxTimestamp = repoService.maxTimestamp.getOrElse(throw new Error)
      case _ => runRoute(routes)
    }
  }

  trait Endpoints extends HttpService {
    import spray.httpx.SprayJsonSupport._
    import DefaultJsonProtocol._

    val routes =
      path("history") {
        get {
          complete {
            repoService.timestamps.map(_.getTime)
          }
        }
      } ~
      path("terms") {
        get {
          complete {
            repoService.terms.map(PayloadPacker.term)
          }
        }
      } ~
      path("term" / IntNumber) { id =>
        get {
          complete {
            repoService.termById(id).map(PayloadPacker.term)
          }
        }
      } ~
      path("term" / Segment) { (yyyyst: String) =>
        get {
          complete {
            repoService.termByYyyyst(yyyyst).map(PayloadPacker.term)
          }
        }
      } ~
      path("schools") {
        get {
          complete {
            repoService.schools.map(PayloadPacker.school)
          }
        }
      } ~
      path("school" / IntNumber) { id =>
        get {
          complete {
            repoService.schoolById(id).map(PayloadPacker.school)
          }
        }
      } ~
      path("school" / Segment) { code =>
        get {
          complete {
            repoService.schoolByCode(code).map(PayloadPacker.school)
          }
        }
      } ~
      path("school" / IntNumber / "departments") { id =>
        get {
          complete {
            repoService.departmentsBySchoolId(id).map(PayloadPacker.department)
          }
        }
      } ~
      path("departments") {
        get {
          complete {
            repoService.departments.map(PayloadPacker.department)
          }
        }
      } ~
      path("department" / IntNumber) { id =>
        get {
          complete {
            repoService.departmentById(id).map(PayloadPacker.department)
          }
        }
      } ~
      path("department" / IntNumber / "courses") { id =>
        get {
          complete {
            repoService.coursesByDepartmentId(id).map(PayloadPacker.course)
          }
        }
      } ~
      path("courses") {
        get {
          complete {
            repoService.courses.map(PayloadPacker.course)
          }
        }
      } ~
      path("section" / IntNumber) { sectionId =>
        get {
          complete {
            repoService.sectionById(sectionId).map(PayloadPacker.section)
          }
        }
      } ~
      path("section" / IntNumber / IntNumber) { (ccode, termId) => {
        get {
          complete {
            getSectionWithExtras(ccode, termId)(maxTimestamp)
          }
        }
      }} ~
      path("section" / IntNumber / IntNumber / LongNumber) { (ccode, termId, timestamp) => {
        get {
          complete {
            getSectionWithExtras(ccode, termId)(new Timestamp(timestamp))
          }
        }
      }} ~
      path("meetings" / IntNumber) { sectionId =>
        get {
          complete {
            repoService.meetingsBySectionId(sectionId).map(PayloadPacker.meeting)
          }
        }
      } ~
      path("final" / IntNumber) { sectionId =>
        get {
          complete {
            repoService.finalBySectionId(sectionId).map(PayloadPacker.fina)
          }
        }
      } ~
      path("restrictions" / IntNumber) { sectionId =>
        get {
          complete {
            repoService.restrictionsBySectionId(sectionId).map(PayloadPacker.restriction)
          }
        }
      } ~
      path("instructors" / IntNumber) { sectionId =>
        get {
          complete {
            repoService.instructorsBySectionId(sectionId).map(PayloadPacker.instructor)
          }
        }
      } ~
      path("enrollment" / IntNumber) { sectionId =>
        get {
          complete {
            repoService.enrollmentBySectionId(sectionId).map(PayloadPacker.enrollment)
          }
        }
      } ~
      path("") {
        get {
          getFromResource("dist/index.html")
        }
      } ~
      get {
        getFromResourceDirectory("dist")
      }
  }

}
