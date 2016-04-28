resolvers += "Sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

resolvers += Classpaths.sbtPluginReleases

resolvers += Resolver.url("scoverage-bintray", url("https://dl.bintray.com/sksamuel/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.7")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.1")

addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0")

//addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.8.0")
