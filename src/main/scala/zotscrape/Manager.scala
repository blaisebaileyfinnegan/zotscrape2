package zotscrape

import akka.actor._
import java.io.IOException
import org.jsoup.Jsoup
import scala.concurrent.Future
import scalaj.http.{HttpOptions, Http}
import org.jsoup.nodes.{Element, Document}
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Success

object Manager {
  case object Start
  case object StartCollectorService
  case object StartWriterService

  case object Shutdown
}

class Manager(baseUrl: String, potentialQuarters: List[String], debug: Boolean, jdbcUrl: String, username: String, password: String, timestamp: java.sql.Timestamp)
  extends Actor with ActorLogging {
  import Manager._

  var collectorServiceDone = false
  var writerServiceDone = false

  val writerService = context.actorOf(
    Props(classOf[WriterService], jdbcUrl, username, password, timestamp),
    "WriterService"
  )

  def receive = {
    case document: WriterService.WriteDocument => writerService ! document

    case CollectorService.Done => {
      collectorServiceDone = true
      sender ! PoisonPill

      writerService ! CollectorService.Done
    }

    case WriterService.Done => {
      writerServiceDone = true
      sender ! PoisonPill
      self ! PoisonPill
    }

    case Start => {
      writerService ! StartWriterService
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
            Props(classOf[CollectorService], targetQuarters, departments, baseUrl, debug),
            "CollectorService"
          )

          collectorService ! StartCollectorService
        }
        case _ => {
          log.error("Could not retrieve quarters and departments!")
          self ! PoisonPill
        }
      }
    }

    case x => {
      log.error("unknown message sent to manager: " + x)
    }
  }
}