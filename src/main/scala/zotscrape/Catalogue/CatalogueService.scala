package zotscrape.catalogue

import java.util.concurrent.atomic.AtomicLong

import akka.actor._
import com.typesafe.config.ConfigObject
import zotscrape.collector.CollectorService
import zotscrape.WebSoc.Course

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaj.http.{Http, HttpOptions}

object CatalogueService {
  case object Done
  case object TryShutdown
}

class CatalogueService(url: String, params: ConfigObject, writerService: ActorRef)
  extends Actor with ActorLogging {

  val remaining: AtomicLong = new AtomicLong(0)

  override def receive = {
    case document @ CollectorService.Document(courses, department, websoc) => {
      writerService ! document

      val courses: Iterable[(String, Course)] = for {
        courseList <- websoc.courseList.toList
        school <- courseList.schools
        dept <- school.departments
        course <- dept.courses
      } yield (course.number.get, course)

      remaining.getAndAdd(courses.size)

      courses foreach { c =>
        Future {
          Http(url).options(HttpOptions.connTimeout(5000))
            .options(HttpOptions.readTimeout(5000))
            .params(params.unwrapped().asScala.toMap.mapValues(_.toString))
            .param("code", department + " " + c._1)
            .asXml
        } map { elem =>
          writerService ! CatalogueInfo(elem.toString(), "")
        } onComplete {
          case _ => {
            remaining.getAndDecrement
            log.info("Remaining catalogues " + remaining)
            self ! CatalogueService.TryShutdown
          }
        }
      }
    }

    case CollectorService.Done => {
      self ! CatalogueService.TryShutdown
    }

    case CatalogueService.TryShutdown => {
      if (remaining.get() == 0) {
        log.info("Shutting down catalogue service.")
        writerService ! CatalogueService.Done
        self ! PoisonPill
      }
    }
  }
}
