// The Typesafe repository
//resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

//resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

organization := "schola.oadmin"

name := "oadmin"

version := "0.0.1"

scalaVersion := "2.10.3"

scalacOptions += "-deprecation"

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

libraryDependencies ++= List(
  "com.typesafe.slick" %% "slick" % "2.0.0-M3",
  "com.typesafe" % "config" % "1.0.2",
  "net.databinder" %% "unfiltered-json4s" % "0.7.1",
//  "org.json4s" %% "json4s-ext" % "3.2.6",
 // "joda-time" % "joda-time" % "2.3",
//  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "org.clapper" %% "avsl" % "1.0.1",
  "c3p0" % "c3p0" % "0.9.1.2",
 // "com.h2database" % "h2" % "1.3.166",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "net.databinder" %% "unfiltered-oauth2" % "0.7.1",
  "net.databinder" %% "unfiltered-spec" % "0.7.1",
  "net.databinder" %% "unfiltered-filter-async" % "0.7.1",
  "io.webcrank" %% "webcrank-password" % "0.3",
  "org.bouncycastle" % "bcprov-jdk14" % "1.49"
  //"com.typesafe" %% "scalalogging-slf4j" % "1.0.1"
)