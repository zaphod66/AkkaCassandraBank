import sbt._

object Dependencies {
  lazy val akkaHttpVersion  = "10.2.8"
  lazy val akkaVersion      = "2.6.9"
  lazy val circeVersion     = "0.14.1"
  lazy val scalaTestVersion = "3.2.11"

  lazy val akkaHttp         = "com.typesafe.akka" %% "akka-http"                  % akkaHttpVersion
  lazy val akkaTyped        = "com.typesafe.akka" %% "akka-actor-typed"           % akkaVersion
  lazy val akkaStream       = "com.typesafe.akka" %% "akka-stream"                % akkaVersion
  lazy val akkaPersistence  = "com.typesafe.akka" %% "akka-persistence-typed"     % akkaVersion
  lazy val dataDriver       = "com.datastax.oss"  %  "java-driver-core"           % "4.13.0"
  lazy val akkaCassandra    = "com.typesafe.akka" %% "akka-persistence-cassandra" % "1.0.5"
  lazy val circeCore        = "io.circe"          %% "circe-core"                 % circeVersion
  lazy val circeGeneric     = "io.circe"          %% "circe-generic"              % circeVersion
  lazy val circeParser      = "io.circe"          %% "circe-parser"               % circeVersion
  lazy val akkaHttpCirce    = "de.heikoseeberger" %% "akka-http-circe"            % "1.39.2"
  lazy val logbackClassic   = "ch.qos.logback"    % "logback-classic"             % "1.2.10"

  lazy val scalaTest        = "org.scalatest"     %% "scalatest"                  % scalaTestVersion
  lazy val akkaHttpTestkit  = "com.typesafe.akka" %% "akka-http-testkit"          % akkaHttpVersion % Test
  lazy val akkaTypedTestkit = "com.typesafe.akka" %% "akka-actor-testkit-typed"   % akkaVersion     % Test
}
