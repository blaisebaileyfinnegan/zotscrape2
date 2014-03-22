package zotscrape

import java.io.IOException

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success

import akka.actor._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
import scalaj.http.{HttpOptions, Http}

object Manager {
  case object Start

  case object Shutdown
}

class Manager(baseUrl: String,
              potentialQuarters: List[String],
              debug: Boolean,
              jdbcUrl: String,
              username: String,
              password: String,
              timestamp: java.sql.Timestamp) extends Actor with ActorLogging {
  import Manager._

  var collectorServiceDone = false
  var writerServiceDone = false

  val writerService = context.actorOf(
    Props(classOf[WriterService], jdbcUrl, username, password, timestamp),
    "WriterService"
  )

  def receive = {
    case Manager.Start => writerService ! WriterService.Start

    case x: WriterService.WriteDocument => writerService ! x

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

          val collectorService = context.actorOf(
            Props(classOf[CollectorService], targetQuarters, departments, baseUrl, debug),
            "CollectorService"
          )

          collectorService ! CollectorService.Start
        }

        case _ => {
          log.error("Could not retrieve quarters and departments!")
          self ! PoisonPill
        }
      }
    }

    case CollectorService.Done => {
      collectorServiceDone = true
      sender ! PoisonPill

      writerService ! CollectorService.Done
    }

    case WriterService.Done => {
      writerServiceDone = true
      log.info("All done!")

      self ! PoisonPill
    }
  }

}