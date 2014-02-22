package antbutter

import com.typesafe.config.ConfigFactory
import scala.collection.JavaConversions._

trait ConfigProvider {
  val configService: Config

  class Config {
    lazy val config = ConfigFactory.load("application.conf")

    lazy val jdbcUrl = config.getString("jdbcUrl")
    lazy val username = config.getString("username")
    lazy val password = config.getString("password")
    lazy val baseUrl = config.getString("baseUrl")
    lazy val potentialQuarters = config.getStringList("targetQuarters").toList
    lazy val debug = config.getBoolean("debug")
  }
}
