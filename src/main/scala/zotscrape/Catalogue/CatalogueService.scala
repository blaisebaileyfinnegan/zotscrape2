package zotscrape.Catalogue

import akka.actor._
import akka.routing.FromConfig
import akka.pattern.ask

import zotscrape.Collector.CollectorService
import scala.util.{Failure, Success}
import com.typesafe.config.ConfigObject

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

object CatalogueService {
  case object Done
  case object TryShutdown
}

class CatalogueService(disabled: Boolean, url: String, params: ConfigObject, writerService: ActorRef)
  extends Actor with ActorLogging {
  val catalogueWorkerRouter: Option[ActorRef] =
    if (disabled) None
    else Some(context.actorOf(Props(classOf[CatalogueScraperWorker], url, params).withRouter(FromConfig()), "CatalogueRouter"))

  var remaining: Long = 0

  override def receive = {
    case document: CollectorService.Document => {
      writerService ! CollectorService.Document

      catalogueWorkerRouter foreach { c =>
        remaining += 1
        (c ? document)(1.minute).mapTo[CatalogueInfo] andThen {
          case Success(catalogueInfo) => writerService ! catalogueInfo
          case Failure(exception) => log.info(exception.toString)
        } andThen {
          case _ => remaining -= 1
        } andThen {
          case _ => self ! CatalogueService.TryShutdown
        }
      }
    }

    case CollectorService.Done => {
      self ! CatalogueService.TryShutdown
    }

    case CatalogueService.TryShutdown => {
      if (remaining == 0) {
        writerService ! CatalogueService.Done
        self ! PoisonPill
      }
    }
  }
}
