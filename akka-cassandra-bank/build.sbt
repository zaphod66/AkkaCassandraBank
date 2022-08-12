import Dependencies._

ThisBuild / scalaVersion     := "2.13.8"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.zaphod"
ThisBuild / organizationName := "zaphod"

lazy val root = (project in file("."))
  .settings(
    name := "Akka-Cassandra-Bank",

    libraryDependencies ++= Seq(
      akkaHttp,
      akkaTyped,
      akkaStream,
      akkaPersistence,
      dataDriver,
      akkaCassandra,
      circeCore,
      circeGeneric,
      circeParser,
      akkaHttpCirce,
      logbackClassic,

      scalaTest,
      akkaHttpTestkit,
      akkaTypedTestkit
    )
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
