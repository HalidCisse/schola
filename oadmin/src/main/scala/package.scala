package schola

package object oadmin {
  import com.typesafe.config._

  lazy val system = akka.actor.ActorSystem("ScholaActorSystem")

  lazy val avatars = system.actorOf(
    akka.actor.Props(classOf[utils.Avatars], new MongoDBSettings(config getConfig "mongodb")), name = "avatars")

  val passwords = webcrank.password.Passwords.pbkdf2() // TODO: register bouncycastle provider and use {digest = SHA512} . . .

  val config = {
    val g = ConfigFactory.load()
    g.getConfig("oadmin")
  }

  val SESSION_KEY = "_session_key"

  val AccessTokenSessionLifeTime = config.getInt("oauth2.access-token-session-lifetime")

  val RefreshTokenSessionLifeTime = config.getInt("oauth2.refresh-token-session-lifetime")

  val Q = scala.slick.driver.PostgresDriver.simple

  val SuperUser = domain.U.SuperUser

  val SuperUserR = domain.R.SuperUserR
  val AdministratorR = domain.R.AdministratorR

  val MACAlgorithm = config.getString("oauth2.mac-algorithm")

  class MongoDBSettings(config: Config) {
    val Host = config.getString("hostname")
    val Port = config.getInt("port")
    val DatabaseName = config.getString("dbname")
    val CollectionName = config.getString("dbcollection")
  }

  val DefaultAvatars = new {
    import com.owtelse.codec.Base64
    import java.nio.file.{Files, Paths}

    val M = Base64.encode(Files.readAllBytes(Paths.get(getClass.getResource(config.getString("avatars.male")).toURI)))

    val F = Base64.encode(Files.readAllBytes(Paths.get(getClass.getResource(config.getString("avatars.female")).toURI)))
  }

  val S = Façade.simple
}