
resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/releases/"

// Comment to get more information during initialization
//logLevel := Level.Warn

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3.0-RC1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.0-RC3")