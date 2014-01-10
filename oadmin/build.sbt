
organization := "schola.oadmin"

name := "oadmin"

version := "0.5.0-Beta"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-encoding", "utf8")

javaOptions ++= Seq("-Dsun.net.inetaddr.ttl=30")

ideaExcludeFolders += ".idea"

ideaExcludeFolders += ".idea_modules"

proguardSettings

ProguardKeys.options in Proguard ++= Seq("-dontnote", "-dontwarn", "-ignorewarnings")

ProguardKeys.options in Proguard += ProguardOptions.keepMain("Test")

ProguardKeys.merge in Proguard := true

ProguardKeys.mergeStrategies in Proguard += ProguardMerge.discard("META-INF/.*".r)

ProguardKeys.mergeStrategies in Proguard += ProguardMerge.append("reference.conf")

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "2.0.0-RC1",
  "com.typesafe" % "config" % "1.0.2"
)

libraryDependencies ++= Seq(
  // "com.h2database" % "h2" % "1.3.166",
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc41",
  "c3p0" % "c3p0" % "0.9.1.2"
)
 
libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-oauth2" % "0.7.1",
  //"net.databinder" %% "unfiltered-directives" % "0.7.1",
  "net.databinder" %% "unfiltered-spec" % "0.7.1" % "test",
  "net.databinder" %% "unfiltered-filter-async" % "0.7.1",
  "net.databinder" %% "unfiltered-filter-uploads" % "0.7.1",
  "net.databinder" %% "unfiltered-json4s" % "0.7.1",
  "net.databinder" %% "unfiltered-agents" % "0.7.1"
)

libraryDependencies ++= Seq(
  "io.webcrank" %% "webcrank-password" % "0.3",
  "org.bouncycastle" % "bcprov-jdk14" % "1.49"
)

// The Typesafe repository
resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "reactivemongo" % "0.11.0-SNAPSHOT" exclude("com.typesafe.play", "play-iteratees"),
  "com.typesafe.play" %% "play-iteratees" % "2.2.1"
)

libraryDependencies ++= Seq(
  // "org.mongodb" % "mongo-java-driver" % "1.3"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3-M1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.3-M1"
)

libraryDependencies ++= Seq(
  "org.fusesource.scalate" %% "scalate-core" % "1.6.1"
)

libraryDependencies ++= Seq(
 // "org.slf4j" % "slf4j-simple" % "1.7.5",
  "org.clapper" %% "avsl" % "1.0.1"
)

libraryDependencies ++= Seq(
  "commons-validator" % "commons-validator" % "1.4.0",
  "org.apache.commons" % "commons-lang3" % "3.1"
)

libraryDependencies ++= Seq(
  "net.jpountz.lz4" % "lz4" % "1.2.0",
  "net.spy" % "spymemcached" % "2.10.3"
)

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.0",
  "net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.0"
)

libraryDependencies ++= Seq(
 // "org.mockito" % "mockito-core" % "1.9.5" % "test"
 // "com.typesafe.play" %% "templates" % "2.2.1"
)

libraryDependencies += "org.apache.commons" % "commons-email" % "1.3.2"

// libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file"  % "0.4.2"

libraryDependencies +=  "com.sun.mail" % "javax.mail" % "1.5.1"

// libraryDependencies += "net.databinder" %% "unfiltered-directives" % "0.7.1"

libraryDependencies := libraryDependencies.value map(_ excludeAll(ExclusionRule(organization = "javax.mail")))