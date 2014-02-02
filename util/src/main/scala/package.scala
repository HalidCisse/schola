package schola
package oadmin

/**
 * Schola base Exception.
 */
@SerialVersionUID(1L)
class ScholaException(message: String, cause: Throwable) extends RuntimeException(message, cause) with Serializable {
  def this(msg: String) = this(msg, null)
}

object `package` {
  import com.typesafe.config._

  val config = {
    val root = ConfigFactory.load()
    root getConfig "oadmin"
  }

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

  val MACAlgorithm = config.getString("oauth2.mac-algorithm")
}