import akka.actor._
import java.io.IOException
import org.jsoup.Jsoup
import scala.concurrent.Future
import scala.util.Success
import scala.util.{Success, Failure}
import scalaj.http.{HttpOptions, Http}
import org.jsoup.nodes.{Element, Document}
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

object Manager {
  case object StartCollectorService
  case class StartWriterService(timestamp: java.sql.Timestamp)

  case object Shutdown
}

class Manager(baseUrl: String, potentialQuarters: List[String], debug: Boolean)
  extends Actor with ActorLogging {
  import ZotScrape._
  import Manager._

  var collectorServiceDone = false
  var writerServiceDone = false

  val writerService = context.actorOf(
    Props(classOf[WriterService]),
    "WriterService"
  )

  def receive = {
    case CollectorService.Done => {
      collectorServiceDone = true
      sender ! PoisonPill

      writerService ! CollectorService.Done
    }

    case WriterService.Done => {
      writerServiceDone = true
      sender ! PoisonPill

      system.shutdown()
    }

    case StartConductor => {
      writerService ! StartWriterService(new java.sql.Timestamp(new java.util.Date().getTime))
    }

    case WriterService.Ready => {
      val chooseRecentQuarter = potentialQuarters.isEmpty

      lazy val getDocument = Future {
        log.info("Retrieving quarters and departments...")
        Jsoup.parse(Http(baseUrl).options(HttpOptions.connTimeout(5000)).asString)
      }

      def getDropdownValues(document: Document, predicate: (Element) => Boolean): List[String] = {
        val selects = document.getElementsByTag("select")
          .toList
          .filter(predicate)

        if (selects.size != 1) throw new Error("Unexpected amount of term dropdowns! Found " + selects.size)
        else selects(0).children().toList.map(_.attr("value")).map(_.trim)
      }

      getDocument onComplete {
        case Success(document) => {
          val quarters = getDropdownValues(document, _.attr("name") == "YearTerm")
          val departments = getDropdownValues(document, _.attr("name") == "Dept")
            .toStream
            .filter(_ != "ALL")

          val targetQuarters =
            if (chooseRecentQuarter) List(quarters.head)
            else potentialQuarters

          val collectorService = context.actorOf(
            Props(classOf[CollectorService], targetQuarters, departments, baseUrl, debug, writerService),
            "CollectorService"
          )

          collectorService ! StartCollectorService
        }
        case _ => {
          log.error("Could not retrieve quarters and departments!")
          system.shutdown()
        }
      }
    }

    case x => {
      log.error("unknown message sent to manager: " + x)
    }
  }
}