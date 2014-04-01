package ma.epsilon.schola

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
    root getConfig "schola"
  }

  val passwords = webcrank.password.Passwords.scrypt(n = 4096) // TODO: register bouncycastle provider and use {digest = SHA512} . . .

  val MaxResults = 100

  val API_VERSION = config.getString("api-version")

  val MaxUploadSize = config.getInt("max-upload-size")

  val PasswordMinLength = config.getInt("password-min-length")

  val SESSION_KEY = "_session_key"

  val AccessTokenSessionLifeTime = config.getInt("oauth2.access-token-session-lifetime")

  val RefreshTokenSessionLifeTime = config.getInt("oauth2.refresh-token-session-lifetime")

  val MACAlgorithm = config.getString("oauth2.mac-algorithm")

  @inline def Logger(name: String) = org.slf4j.LoggerFactory.getLogger(name)

  @inline def uuid(s: String) = scala.util.control.Exception.allCatch.opt { java.util.UUID.fromString(s) } getOrElse java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
}

// ------------------------------------------------------------------------------------------------------------

trait MailingComponent {

  val mailer: Mailer

  trait Mailer {

    def sendPasswordResetEmail(username: String, key: String)

    def sendPasswordChangedNotice(username: String)

    def sendWelcomeEmail(username: String, password: String)
  }
}