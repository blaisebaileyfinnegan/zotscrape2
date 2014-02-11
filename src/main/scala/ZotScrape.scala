import akka.actor.{Props, ActorSystem}
import com.typesafe.config.ConfigFactory
import org.jsoup.Jsoup
import org.jsoup.nodes.{Element, Document}
import scala.collection.JavaConversions._
import scalaj.http.Http
import scalaj.http.HttpOptions


object ZotScrape extends App {
  case object StartConductor

  val config = ConfigFactory.load()

  val baseUrl = config.getString("baseUrl")
  val potentialQuarters = config.getStringList("targetQuarters").toList
  val debug = config.getBoolean("debug")

  val system = ActorSystem("zotscrape-system")
  val conductor = system.actorOf(
    Props(classOf[Manager], baseUrl, potentialQuarters, debug),
    "Manager"
  )

  conductor ! StartConductor
}