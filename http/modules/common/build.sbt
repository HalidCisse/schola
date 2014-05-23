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
  "com.typesafe" %% "play-plugins-util" % Common.playVersion,
  "com.typesafe" %% "play-plugins-mailer" % Common.playVersion,
  "com.typesafe.play" %% "play" % Common.playVersion
)

// libraryDependencies += Common.unfilteredMac
libraryDependencies += Common.unfilteredCore

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"