package zotscrape

import akka.actor.{Props, ActorLogging, Actor}
import akka.routing.SmallestMailboxRouter
import scala.collection.mutable.ListBuffer


object CollectorService {
  case class Todo(quarter: String, department: String, retryCount: Int = 0)
  case object Done
}

class CollectorService(quarters: Seq[String], departments: Seq[String], baseUrl: String, debug: Boolean)
  extends Actor with ActorLogging {
  import CollectorService._
  import ScraperWorker._
  import Manager._

  val cpuCount = Runtime.getRuntime.availableProcessors()
  val pageScraper = context.actorOf(Props(classOf[ScraperWorker], baseUrl, self)
    .withRouter(SmallestMailboxRouter(cpuCount)), "PageScraperService")

  var awaiting = 0
  var totalParsingTime: Long = 0 // Excluding I/O
  var timeBegan: Long = 0

  val failedPages: ListBuffer[String] = ListBuffer()

  override def postStop() = {
    if (failedPages.isEmpty) log.info("All pages were successfully scraped.")
    else log.error("Scrapes failed on the following departments: " + failedPages)

    log.info("We took " + ((System.currentTimeMillis() - timeBegan) / 1000.0) + " seconds to finish.")
    log.info("We spent " + (totalParsingTime / 1000.0) + " seconds parsing.")
  }

  def receive = {
    case StartCollectorService => {
      val pairs = for {
        quarter <- quarters
        department <- departments
      } yield Todo(quarter, department)

      val left: List[Todo] = if (debug) List(pairs.head) else pairs.toList
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

    case ScrapingDone(quarter, department, websoc, time) => {
      totalParsingTime += time
      awaiting -= 1

      log.info(awaiting + " left.")

      context.parent ! WriterService.WriteDocument(quarter, department, websoc)

      if (awaiting == 0) context.parent ! Done
    }

    case ScrapingFailed(Todo(quarter, department, retryCount)) => {
      awaiting -= 1
      log.error(department + " failed after " + retryCount + " tries!")

      failedPages += department

      if (awaiting == 0) context.parent ! Done
    }
    case x => {
      log.error("Unhandled message in collector service: " + x)
    }
  }
}
