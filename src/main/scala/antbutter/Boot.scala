package antbutter

import akka.actor.{Props, ActorSystem}
import spray.can.Http
import akka.io.IO
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import zotscrape._

object Boot extends App with ConfigProvider {
  val configService = new Config

  val system = ActorSystem("antbutter")
  val service = system.actorOf(Props(classOf[EndpointsProvider.EndpointsActor]), "endpoints")

  IO(Http)(system) ! Http.Bind(service, "localhost", port = 7070)

  system.scheduler.schedule(configService.initialDelay milliseconds, configService.frequency milliseconds) {
    ZotScrape.scrape(configService, system)
  }
}
