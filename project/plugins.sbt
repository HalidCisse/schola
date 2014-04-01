
resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/snapshots/"
)

// Comment to get more information during initialization
//logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")
