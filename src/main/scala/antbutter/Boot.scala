package antbutter

import scala.util.{Success, Failure, Try}
import java.sql.Timestamp
import spray.routing.SimpleRoutingApp
import akka.actor.{Props, ActorSystem}
import spray.can.Http
import akka.io.IO

object Boot extends App {
  val system = ActorSystem("antbutter")
  val service = system.actorOf(Props(classOf[EndpointsProvider.EndpointsActor]), "endpoints")

  IO(Http)(system) ! Http.Bind(service, "localhost", port = 7070)
}
