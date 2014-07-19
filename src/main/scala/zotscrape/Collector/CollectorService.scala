package zotscrape.collector

import akka.actor._
import akka.routing.FromConfig
import zotscrape.WebSoc

import scala.collection.mutable.ListBuffer


object CollectorService {
  case object Done
  case class QueueScrapeTask(todo: Todo)
  case class Start(targetQuarters: Seq[String], departments: Seq[String])
  case class Todo(quarter: String, department: String, retryCount: Int = 0)
  case class Document(courses: String, department: String, websoc: WebSoc)
}

class CollectorService(baseUrl: String, debug: Boolean, catalogueService: ActorRef)
  extends Actor with ActorLogging {
  import zotscrape.collector.CollectorService._

  val pageScraperRouter = context.actorOf(Props(classOf[ScraperWorker], baseUrl, self)
    .withRouter(FromConfig()), "ScraperWorkerRouter")

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
    case Start(quarters, departments) => {
      val pairs = for {
        quarter <- quarters
        department <- departments
      } yield Todo(quarter, department)

      val left: List[Todo] = if (debug) List(pairs.head) else pairs.toList
      awaiting = left.size
      timeBegan = System.currentTimeMillis()

      log.info("Pages to scrape: " + awaiting)

      left foreach { todo =>
        self ! QueueScrapeTask(todo)
      }
    }

    case QueueScrapeTask(Todo(quarter, department, tryCount)) => {
      if (tryCount < 3) pageScraperRouter ! ScraperWorker.ScrapePage(Todo(quarter, department, tryCount + 1))
      else self ! ScraperWorker.ScrapingFailed(Todo(quarter, department, tryCount))
    }

    case ScraperWorker.ScrapingDone(quarter, department, websoc, time) => {
      totalParsingTime += time
      awaiting -= 1

      log.info(awaiting + " left.")

      catalogueService ! Document(quarter, department, websoc)

      if (awaiting == 0) {
        catalogueService ! Done
        self ! PoisonPill
      }
    }

    case ScraperWorker.ScrapingFailed(Todo(quarter, department, retryCount)) => {
      awaiting -= 1
      log.error(department + " failed after " + retryCount + " tries!")

      failedPages += department

      if (awaiting == 0) {
        catalogueService ! Done
        self ! PoisonPill
      }
    }
  }
}
