package zotscrape

import antbutter._

import akka.actor.{ActorRef, Props, ActorSystem}

object Main extends App with ConfigProvider {
  override val configService = new Config

  ZotScrape.apply(configService)
}

object ZotScrape {
  def apply(config: Main.Config) {
    val system = ActorSystem("zotscrape-solo")
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
        config.Scraper.Catalogue.disabled,
        config.Scraper.Catalogue.url,
        config.Scraper.Catalogue.params,
        timestamp,
        consumer),
      "Manager")

    conductor ! Manager.Start
  }
}
