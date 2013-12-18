package schola

package object oadmin {
  lazy val system = akka.actor.ActorSystem("ScholaActorSystem")

  lazy val avatars = system.actorOf(akka.actor.Props(new utils.Avatars), name = "avatars")

  val passwords = webcrank.password.Passwords.pbkdf2() // TODO: register bouncycastle provider and use {digest = SHA512} . . .

  val config = com.typesafe.config.ConfigFactory.load()

  val AccessTokenSessionLifeTime = config.getInt("access-token-session-lifetime")

  val RefreshTokenSessionLifeTime = config.getInt("refresh-token-session-lifetime")

  val Q = scala.slick.driver.PostgresDriver.simple

  val SuperUser = domain.U.SuperUser

  val SuperUserR = domain.R.SuperUserR
  val AdministratorR = domain.R.AdministratorR

  val MACAlgorithm = config.getString("oauth2-mac-algorithm")

  val Db = new {
    val DriverClass = config.getString("db.database-driver-class")

    val DatabaseURL = config.getString("db.database-url")

    val Username = config.getString("db.database-username")

    val Password = config.getString("db.database-password")

    //------------------------------------------

    val MaxPoolSize = config.getInt("db.database-max-pool-size")

    val MinPoolSize = config.getInt("db.database-min-pool-size")
  }

  val MongoDB = new {
    val Hostname = config.getString("mongodb.hostname")
    val Port = config.getInt("mongodb.dbport")
    val DatabaseName = config.getString("mongodb.dbname")
    val CollectionName = config.getString("mongodb.dbcollection")
  }

  val DefaultAvatars = new {
    import com.owtelse.codec.Base64
    import java.nio.file.{Files, Paths}

    val Male = Base64.encode(Files.readAllBytes(Paths.get(getClass.getResource(config.getString("default_avatar.male")).toURI)))

    val Female = Base64.encode(Files.readAllBytes(Paths.get(getClass.getResource(config.getString("default_avatar.male")).toURI)))
  }

  //  java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider)
}