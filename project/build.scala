import sbt._
import Keys._

object ApplicationBuild extends Build {

  val appDependencies = Seq()

  private def module(moduleName: String)(
    projectId: String = "schola-" + moduleName,
    dirName: String = moduleName,
    srcPath: String = "schola/" + moduleName.replace("-","/"),
    settings: Seq[Setting[_]] = Seq.empty
  ) = play.Project(projectId, Common.appVersion, Seq(), path = file(dirName),
        settings = Defaults.defaultSettings ++ Common.settings ++ settings)

  lazy val root =
    Project("root",
            file("."),
            settings = Defaults.defaultSettings ++ Common.settings
    ).aggregate(
            domain, services, servicesJdbc, cache, 
            oauth2Server, httpPlay2, util, cli)

  // Common

  lazy val cache = module("cache")().dependsOn(util)

  lazy val util = module("util")()

 lazy val oauth2Server = 
    module("oauth2-server")(
      ).dependsOn(domain, servicesJdbc, util)  

  // Base

  lazy val domain =
    module("domain")(
   ).dependsOn(util)

  lazy val services =
    module("services")().dependsOn(domain)

  lazy val servicesJdbc =
    module("services-jdbc")(
    ).dependsOn(services, util, cache)

  // Admin module

  // API access projects

  val httpPlay2 =
    module("http-play2")(
      ).dependsOn(domain, servicesJdbc, util, common, admin)

  lazy val common =
    project.in(
      file("http-play2/modules/common")
    ).settings(Defaults.defaultSettings ++ Common.settings: _*)
     .dependsOn(domain, servicesJdbc, util)

  lazy val admin =
    project.in(
      file("http-play2/modules/admin")
    ).settings(Defaults.defaultSettings ++ Common.settings: _*)
     .dependsOn(common)

  // Web-client access modules

  val cli = 
    module("cli")(
      ).dependsOn(domain, util, `cli-admin`)
       .settings(
          libraryDependencies := libraryDependencies.value map(_ excludeAll ExclusionRule(name = "avsl")))

  lazy val `cli-admin` =
    project.in(
      file("cli/modules/admin")
    ).settings(Defaults.defaultSettings ++ Common.settings: _*)
}