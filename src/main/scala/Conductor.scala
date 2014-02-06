import akka.actor.{ActorRef, Props, ActorLogging, Actor}
import java.io.IOException
import org.jsoup.Jsoup
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scalaj.http.{HttpOptions, Http}
import org.jsoup.nodes.{Element, Document}
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

object Conductor {
  case object StartCollectorService
  case object StartWriterService

  case object Shutdown
}

class Conductor(baseUrl: String, potentialQuarters: List[String], debug: Boolean)
  extends Actor with ActorLogging {
  import ZotScrape._
  import Conductor._

  override def postStop() = log.info("Conductor going down.")

  def receive = {
    case CollectorService.Done =>
      system.shutdown()

    case StartConductor => {
      val chooseRecentQuarter = potentialQuarters.isEmpty

      def getDocument() = Future {
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

      getDocument() onComplete {
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
          system.shutdown()
        }
      }
    }
  }
}