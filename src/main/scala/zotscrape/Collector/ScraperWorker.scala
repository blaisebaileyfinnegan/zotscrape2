package zotscrape.Collector
import akka.actor.{ActorRef, ActorLogging, Actor}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaj.http.{HttpOptions, HttpException, Http}
import scala.concurrent.ExecutionContext.Implicits.global

import CollectorService._
import zotscrape.WebSoc

object ScraperWorker {
  case class ScrapePage(todo: Todo)
  case class ScrapingDone(quarter: String, department: String, websoc: WebSoc, time: Long)
  case class ScrapingFailed(todo: Todo)
}

class ScraperWorker(baseUrl: String, collectorService: ActorRef) extends Actor with ActorLogging {
  import ScraperWorker._

  def getDocument(quarter: String, department: String): Future[xml.Elem] = Future {
    Http(baseUrl).options(HttpOptions.connTimeout(2000))
      .options(HttpOptions.readTimeout(5000))
      .param("YearTerm", quarter)
      .param("Dept", department)
      .param("Submit", "Display+XML+Results")
      .param("FullCourses", "ANY")
      .param("ClassType", "ALL")
      .param("Division", "ANY")
      .param("Breadth", "ANY")
      .param("ShowFinals", "on")
      .param("MaxCap", "")
      .param("CourseNum", "")
      .param("CourseCodes", "")
      .param("InstrName", "")
      .param("CourseTitle", "")
      .param("Units", "")
      .param("Days", "")
      .param("FullCourses", "ANY")
      .param("CancelledCourses", "Exclude")
      .asXml
  }

  override def postStop() = log.info("Shutting down scraper.")

  def receive = {
    case ScrapePage(Todo(quarter, department, tryCount)) => {
      val document = getDocument(quarter, department)

      def retry() = {
        collectorService ! CollectorService.QueueScrapeTask(Todo(quarter, department, tryCount))
      }

      var beginTime: Long = 0
      document flatMap { x =>
        beginTime = System.currentTimeMillis()
        DocumentParser(x)
      } onComplete {
        case Success(websoc) =>
          collectorService ! ScrapingDone(quarter, department, websoc, System.currentTimeMillis() - beginTime)
        case Failure(x) => x match {
          case _: java.net.SocketTimeoutException => retry()
          case _: HttpException => retry()
          case _: java.lang.NumberFormatException =>
            collectorService ! ScrapingFailed(Todo(quarter, department, tryCount))
        }
      }
    }
  }
}
