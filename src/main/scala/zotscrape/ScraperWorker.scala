package zotscrape

import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import CollectorService._
import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaj.http.{HttpOptions, HttpException, Http}
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global

object ScraperWorker {
  case class StartScrapingPage(todo: Todo)
  case class ScrapingFailed(todo: Todo)
  case class ScrapingDone(quarter: String, department: String, websoc: WebSoc, time: Long)

  var times = 0
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

  override def postStop() =
    log.info("Shutting down scraper.")

  def receive = {
    case StartScrapingPage(Todo(quarter, department, tryCount)) => {
      val message = "Retrieving " + department + " in " + quarter + "."
      if (tryCount > 1) {
        log.warning("Retry #" + tryCount + ": " + message)
      } else {
        log.info(message)
      }

      val document = getDocument(quarter, department)

      def retry() = {
        collectorService ! StartScrapingPage(Todo(quarter, department, tryCount))
      }

      var beginTime: Long = 0
      document.flatMap(x => {
        beginTime = System.currentTimeMillis()
        log.info("Started parsing " + department + " in " + quarter + ".")
        DocumentParser(x)
      }) onComplete {
        case Success(websoc) =>
          collectorService ! ScrapingDone(quarter, department, websoc, System.currentTimeMillis() - beginTime)
        case Failure(x) => x match {
          case _: java.net.SocketTimeoutException => retry()
          case _: HttpException => retry()
          case unrecognized => collectorService ! unrecognized
        }
      }
    }
    case unrecognized => log.error("Unrecognized message sent to scraper service: " + unrecognized)
  }
}
