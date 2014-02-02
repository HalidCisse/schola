
description := "Services implementation based on slick"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= 
  Common.jdbcDeps

libraryDependencies += Common.slick

libraryDependencies += Common.reactiveMango 