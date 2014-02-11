package zotscrape

import com.typesafe.config.{ConfigFactory, Config}
import akka.actor.{Props, ActorSystem}
import scala.collection.JavaConversions._

object Main extends App {
  ZotScrape(ConfigFactory.load("application.conf"))
}

object ZotScrape {
  def apply(config: Config) {
    val baseUrl = config.getString("baseUrl")
    val potentialQuarters = config.getStringList("targetQuarters").toList
    val debug = config.getBoolean("debug")
    val jdbcUrl = config.getString("jdbcUrl")
    val username = config.getString("username")
    val password = config.getString("password")

    val timestamp = new java.sql.Timestamp(new java.util.Date().getTime)
    val system = ActorSystem("zotscrape-system")
    val conductor = system.actorOf(
      Props(classOf[Manager], baseUrl, potentialQuarters, debug, jdbcUrl, username, password, timestamp),
      "Manager"
    )

    conductor ! Manager.Start
  }
}
