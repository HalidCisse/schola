package ma.epsilon.schola

import java.time.{ Instant, LocalDateTime, LocalDate, Clock, ZoneOffset }

import scala.util.{ DynamicVariable, Try }
import scala.util.control.Exception.allCatch

import java.util.{ Locale, UUID }

import webcrank.password.{ Passwords, SHA512 }

/**
 * Schola base Exception.
 */
@SerialVersionUID(1L)
class ScholaException(message: String, cause: Throwable) extends RuntimeException(message, cause) with Serializable {
  def this(msg: String) = this(msg, null)
}

@SerialVersionUID(1L)
case object InvalidUuidException extends ScholaException("invalid.uuid")

object `package` {
  import com.typesafe.config._

  val config = {
    val root = ConfigFactory.load()
    root getConfig "schola"
  }

  val OAUTH_CLIENT = "schola"

  val OAUTH_CLIENT_SECRET = "schola"

  val OAUTH_REDIRECT_URI = "http://localhost/"

  val passwords = Passwords.pbkdf2(digest = SHA512)

  val API_VERSION = config.getString("api-version")

  val MaxUploadSize = config.getInt("max-upload-size")

  val PasswordMinLength = config.getInt("password-min-length")

  val SESSION_KEY = "_session_key"

  val ACTIVE_RIGHT_KEY = "_active_right_key"

  val AccessTokenSessionLifeTime = config.getInt("oauth2.access-token-session-lifetime")

  val RefreshTokenSessionLifeTime = config.getInt("oauth2.refresh-token-session-lifetime")

  val MACAlgorithm = config.getString("oauth2.mac-algorithm")

  @inline def Logger(name: String) = org.slf4j.LoggerFactory.getLogger(name)

  type Uuid = java.util.UUID

  object Uuid {
    def apply(s: String) = allCatch.opt { UUID.fromString(s) } getOrElse { throw InvalidUuidException } // java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
    def unapply(s: String) = Try(UUID.fromString(s)).toOption
    def unapply(uuid: Uuid) = Try(uuid.toString).toOption
  }

  val locale = new DynamicVariable[Locale](Locale.getDefault)
  implicit def _locale = locale.value

  val clock = new DynamicVariable[Clock](Clock.systemUTC())
  implicit def _clock = clock.value

  @inline def dateNow(implicit clock: Clock) = LocalDate.now(_clock)

  @inline def now(implicit clock: Clock) = Instant.now(_clock)

  @inline implicit def asDateTime(time: Instant) = LocalDateTime.ofInstant(time, ZoneOffset.UTC)

  // ------------------------------------------------------------------------

  case class Page(offset: Int, fetch: Int)
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