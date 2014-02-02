import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.{SmallestMailboxRouter, RoundRobinRouter}
import com.typesafe.scalalogging.slf4j.Logger
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


object CollectorService {
  case class Todo(quarter: String, department: String, retryCount: Int = 0)

  case object StartScraper
}

class CollectorService(quarters: List[String], departments: List[String], baseUrl: String, debug: Boolean)
  extends Actor with ActorLogging {
  import CollectorService._
  import PageScraperService._

  val cpuCount = Runtime.getRuntime.availableProcessors()
  val pageScraper = context.actorOf(Props(classOf[PageScraperService], baseUrl, self)
    .withRouter(SmallestMailboxRouter(cpuCount)), "PageScraperService")

  var awaiting = 0
  var timeBegan: Long = 0
  val failedPages: ListBuffer[String] = ListBuffer()

  def shutdown() = {
    if (failedPages.isEmpty) log.info("All pages were successfully scraped.")
    else log.error("Scrapes failed on the following departments: " + failedPages)

    log.info("We took " + ((System.currentTimeMillis() - timeBegan) / 1000.0) + " seconds to finish.")
    log.info("Shutting down collector service.")

    context.system.shutdown()
  }

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
        self ! StartScrapingPage(todo)
      })
    }

    case StartScrapingPage(Todo(quarter, department, tryCount)) =>
      if (tryCount < 3) pageScraper ! StartScrapingPage(Todo(quarter, department, tryCount + 1))
      else self ! ScrapingFailed(Todo(quarter, department, tryCount))

    case result: WebSoc => {
      awaiting = awaiting - 1
      log.info(awaiting + " left.")
      if (awaiting == 0) shutdown()
    }
    case ScrapingFailed(Todo(quarter, department, retryCount)) => {
      awaiting = awaiting - 1
      log.error(department + " failed after " + retryCount + " tries!")

      failedPages += department

      if (awaiting == 0) shutdown()
    }
    case x => {
      log.error("Unhandled message in collector service: " + x)
    }
  }
}
