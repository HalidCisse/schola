import sbt._
import Keys._
// import com.typesafe.sbt.SbtAtmosPlay.atmosPlaySettings

object ApplicationBuild extends Build {
  import Common._
  import java.lang.{ Boolean => JBoolean }

  val appVersion      = "0.6.0"

  val appDependencies = Seq()  

  def id(name: String) = "schola-%s" format name

  def local(name: String) = LocalProject(id(name))

  def srcPathSetting(projectId: String, rootPkg: String) =
    mappings in (LocalProject(projectId), Compile, packageSrc) ~= {
      defaults: Seq[(File,String)] =>
        defaults.map { case(file, path) =>
          (file, rootPkg + "/" + path)
        }
    }

  private def ciSettings: Seq[Def.Setting[_]] = {
    if (JBoolean.parseBoolean(
      sys.env.getOrElse("TRAVIS", "false"))) Seq(
      logLevel in Global := Level.Warn,
      logLevel in Compile := Level.Warn,
      logLevel in Test := Level.Info
    ) else Seq.empty[Def.Setting[_]]
  }

  private def module(moduleName: String)(
    projectId: String = "schola-" + moduleName,
    dirName: String = moduleName,
    srcPath: String = "schola/" + moduleName.replace("-","/"),
    settings: Seq[Setting[_]] = Seq.empty
  ) = play.Project(projectId, appVersion, Seq(), path = file(dirName),
        settings = (
            Defaults.defaultSettings ++
              Common.settings ++
              ciSettings ++
              srcPathSetting(projectId, srcPath) ++ settings            
            ))

  lazy val root =
    Project("root",
            file("."),
            settings = Defaults.defaultSettings ++ Common.settings
    ).aggregate(
            domain, services, servicesJdbc, cache, 
            oauth2Server/*, http, httpPlay2*/, util, cli)

  lazy val domain: Project =
    module("domain")(
   ).dependsOn(util)

  lazy val services =
    module("services")().dependsOn(domain)

  lazy val servicesJdbc =
    module("services-jdbc")(
    ).dependsOn(services, util, cache)

  lazy val cache = module("cache")().dependsOn(util)

  lazy val util = module("util")()

  lazy val oauth2Server = 
    module("oauth2-server")(
      ).dependsOn(domain, servicesJdbc, util)
       // .settings(atmosPlaySettings: _*)

  lazy val httpPlay2 = 
    module("http-play2")(
      ).dependsOn(domain, servicesJdbc, util)
       // .settings(atmosPlaySettings: _*)   

  val cli = 
    module("cli")(
      ).dependsOn(domain, util)
       // .settings(atmosPlaySettings: _*)
       .settings(
          libraryDependencies := libraryDependencies.value map(_ excludeAll(ExclusionRule(name = "avsl"))))
}