ThisBuild / scalaVersion := "2.13.8"

import com.typesafe.sbt.packager.docker._


val commonScalacSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-unchecked",
    "-encoding", "UTF-8",
    "-Xlint",
  )
)

val akka = "2.6.12"
val akkaHttpVersion = "10.2.9"

lazy val commonDependencies = Seq (
  // -- Logging --
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // -- Akka --
  "com.typesafe.akka" %% "akka-actor-typed"   % akka,
  "com.typesafe.akka" %% "akka-cluster-typed" % akka,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  
  // kamon for jaeger (and potentially prometheus)
  "io.kamon" %% "kamon-bundle" % "2.5.4",
  "io.kamon" %% "kamon-akka" % "2.5.4",
  "io.kamon" %% "kamon-jaeger" % "2.5.4"
)

lazy val gateway = project
  .in(file("module-gateway"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    name := "gateway",
    libraryDependencies ++= commonDependencies,

    version in Docker := "latest",
    dockerExposedPorts in Docker := Seq(1600),
    dockerRepository := Some("suu_project_repository"),
    dockerBaseImage := "java",

    // run as root 
    daemonUserUid in Docker := Option("0"),
    daemonUser in Docker    := "daemon",
  )

lazy val messageregistry = project
  .in(file("module-messageregistry"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    name := "messageregistry",
    libraryDependencies ++= commonDependencies,

    version in Docker := "latest",
    dockerExposedPorts in Docker := Seq(1601),
    dockerRepository := Some("suu_project_repository"),
    dockerBaseImage := "java",

    // run as root 
    daemonUserUid in Docker := Option("0"),
    daemonUser in Docker    := "daemon",
  )

lazy val messageroom = project
  .in(file("module-messageroom"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    name := "messageroom",
    libraryDependencies ++= commonDependencies,

    version in Docker := "latest",
    dockerExposedPorts in Docker := Seq(1602),
    dockerRepository := Some("suu_project_repository"),
    dockerBaseImage := "java",

    // run as root 
    daemonUserUid in Docker := Option("0"),
    daemonUser in Docker    := "daemon",
  )

lazy val userservice = project
  .in(file("module-userservice"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    name := "userservice",
    libraryDependencies ++= commonDependencies,

    version in Docker := "latest",
    dockerExposedPorts in Docker := Seq(1603),
    dockerRepository := Some("suu_project_repository"),
    dockerBaseImage := "java",

    // run as root 
    daemonUserUid in Docker := Option("0"),
    daemonUser in Docker    := "daemon",
  )

val http4sVersion = "0.23.13"

val dockerOptions = Seq(
  version in Docker := "latest",
  dockerExposedPorts in Docker := Seq(1603),
  dockerRepository := Some("suu_nonreactive"),
  dockerBaseImage := "openjdk:jre",

  // run as root 
  daemonUserUid in Docker := Option("0"),
  daemonUser in Docker    := "daemon",
)

val nonReactiveDeps = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",

  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-ember-client" % http4sVersion,

  "io.kamon" %% "kamon-core" % "2.2.3",
  "io.kamon" %% "kamon-http4s-0.23" % "2.2.1",
  "io.kamon" %% "kamon-prometheus" % "2.2.3",
  "io.kamon" %% "kamon-zipkin" % "2.2.3",
  "io.kamon" %% "kamon-jaeger" % "2.2.3"
)

lazy val gatewayNR = project
  .in(file("nonreactive/gateway"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    libraryDependencies ++= nonReactiveDeps,
    name := "gatewaynr",
    dockerOptions
  )

lazy val messageregistryNR = project
  .in(file("nonreactive/messageregistry"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    libraryDependencies ++= nonReactiveDeps,
    name := "messageregistrynr",
    dockerOptions
  )

lazy val messageroomNR = project
  .in(file("nonreactive/messageroom"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    libraryDependencies ++= nonReactiveDeps,
    name := "messageroomnr",
    dockerOptions
  )

lazy val userserviceNR = project
  .in(file("nonreactive/userservice"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    commonScalacSettings,
    libraryDependencies ++= nonReactiveDeps,
    name := "userservicenr",
    dockerOptions
  )
