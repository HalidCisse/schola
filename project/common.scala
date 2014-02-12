import sbt._
import org.sbtidea.SbtIdeaPlugin._

object Common {
  import Keys._

  def specsDep(sv: String) =
    sv.split("[.-]").toList match {
      case "2" :: "9" :: "0" :: _ :: _ => "org.scala-tools.testing" %% "specs" % "1.6.8"
      case "2" :: "9" :: _ => "org.scala-tools.testing" % "specs_2.9.1" % "1.6.9"
      case _ => "org.scala-tools.testing" %% "specs" % "1.6.9"
    }

  def specs2Dep(sv: String) =
    sv.split("[.-]").toList match {
      case "2" :: "9" :: "1" :: "1" :: _ =>
        "org.specs2" %% "specs2" % "1.12.4"
      case "2" :: "9" :: _ => "org.specs2" %% "specs2" % "1.12.4.1"
      case "2" :: "10" :: _ => "org.specs2" %% "specs2" % "1.13"
      case _ => sys.error("Unsupported scala version")
    }

  private def withExclusions(items: Seq[ModuleID]) = 
    items.map(_.excludeAll(ExclusionRule(organization = "javax.mail"), ExclusionRule(organization = "jline", name = "jline")))

  // val json4s = "org.json4s" %% "json4s-native" % "3.2.6"

  val dispatchVersion = "0.11.0"  
  def dispatchDeps =
    "net.databinder.dispatch" %% "dispatch-core" % dispatchVersion :: Nil
    // "net.databinder.dispatch" %% "dispatch-json4s-native" % dispatchVersion ::

  val unfilteredVersion = "0.7.1"
  val oauth2Dep = "net.databinder" %% "unfiltered-oauth2" % unfilteredVersion
  val unfilteredMac = "net.databinder" %% "unfiltered-mac" % unfilteredVersion
  val unfilteredSpec = "net.databinder" %% "unfiltered-spec" % unfilteredVersion % "test"
  val unfilteredSpec2 = "net.databinder" %% "unfiltered-spec2" % unfilteredVersion % "test"

  val akkaVersion = "2.2.1"
  def akkaDeps =
    "com.typesafe.akka" %% "akka-actor" % akkaVersion ::
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion :: Nil

  def jdbcDeps = 
    "org.postgresql" % "postgresql" % "9.3-1100-jdbc41" ::
    "com.mchange" % "c3p0" % "0.9.5-pre6" :: Nil  

  val reactiveMango = "org.reactivemongo" %% "reactivemongo" % "0.11.0-SNAPSHOT"

  val slick = "com.typesafe.slick" %% "slick" % "2.0.0"

  val utilDeps = withExclusions {   
    // "commons-validator" % "commons-validator" % "1.4.0" ::
    "org.apache.commons" % "commons-lang3" % "3.1" ::    
    "net.jpountz.lz4" % "lz4" % "1.2.0" :: 
    "com.typesafe" % "config" % "1.0.2" ::    
    "org.bouncycastle" % "bcprov-jdk15on" % "1.50" ::
    // "org.clapper" %% "avsl" % "1.0.1" ::  
    "ch.qos.logback" % "logback-classic" % "1.1.1" ::
    "org.slf4j" % "slf4j-api" % "1.7.5" ::  
    "org.apache.commons" % "commons-email" % "1.3.2" ::
    "io.webcrank" %% "webcrank-password" % "0.3" ::
    "com.sun.mail" % "javax.mail" % "1.5.1" :: Nil    
  }

  val memcachedDeps = 
    "net.spy" % "spymemcached" % "2.10.3" :: Nil

  def integrationTestDeps(sv: String) = (specsDep(sv) :: dispatchDeps) map { _ % "test" }

  val settings = Seq(

    organization := "schola.oadmin",

    version := "0.5.1",

    scalaVersion := "2.10.3",

    shellPrompt := buildShellPrompt,

    // crossScalaVersions := Seq("2.9.3", "2.10.3"),

    scalacOptions <++= scalaVersion.map(sv =>
      Seq("-Xcheckinit", "-encoding", "utf8", "-deprecation", "-unchecked", "-language:_")),

    parallelExecution in Test := false, // :( test servers collide on same port

    // homepage := Some(new java.net.URL("http://unfiltered.databinder.net/")),

    publishMavenStyle := true,

    // publishTo := Some("releases" at
              // "https://oss.sonatype.org/service/local/staging/deploy/maven2"),

  ideaExcludeFolders := Seq(".idea", ".idea_modules"),

    publishArtifact in Test := false/*,

    licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/MIT"))*/,

    pomExtra := (
      <scm>
        <url>git@github.com:amsayk/schola.git</url>
        <connection>scm:git:git@github.com:amsayk/schola.git</connection>
      </scm>
      <developers>
        <developer>
          <id>amsayk</id>
          <name>Amadou Cisse</name>
          <url>http://twitter.com/amsayk</url>
        </developer>
      </developers>
    )
  ) ++ Format.settings

  val buildShellPrompt = 
    (state: State) => "[%s] ".format(Project.extract(state).currentProject.id)
}

object Format {
  import com.typesafe.sbt.SbtScalariform._

  lazy val settings = scalariformSettings ++ Seq(
    ScalariformKeys.preferences := formattingPreferences)

  lazy val formattingPreferences = {
    import scalariform.formatter.preferences._
    FormattingPreferences().
      setPreference(AlignParameters, true).
      setPreference(AlignSingleLineCaseStatements, true).
      setPreference(CompactControlReadability, false).
      setPreference(CompactStringConcatenation, false).
      setPreference(DoubleIndentClassDeclaration, true).
      setPreference(FormatXml, true).
      setPreference(IndentLocalDefs, false).
      setPreference(IndentPackageBlocks, true).
      setPreference(IndentSpaces, 2).
      setPreference(MultilineScaladocCommentsStartOnFirstLine, false).
      setPreference(PreserveSpaceBeforeArguments, false).
      setPreference(PreserveDanglingCloseParenthesis, false).
      setPreference(RewriteArrowSymbols, false).
      setPreference(SpaceBeforeColon, false).
      setPreference(SpaceInsideBrackets, false).
      setPreference(SpacesWithinPatternBinders, true)
  }
}