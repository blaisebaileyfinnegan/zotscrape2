name := """zotscrape2"""

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1",
  "com.typesafe.akka" %% "akka-remote" % "2.2.3",
  "org.scalatest" % "scalatest_2.10" % "2.0" % "test",
  "com.typesafe" % "config" % "1.0.0",
  "org.jsoup" % "jsoup" % "1.7.2",
  "org.scalaj" %% "scalaj-http" % "0.3.12",
  "com.github.nscala-time" %% "nscala-time" % "0.6.0",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "com.typesafe.slick" %% "slick" % "2.0.0",
  "mysql" % "mysql-connector-java" % "5.1.29"
)

ScoverageSbtPlugin.instrumentSettings

