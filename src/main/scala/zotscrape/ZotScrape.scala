package zotscrape

import antbutter._
import akka.actor.{Props, ActorSystem}
import scala.concurrent.duration._
import com.typesafe.config.Config

object Main extends App with ConfigProvider {
  override val configService = new Config

  ZotScrape.apply(configService)
}

object ZotScrape {
  def apply(config: Main.Config) {
    val system = ActorSystem("zotscrape-solo")
    scrape(config, system)
  }

  def scrape(config: ConfigProvider#Config, system: ActorSystem) {
    val timestamp = new java.sql.Timestamp(new java.util.Date().getTime)
    val conductor = system.actorOf(
      Props(classOf[Manager],
        config.baseUrl,
        config.potentialQuarters,
        config.debug,
        config.jdbcUrl,
        config.username,
        config.password,
        timestamp),
      "Manager"
    )

    conductor ! Manager.Start
  }
}
