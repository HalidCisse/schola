import sbt._
import Keys._

import play.Play.autoImport._, PlayKeys._
// import play.Project._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._

object ScholaBuild extends Build {
  
  lazy val root = 
    project.in(
      file(".")
    )
     .settings(Common.settings: _*)
     .aggregate(
        domain, services, `services-jdbc`, cache, 
        oauth2, http, util, cli
      )

  // Common

  lazy val cache = project.settings(Common.settings: _*).dependsOn(util)

  lazy val util = project.settings(Common.settings: _*)

  lazy val oauth2 = project.settings(Common.settings: _*).dependsOn(domain, `services-jdbc`, util)

  // Base

  lazy val domain = project.settings(Common.settings: _*).dependsOn(util)

  lazy val services = project.settings(Common.settings: _*).dependsOn(domain)

  lazy val uploads = project.settings(Common.settings: _*).dependsOn(services)

  lazy val `jdbc-base` = project.settings(Common.settings: _*).dependsOn(domain, `domain-schools`)
  
  lazy val `services-jdbc` = project.settings(Common.settings: _*).dependsOn(`jdbc-base`, services, util, cache, uploads)

  // Schools

  lazy val `domain-schools` = project.settings(Common.settings: _*).dependsOn(domain, util)

  lazy val `services-schools` = project.settings(Common.settings: _*).dependsOn(services, `domain-schools`, domain)

  lazy val `services-schools-jdbc` = project.settings(Common.settings: _*).dependsOn(`jdbc-base`, `services-schools`, util, cache, uploads)

  // Admin module

  // API access projects

  val http = 
    project.enablePlugins(play.PlayScala)
           .settings(Common.settings: _*)
           // .settings(playScalaSettings: _*)
           .settings(sourceDirectory := baseDirectory.value / "app")
           .aggregate(domain, `services-jdbc`, util, common, admin, schools)
           .dependsOn(domain, `services-jdbc`, util, common, admin, schools)

  lazy val common = project.in(
      file("http/modules/common")
    ).settings(Common.settings: _*)
     .dependsOn(domain, `services-jdbc`, `services-schools-jdbc`, util)

  lazy val admin = project.in(
      file("http/modules/admin")
    ).enablePlugins(play.PlayScala)
     .settings(Common.settings: _*)
     // .settings(playScalaSettings: _*)
     .dependsOn(common)

  lazy val schools = project.in(
      file("http/modules/schools")
    ).enablePlugins(play.PlayScala)
     .settings(Common.settings: _*)
     // .settings(playScalaSettings: _*)
     .dependsOn(common)     

  // Web-client access modules

  val cli = 
    project.aggregate(domain, util, `cli-admin`, `cli-schools`)
           .dependsOn(domain, util, `cli-admin`, `cli-schools`)
           .enablePlugins(play.PlayScala)
           .settings(Common.settings: _*)
           // .settings(playScalaSettings: _*)
           .settings(playDefaultPort := 9999) 
           .settings(sourceDirectory := baseDirectory.value / "app")
           .settings(
              libraryDependencies := libraryDependencies.value map(_ excludeAll ExclusionRule(name = "avsl")))

  lazy val `cli-admin` = project.in(
      file("cli/modules/admin")
    ).enablePlugins(play.PlayScala)
    .settings(Common.settings: _*)
    // .settings(playScalaSettings: _*)

  lazy val `cli-schools` = project.in(
      file("cli/modules/schools")
    ).enablePlugins(play.PlayScala)
    .settings(Common.settings: _*)
    // .settings(playScalaSettings: _*)
}
