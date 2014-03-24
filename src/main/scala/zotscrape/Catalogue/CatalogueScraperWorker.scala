package zotscrape.Catalogue

import akka.actor._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConverters._

import com.typesafe.config.ConfigObject
import scalaj.http.{HttpOptions, Http}

import zotscrape.Collector.CollectorService
import zotscrape.WebSoc.Course

class CatalogueScraperWorker(url: String, params: ConfigObject) extends Actor with ActorLogging {
  def receive = {
    case CollectorService.Document(quarter, department, websoc) => {
      val courses: Iterable[(String, Course)] = for {
        courseList <- websoc.courseList.toList
        school <- courseList.schools
        dept <- school.departments
        course <- dept.courses
        number <- course.number
      } yield (number, course)

      courses foreach {
        case (number, course) => Future {
          Http(url).options(HttpOptions.connTimeout(5000))
            .options(HttpOptions.readTimeout(5000))
            .params(params.unwrapped().asScala.toMap.mapValues(_.toString))
            .param("code", department + " " + number)
            .asXml
        } onComplete {
          case Success(elem) => {
            sender ! CatalogueInfo(elem.toString(), "")
          }

          case Failure(x) => log.error(x.toString)
        }
      }
    }
  }
}
