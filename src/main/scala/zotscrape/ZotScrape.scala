package zotscrape

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object Main extends App with ConfigProvider {
  override val configService = new Config(None)

  ZotScrape(configService)
}

object ZotScrape {
  def apply(config: Main.Config) {
    val system = ActorSystem("zotscrape-solo", config.config)
    scrape(config, system)
  }

  def scrape(config: ConfigProvider#Config, system: ActorSystem, consumer: Option[ActorRef] = None) {
    val timestamp = new java.sql.Timestamp(new java.util.Date().getTime)
    val conductor = system.actorOf(
      Props(classOf[Manager],
        config.Scraper.baseUrl,
        config.Scraper.potentialQuarters,
        config.Scraper.debug,
        config.Jdbc.url,
        config.Jdbc.username,
        config.Jdbc.password,
        config.Scraper.Catalogue.url,
        false,
        config.Scraper.Catalogue.params,
        timestamp,
        consumer),
      "Manager")

    conductor ! Manager.Start
  }
}
