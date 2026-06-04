import sbt._
import Keys._


lazy val commonSettings = Seq(
  name := "Soriento",
  organization := "com.emotioncity",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.12.8",
  offline := false,
  fork in run := true,
  fork in Test := true,
  testForkedParallel := false,
  parallelExecution in Test := false,
  autoCompilerPlugins := true,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:implicitConversions"),
  resolvers ++= Seq(
    Classpaths.typesafeReleases,
    Resolver.sonatypeRepo("snapshots"),
    "Sonatype HTTPS RELEASES" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe Maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  ),
libraryDependencies ++= Seq(
  "org.scala-lang" % "scala-reflect" % "2.12.8",
  "com.tinkerpop.blueprints" % "blueprints-core" % "2.6.0",
  "com.orientechnologies" % "orientdb-core" % "3.0.0",
  "com.orientechnologies" % "orientdb-graphdb" % "3.0.0",

  "io.circe" %% "circe-core" % "0.11.1",
  "io.circe" %% "circe-generic" % "0.11.1",
  "io.circe" %% "circe-parser" % "0.11.1",

  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test",
)
)

lazy val root = (project in file(".")).settings(commonSettings: _*)

