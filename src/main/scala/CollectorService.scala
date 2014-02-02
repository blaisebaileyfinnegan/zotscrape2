import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import com.typesafe.scalalogging.slf4j.Logger
import scala.concurrent.Future


object CollectorService {
  case class Todo(quarter: String, department: String)

  case object StartScraper
}

class CollectorService(quarters: List[String], departments: List[String], baseUrl: String, debug: Boolean)
  extends Actor with ActorLogging {
  import CollectorService._
  import PageScraperService._

  val cpuCount = Runtime.getRuntime().availableProcessors()
  val pageScraper = context.actorOf(Props(classOf[PageScraperService], baseUrl)
    .withRouter(SmallestMailboxRouter(cpuCount)), "PageScraperService")

  var awaiting = 0
  var timeBegan: Long = 0

  def receive = {
    case StartScraper => {
      val pairs = for {
        quarter <- quarters
        department <- departments
      } yield Todo(quarter, department)

      val left: List[Todo] = if (debug) List(pairs.head) else pairs
      awaiting = left.size
      timeBegan = System.currentTimeMillis()

      log.info("Starting " + cpuCount + " scrapers.")
      log.info("Pages to scrape: " + awaiting)

      left foreach (todo => {
        pageScraper ! StartScrapingPage(todo)
      })
    }
    case result: WebSoc => {
      awaiting = awaiting - 1
      if (awaiting == 0) {
        log.info("We took " + ((System.currentTimeMillis() - timeBegan) / 1000.0) + " to finish.")
        context.system.shutdown()
      }
    }
  }
}
