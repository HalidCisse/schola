
import com.typesafe.sbt.packager.Keys._

com.typesafe.sbt.SbtNativePackager.packageArchetype.java_server

name := "schola-oauth2"

daemonUser in Linux := "root"

daemonGroup in Linux := "root"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies +=
  Common.oauth2Dep

description := "API based on Play! framework"

packageSummary in Debian := description.value

packageDescription in Debian := description.value