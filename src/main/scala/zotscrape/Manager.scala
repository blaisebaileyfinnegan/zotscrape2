package zotscrape

import akka.actor._
import com.typesafe.config.ConfigObject
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import zotscrape.catalogue.CatalogueService
import zotscrape.collector.CollectorService
import zotscrape.writer.WriterService

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success
import scalaj.http.{Http, HttpOptions}

object Manager {
  case object Done
  case object Start

  case object Shutdown
}

class Manager(baseUrl: String,
              potentialQuarters: List[String],
              debug: Boolean,
              jdbcUrl: String,
              username: String,
              password: String,
              catalogueUrl: String,
              catalogueEnabled: Boolean,
              params: ConfigObject,
              timestamp: java.sql.Timestamp,
              consumer: Option[ActorRef]) extends Actor with ActorLogging {

  var collectorServiceDone = false

  val writerService = context.actorOf(
    Props(classOf[WriterService], jdbcUrl, username, password, timestamp),
    "WriterService")

  val catalogueService: Option[ActorRef] = if (catalogueEnabled) {
    Some(context.actorOf(
      Props(classOf[CatalogueService], catalogueUrl, params, writerService),
      "CatalogueService"))
  } else {
    None
  }

  val collectorService = context.actorOf(
    Props(classOf[CollectorService], baseUrl, debug, catalogueService.getOrElse(writerService)),
    "CollectorService")

  def receive = {
    case Manager.Start => {
      writerService ! WriterService.Start
    }

    case WriterService.Done => {
      log.info("Shutting down manager.")
      self ! PoisonPill
    }

    case WriterService.Ready => {
      def getDropdownValues(document: Document, predicate: (Element) => Boolean): List[String] = {
        val selects = document.getElementsByTag("select")
          .toList
          .filter(predicate)

        if (selects.size != 1) throw new Error("Unexpected amount of term dropdowns! Found " + selects.size)
        else selects(0).children().toList.map(_.attr("value")).map(_.trim)
      }

      lazy val getDocument = Future {
        log.info("Retrieving quarters and departments...")
        Jsoup.parse(Http(baseUrl).options(HttpOptions.connTimeout(5000)).asString)
      }

      getDocument onComplete {
        case Success(document) => {
          val quarters = getDropdownValues(document, _.attr("name") == "YearTerm")
          val departments = getDropdownValues(document, _.attr("name") == "Dept")
            .toStream
            .filter(_ != "ALL")

          val targetQuarters =
            if (potentialQuarters.isEmpty) List(quarters.head)
            else potentialQuarters


          collectorService ! CollectorService.Start(targetQuarters, departments)
        }

        case _ => {
          log.error("Could not retrieve quarters and departments!")
          self ! PoisonPill
        }
      }
    }
  }

}