import akka.actor.{Props, ActorLogging, Actor}
import CollectorService._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import scalaj.http.Http

object PageScraperService {
  case class StartScrapingPage(todo: Todo)
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
      .param("ShowFinals", "on")
      .param("CancelledCourses", "Exclude")
      .asXml

  def receive = {
    case StartScrapingPage(Todo(quarter, department)) => {
      DocumentParser(getDocument(quarter, department))
    }
  }
}
