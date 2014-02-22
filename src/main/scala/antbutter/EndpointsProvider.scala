package antbutter

import spray.routing.{RoutingSettings, ExceptionHandler, HttpService}
import akka.actor.{Actor, ActorRefFactory}
import spray.json._
import DefaultJsonProtocol._
import zotscrape._
import java.sql.Timestamp
import spray.httpx.marshalling.ToResponseMarshallable

object EndpointsProvider extends ConfigProvider with RepoProvider {
  override val repoService: EndpointsProvider.Repo = new Repo
  override val configService: EndpointsProvider.Config = new Config

  class EndpointsActor extends Actor with Endpoints {
    override implicit def actorRefFactory: ActorRefFactory = context
    implicit def routingSettings = RoutingSettings.default(actorRefFactory)
    implicit def handler = ExceptionHandler {
      case e: Exception => requestUri { uri =>
        complete("error")
      }
    }

    override def receive: Actor.Receive = runRoute(routes)
  }

  trait Endpoints extends HttpService {
    import spray.httpx.SprayJsonSupport._
    import DefaultJsonProtocol._

    val routes = {
      path("history") {
        get {
          complete {
            repoService.timestamps.map(_.getTime)
          }
        }
      }
    }
  }

}
