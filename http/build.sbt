
import com.typesafe.sbt.packager.Keys._

com.typesafe.sbt.SbtNativePackager.packageArchetype.java_server

name := "schola-http"

daemonUser in Linux := "schola"

daemonGroup in Linux := (daemonUser in Linux).value

libraryDependencies ++= Seq(
  // Select Play modules
  //jdbc,      // The JDBC connection pool and the play.api.db API
  //anorm,     // Scala RDBMS Library
  //javaJdbc,  // Java database API
  //javaEbean, // Java Ebean plugin
  //javaJpa,   // Java JPA plugin
  //filters,   // A set of built-in filters
  //javaCore,  // The core Java API  
  // WebJars pull in client-side web libraries
  //"org.webjars" %% "webjars-play" % "2.2.0",
  //"org.webjars" % "bootstrap" % "2.3.1"
  // Add your own project dependencies in the form:
  // "group" % "artifact" % "version"
  // "com.typesafe.akka" %% "akka-actor" % "2.2.1",
  // "com.typesafe.akka" %% "akka-slf4j" % "2.2.1",
  // "com.typesafe.play" %% "filters-helpers" % Common.playVersion,
  "com.typesafe" %% "play-plugins-util" % Common.playVersion,
  "commons-io" % "commons-io" % "2.4"
)

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

description := "OAuth implementation based on Unfiltered OAuth2 module"

packageSummary in Linux := description.value

packageDescription in Linux := description.value

libraryDependencies ~= { _ map {
  case m if m.organization == "com.typesafe.play" =>
    m.exclude("commons-logging", "commons-logging")
     .exclude("com.typesafe.play", "sbt-link")
     .exclude("jline", "jline")
  case m => m
}}