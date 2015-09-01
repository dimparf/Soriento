import sbt.Keys._
import sbt._

lazy val commonSettings = Seq(
  name := "Soriento",
  coverageEnabled := true,
  organization := "com.emotioncity",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  offline := false,
  fork in run := true,
  fork in Test := true,
  testForkedParallel := true,
  parallelExecution in Test := false,
  autoCompilerPlugins := true,
  javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked", "-language:implicitConversions"),
  resolvers ++= Seq(
    Classpaths.typesafeReleases,
    Resolver.sonatypeRepo("snapshots"),
    "Sonatype HTTPS RELEASES" at "https://oss.sonatype.org/content/repositories/releases/",
    "Typesafe Maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
  ),
  libraryDependencies ++= Seq(
    "com.tinkerpop.blueprints" % "blueprints-core" % "2.3.0",
    "com.orientechnologies" % "orientdb-core" % "2.1.0",
    "com.orientechnologies" % "orientdb-graphdb" % "2.1.0",
    "org.scala-lang" % "scalap" % "2.11.6", //TODO fix scala dep on scalaVersion
    "org.mockito" % "mockito-core" % "1.9.5" % "test",
    "org.scalatest" %% "scalatest" % "2.2.3" % "test",
    "org.specs2" %% "specs2" % "2.3.12" % "test"
  )
)

lazy val root = (project in file(".")).settings(commonSettings: _*)

