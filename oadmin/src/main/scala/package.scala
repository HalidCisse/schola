package schola

package object oadmin {

  lazy val passwords = webcrank.password.Passwords.pbkdf2() // TODO: register bouncycastle provider and use {digest = SHA512} . . .

  lazy val config = com.typesafe.config.ConfigFactory.load()

  lazy val AccessTokenSessionLifeTime = config.getInt("access-token-session-lifetime")

  lazy val RefreshTokenSessionLifeTime = config.getInt("refresh-token-session-lifetime")

  lazy val Q = scala.slick.driver.PostgresDriver.simple

  lazy val SuperUser = {
    val passwords = webcrank.password.Passwords.pbkdf2(digest = webcrank.password.SHA1)

    domain.User(
      java.util.UUID.fromString(config.getString("super-user-id")),
      config.getString("super-user-username"),
      passwords crypt config.getString("super-user-password"),
      config.getString("super-user-firstname"),
      config.getString("super-user-lastname"), createdAt = 0L, createdBy = None)
  }

  lazy val SuperUserR = domain.Role(config.getString("super-user-role-name"), None, createdAt = 0L, createdBy = None, public = false)

  lazy val AdministratorR = domain.Role(config.getString("administrator-role-name"), Some(SuperUserR.name), createdAt = 0L, createdBy = None, public = false)

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
}