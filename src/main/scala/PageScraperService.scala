import akka.actor.{Props, ActorLogging, Actor}
import CollectorService._
import scalaj.http.Http
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits.global

object PageScraperService {
  case class StartScrapingPage(todo: Todo)

  case class Done(result: WebSoc)
}

class PageScraperService(baseUrl: String) extends Actor with ActorLogging {
  import PageScraperService._

  def getDocument(quarter: String, department: String): xml.Elem =
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

  def receive = {
    case StartScrapingPage(Todo(quarter, department)) => {
      log.info("Scraping " + department + " in " + quarter)
      DocumentParser(getDocument(quarter, department)) pipeTo sender
    }
  }
}
