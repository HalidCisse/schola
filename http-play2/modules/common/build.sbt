import com.typesafe.sbt.packager.Keys._

description := "Common lib for all modules"

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
  "com.typesafe" %% "play-plugins-util" % "2.2.0",
  "com.typesafe.play" %% "play" % "2.2.2"
)

libraryDependencies += Common.unfilteredMac

libraryDependencies += "com.typesafe" %% "play-plugins-mailer" % "2.2.0"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

daemonUser in Linux := "root"

daemonGroup in Linux := "root"