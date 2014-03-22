package antbutter

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

trait ConfigProvider {
  val configService: Config

  class Config {
    lazy val config = ConfigFactory.load("application.json")

    lazy val jdbcUrl = config.getString("jdbc.url")
    lazy val username = config.getString("jdbc.username")
    lazy val password = config.getString("jdbc.password")

    lazy val baseUrl = config.getString("scraper.baseUrl")
    lazy val potentialQuarters = config.getStringList("scraper.targetQuarters").toList
    lazy val debug = config.getBoolean("scraper.debug")
    lazy val initialDelay = config.getLong("scraper.initialDelay")
    lazy val frequency = config.getLong("scraper.frequency")
  }
}
