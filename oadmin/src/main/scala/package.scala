package schola
package oadmin

object `package` {
  import com.typesafe.config._
  import akka.actor.{ ActorSystem, Props }

  val config = {
    val root = ConfigFactory.load()
    root getConfig "oadmin"
  }

  val system = ActorSystem("OAdminActorSystem")

  val avatars = system.actorOf(Props(new utils.Avatars(new MongoDBSettings(config getConfig "mongodb"))), name = "avatars")

  val passwords = webcrank.password.Passwords.scrypt(n = 4096) // TODO: register bouncycastle provider and use {digest = SHA512} . . .

  val Hostname = config.getString("hostname")

  val Port = config.getInt("port")

  val MaxResults = 50

  val API_VERSION = config.getString("api-version")

  val MaxUploadSize = config.getInt("max-upload-size")

  val PasswordMinLength = config.getInt("password-min-length")

  val SESSION_KEY = "_session_key"

  val AccessTokenSessionLifeTime = config.getInt("oauth2.access-token-session-lifetime")

  val RefreshTokenSessionLifeTime = config.getInt("oauth2.refresh-token-session-lifetime")

  val Q = scala.slick.driver.PostgresDriver.simple

  val SuperUser = domain.U.SuperUser

  val SuperUserR = domain.R.SuperUserR
  val AdministratorR = domain.R.AdministratorR

  val MACAlgorithm = config.getString("oauth2.mac-algorithm")

  class MongoDBSettings(config: Config) {
    val Host = config.getString("host")
    val Database = config.getString("db")
  }

  val S = Façade.simple
}