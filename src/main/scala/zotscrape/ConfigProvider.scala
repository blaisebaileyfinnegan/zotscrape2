package zotscrape

import com.typesafe.config.ConfigFactory

import scala.collection.JavaConversions._

trait ConfigProvider {
  val configService: Config

  class Config(cfg: Option[String]) {
    val config = ConfigFactory.load(cfg getOrElse "application.json")

    object Jdbc {
      val url = config.getString("jdbc.url")
      val username = config.getString("jdbc.username")
      val password = config.getString("jdbc.password")
    }

    object Scraper {
      val baseUrl = config.getString("scraper.baseUrl")
      val potentialQuarters = config.getStringList("scraper.targetQuarters").toList
      val debug = config.getBoolean("scraper.debug")
      val initialDelay = config.getLong("scraper.initialDelay")
      val frequency = config.getLong("scraper.frequency")

      object Catalogue {
        val disabled = config.getBoolean("scraper.catalogue.disable")
        val url = config.getString("scraper.catalogue.url")
        val params = config.getObject("scraper.catalogue.params")
      }
    }
  }
}

