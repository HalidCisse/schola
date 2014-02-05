package schola
package oadmin
package utils

// ------------------------------------------------------------------------------------------------------------

object Mailer {

  private val log = Logger("oadmin.mailer")

  def sendPasswordResetEmail(username: String, key: String)(implicit system: akka.actor.ActorSystem) {
    val subj = "[Schola] Password reset request"

    val msg = s"""
      | Someone requested that the password be reset for the following account:\r\n\r\n
      | Username: $username \r\n\r\n
      | If this was a mistake, just ignore this email and nothing will happen. \r\n\r\n
      | To reset your password, visit the following address:\r\n\r\n
      | < http://$Hostname${if (Port == 80) "" else ":" + Port}/RstPasswd?key=$key&login=${java.net.URLEncoder.encode(username, "UTF-8")} >\r\n""".stripMargin

    sendEmail(subj, username, (Some(msg), None))
  }

  def sendPasswordChangedNotice(username: String)(implicit system: akka.actor.ActorSystem) {
    val subj = "[Schola] Password change notice"

    val msg = s"""
      | Someone just changed the password for the following account:\r\n\r\n
      | Username: $username \r\n\r\n
      | If this was you, congratulation! the change was successfull. \r\n\r\n
      | Otherwise, contact your administrator immediately.\r\n""".stripMargin

    sendEmail(subj, username, (Some(msg), None))
  }

  def sendWelcomeEmail(username: String, password: String)(implicit system: akka.actor.ActorSystem) {
    val subj = "[Schola] Welcome to Schola!"

    val msg = s"""
      | Congratulation, your account was successfully created.\r\n\r\n
      | Here are the details:\r\n\r\n
      | Username: $username \r\n\r\n
      | Password: $password \r\n\r\n
      | Sign in immediately at < http://$Hostname${if (Port == 80) "" else ":" + Port}/Login > to reset your password and start using the service.\r\n\r\n
      | Thank you.\r\n""".stripMargin

    sendEmail(subj, username, (Some(msg), None))
  }

  val fromAddress = config.getString("smtp.from")

  private lazy val mock = config.getBoolean("smtp.mock")

  private lazy val mailer: MailerAPI = if (mock) {
    MockMailer
  } else {

    import scala.util.control.Exception.allCatch

    val smtpHost = config.getString("smtp.mailhub")
    val smtpPort = config.getInt("smtp.port")
    val smtpSsl = config.getBoolean("smtp.ssl")
    val smtpTls = config.getBoolean("smtp.tls")

    val smtpUser = allCatch.opt { config.getString("smtp.user") }
    val smtpPassword = allCatch.opt { config.getString("smtp.password") }

    new CommonsMailer(smtpHost, smtpPort, smtpSsl, smtpTls, smtpUser, smtpPassword)
  }

  private def sendEmail(subject: String, recipient: String, body: (Option[String], Option[String]))(implicit system: akka.actor.ActorSystem) {
    import scala.concurrent.duration._
    import system.dispatcher

    if (log.isDebugEnabled) {
      log.debug("[oadmin] sending email to %s".format(recipient))
      log.debug("[oadmin] mail = [%s]".format(body))
    }

    system.scheduler.scheduleOnce(1 second) {

      mailer.setSubject(subject)
      mailer.setRecipient(recipient)
      mailer.setFrom(fromAddress)

      mailer.setReplyTo(fromAddress)

      // the mailer plugin handles null / empty string gracefully
      mailer.send(body._1 getOrElse "", body._2 getOrElse "")
    }
  }
}

// -------------------------------------------------------------------------------------------------------------------------

object Crypto {

  import javax.crypto._, spec.{ PBEKeySpec, SecretKeySpec }

  import org.bouncycastle.util.encoders.Hex

  val Secret = config.getString("secret")

  val lineSeparator = System.getProperty("line.separator")

  def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Hex.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }

  def sign(message: String): String =
    sign(message, Secret.getBytes("utf-8"))

  val genMacKey = {

    val rounds = 65536

    val size = 256

    val alg = "PBKDF2WithHMACSHA1"

    (password: String) => {
      val spec = new PBEKeySpec(password.toCharArray, utils.randomBytes(16), rounds, size)
      val keys = SecretKeyFactory.getInstance(alg)
      val key = keys.generateSecret(spec)
      Hex.toHexString(key.getEncoded)
    }
  }

  def sha3(s: String): String = sha3(unifyLineSeparator(s).getBytes("ASCII"))

  @inline def sha3(bytes: Array[Byte]) = SHA3.digest(bytes)

  def generateSecureToken = {
    val bytes = Array.fill(32)(0.byteValue)
    random.nextBytes(bytes)
    sha3(bytes)
  }

  def signToken(token: String) = {
    val nonce = System.currentTimeMillis()
    val joined = nonce + "-" + token
    sign(joined) + "-" + joined
  }

  def extractSignedToken(token: String) = {
    token.split("-", 3) match {
      case Array(signature, nonce, raw) if constantTimeEquals(signature, sign(nonce + "-" + raw)) => Some(raw)
      case _ => None
    }
  }

  def constantTimeEquals(a: String, b: String) = {
    if (a.length != b.length) {
      false
    } else {
      var equal = 0
      for (i <- 0 until a.length) {
        equal |= a(i) ^ b(i)
      }
      equal == 0
    }
  }

  private def unifyLineSeparator(text: String): String = text.replaceAll(lineSeparator, "\n")
}

object `package` {

  @inline def genPasswd(key: String) = passwords.crypt(key)

  private[utils] val random = java.security.SecureRandom.getInstance("SHA1PRNG")

  def randomBytes(size: Int) = {
    val b = Array.fill(size)(0.byteValue)
    random.nextBytes(b)
    b
  }

  @inline def randomString(size: Int) = {
    org.bouncycastle.util.encoders.Hex.toHexString(randomBytes(size))
  }

  lazy val xxHash = {
    import net.jpountz.xxhash.XXHashFactory

    val f = XXHashFactory.fastestInstance().hash32

    (bytes: Array[Byte]) => {
      val x = f.hash(bytes, 0, bytes.length, 0xCAFEBABE)
      Integer.toHexString(x)
    }
  }

  @inline def timeF(thunk: => Any) = {
    val start = System.currentTimeMillis
    thunk
    System.currentTimeMillis - start
  }

  @inline def If[T](cond: Boolean, t: => T, f: => T) = if (cond) t else f

  @inline def option[T](cond: => Boolean, value: => T): Option[T] = if (cond) Some(value) else None  

  def copyToClipboard(s: String) {
    import java.awt.datatransfer.StringSelection
    import java.awt.Toolkit

    val stringSelection = new StringSelection(s)
    val clpbrd = Toolkit.getDefaultToolkit.getSystemClipboard
    
    clpbrd.setContents(stringSelection, null)
  }

  // ------------------------------------------------------------------------------------

  def findFieldStr(json: org.json4s.JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _                             => false
    } collect {
      case org.json4s.JField(_, org.json4s.JString(s)) => s
    }

  def findFieldJArr(json: org.json4s.JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _                             => false
    } collect {
      case org.json4s.JField(_, a @ org.json4s.JArray(_)) => a
    }

  def findFieldJObj(json: org.json4s.JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _                             => false
    } collect {
      case org.json4s.JField(_, o @ org.json4s.JObject(_)) => o
    }
}
