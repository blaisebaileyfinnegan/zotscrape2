name := """zotscrape2"""

version := "0.1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.4",
  "com.typesafe.akka" %% "akka-remote" % "2.3.4",
  "org.scalatest" %% "scalatest" % "2.2.0" % "test",
  "com.typesafe" % "config" % "1.2.1",
  "org.jsoup" % "jsoup" % "1.7.2",
  "org.scalaj" %% "scalaj-http" % "0.3.15",
  "com.github.nscala-time" %% "nscala-time" % "1.2.0",
  "com.typesafe.scala-logging" % "scala-logging_2.11" % "3.0.0",
  "com.typesafe.slick" %% "slick" % "2.1.0-M2",
  "mysql" % "mysql-connector-java" % "5.1.29",
  "org.scalatra" %% "scalatra" % "2.3.0",
  "org.scalatra" %% "scalatra-json" % "2.3.0",
  "org.json4s"   %% "json4s-jackson" % "3.2.10",
  "org.eclipse.jetty" % "jetty-server" % "8.1.15.v20140411",
  "org.eclipse.jetty" % "jetty-webapp" % "8.1.15.v20140411"
)

mainClass in Revolver.reStart := Some("Boot")

Revolver.settings

