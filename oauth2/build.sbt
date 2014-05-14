
import com.typesafe.sbt.packager.Keys._

com.typesafe.sbt.SbtNativePackager.packageArchetype.java_server

name := "schola-oauth2"

daemonUser in Linux := "schola"

daemonGroup in Linux := (daemonUser in Linux).value

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies +=
  Common.oauth2Dep

description := "API based on Play! framework"

packageSummary in Linux := description.value

packageDescription in Linux := description.value