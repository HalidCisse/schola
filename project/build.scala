import sbt._
import Keys._

object ApplicationBuild extends Build {
  import Common._
  import java.lang.{ Boolean => JBoolean }

  val appName         = id("cl")
  val appVersion      = "0.5.1"

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
            oauth2, http, util, cl, bootstrap)

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

  lazy val oauth2 = 
    module("oauth2")().dependsOn(domain, services, util)

  lazy val http = 
    module("http")().dependsOn(oauth2, domain, services, util)

  val bootstrap =
    module("bootstrap")(
    ).dependsOn(
        domain, servicesJdbc, cache, oauth2, http, util)

  val cl = 
    module("cl")(
      ).dependsOn(domain, util)
       .settings(
          libraryDependencies := libraryDependencies.value map(_ excludeAll(ExclusionRule(name = "avsl"))))
}