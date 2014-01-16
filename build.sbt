name := """zotscrape2"""

version := "1.0"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.1",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test",
  "com.typesafe" % "config" % "1.0.0",
  "org.jsoup" % "jsoup" % "1.7.2",
  "org.scalaj" %% "scalaj-http" % "0.3.12",
  "com.github.nscala-time" %% "nscala-time" % "0.6.0"
)
