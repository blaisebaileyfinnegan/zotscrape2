package antbutter

import com.typesafe.config.ConfigFactory

object Config {
  lazy val config = ConfigFactory.load("application.conf")

  lazy val jdbcUrl = config.getString("jdbcUrl")
  lazy val username = config.getString("username")
  lazy val password = config.getString("password")
}
