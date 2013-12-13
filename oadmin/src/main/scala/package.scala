package schola

package object oadmin {
  lazy val system = akka.actor.ActorSystem("ScholaActorSystem")

  lazy val avatars = system.actorOf(akka.actor.Props(new utils.Avatars))

  lazy val passwords = webcrank.password.Passwords.pbkdf2() // TODO: register bouncycastle provider and use {digest = SHA512} . . .

  lazy val config = com.typesafe.config.ConfigFactory.load()

  lazy val AccessTokenSessionLifeTime = config.getInt("access-token-session-lifetime")

  lazy val RefreshTokenSessionLifeTime = config.getInt("refresh-token-session-lifetime")

  lazy val Q = scala.slick.driver.PostgresDriver.simple

  val SuperUser = domain.U.SuperUser

  val SuperUserR = domain.R.SuperUserR
  val AdministratorR = domain.R.AdministratorR

  lazy val MacAlgo = config.getString("oauth2-mac-algo")

  lazy val Db = new {
    val DriverClass = config.getString("db.database-driver-class")

    val DatabaseURL = config.getString("db.database-url")

    val Username = config.getString("db.database-username")

    val Password = config.getString("db.database-password")

    //------------------------------------------

    val MaxPoolSize = config.getInt("db.database-max-pool-size")

    val MinPoolSize = config.getInt("db.database-min-pool-size")
  }

  //  java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider)
}