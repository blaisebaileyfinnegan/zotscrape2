import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.RoundRobinRouter


object CollectorService {
  case class Todo(quarter: String, department: String)

  case object StartScraper
}

class CollectorService(quarters: List[String], departments: List[String], baseUrl: String, debug: Boolean) extends Actor with ActorLogging {
  import CollectorService._
  import PageScraperService._

  val pageScraper = context.actorOf(Props(classOf[PageScraperService], baseUrl)
    .withRouter(RoundRobinRouter(Runtime.getRuntime.availableProcessors())), "PageScraperService")

  def receive = {
    case StartScraper => {
      val pairs = for {
        quarter <- quarters
        department <- departments
      } yield Todo(quarter, department)

      if (!debug) pairs.foreach(pageScraper ! StartScrapingPage(_))
      else pageScraper ! StartScrapingPage(pairs.head)
    }
  }
}
