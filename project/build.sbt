resolvers += Classpaths.sbtPluginReleases

addSbtPlugin("com.sksamuel.scoverage" %% "sbt-scoverage" % "0.95.7")

resolvers += "spray repo" at "http://repo.spray.io" // not needed for sbt >= 0.12

addSbtPlugin("io.spray" % "sbt-revolver" % "0.7.1")

addSbtPlugin("com.gu" % "sbt-grunt-plugin" % "0.1")

