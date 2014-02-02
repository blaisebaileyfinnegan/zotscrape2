import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import CollectorService._
import scala.concurrent.Future
import scalaj.http.{HttpException, Http}
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global

object PageScraperService {
  case class StartScrapingPage(todo: Todo)
  case class ScrapingFailed(todo: Todo)

  var times = 0
}

class PageScraperService(baseUrl: String, collectorService: ActorRef) extends Actor with ActorLogging {
  import PageScraperService._

  def getDocument(quarter: String, department: String): Future[xml.Elem] = Future {
    Http(baseUrl).param("YearTerm", quarter)
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

  def receive = {
    case StartScrapingPage(Todo(quarter, department, tryCount)) => {
      val message = "Scraping " + department + " in " + quarter
      if (tryCount > 1) {
        log.warning("Retry #" + tryCount + ": " + message)
      } else {
        log.info(message)
      }

      val document = getDocument(quarter, department)

      def retry() = collectorService ! StartScrapingPage(Todo(quarter, department, tryCount))

      document onFailure {
        case _: java.net.SocketTimeoutException => retry()
        case _: HttpException => retry()
        case unrecognized => collectorService ! unrecognized
      }

      document.flatMap(DocumentParser(_)) onSuccess {
        case x => collectorService ! x
      }
    }
    case unrecognized => log.error("Unrecognized message sent to scraper service: " + unrecognized)
  }
}
