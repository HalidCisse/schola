import sbt._
import Keys._

// import play.Play.autoImport._, PlayKeys._
import play.Project._

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
        `oauth2-server`, `http-play2`, util, cli
      )

  // Common

  lazy val cache = project.settings(Common.settings: _*).dependsOn(util)

  lazy val util = project.settings(Common.settings: _*)

  lazy val `oauth2-server` = project.settings(Common.settings: _*).dependsOn(domain, `services-jdbc`, util)

  // Base

  lazy val domain = project.settings(Common.settings: _*).dependsOn(util)

  lazy val services = project.settings(Common.settings: _*).dependsOn(domain)

  lazy val `services-jdbc` = project.settings(Common.settings: _*).dependsOn(services, util, cache)

  // Admin module

  // API access projects

  val `http-play2` = 
    project//.enablePlugins(play.PlayScala)
           .settings(Common.settings: _*)
           .settings(playScalaSettings: _*)
           .aggregate(domain, `services-jdbc`, util, common, admin, school)
           .dependsOn(domain, `services-jdbc`, util, common, admin, school)

  lazy val common = project.in(
      file("http-play2/modules/common")
    ).settings(Common.settings: _*)
     .dependsOn(domain, `services-jdbc`, util)

  lazy val admin = project.in(
      file("http-play2/modules/admin")
    )//.enablePlugins(play.PlayScala)
     .settings(Common.settings: _*)
     .settings(playScalaSettings: _*)
     .dependsOn(common)

  lazy val school = project.in(
      file("http-play2/modules/school")
    )//.enablePlugins(play.PlayScala)
     .settings(Common.settings: _*)
     .settings(playScalaSettings: _*)
     .dependsOn(common)     

  // Web-client access modules

  val cli = 
    project.aggregate(domain, util, `cli-admin`, `cli-school`)
           .dependsOn(domain, util, `cli-admin`, `cli-school`)
           // .enablePlugins(play.PlayScala)
           .settings(Common.settings: _*)
           .settings(playScalaSettings: _*)
           .settings(playDefaultPort := 9999)      
           .settings(
              libraryDependencies := libraryDependencies.value map(_ excludeAll ExclusionRule(name = "avsl")))

  lazy val `cli-admin` = project.in(
      file("cli/modules/admin")
    )
    // .enablePlugins(play.PlayScala)
    .settings(Common.settings: _*)
    .settings(playScalaSettings: _*)

  lazy val `cli-school` = project.in(
      file("cli/modules/school")
    )
    // .enablePlugins(play.PlayScala)
    .settings(Common.settings: _*)
    .settings(playScalaSettings: _*)
}