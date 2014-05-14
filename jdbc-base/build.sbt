
description := "JDBC base"

resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"

// resolvers += "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"

libraryDependencies ++= Common.jdbcDeps

libraryDependencies ++= Common.slickDeps