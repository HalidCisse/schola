package ma.epsilon.schola
package utils

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

  @inline def tryo[T](thunk: => T) = try {
    thunk; true
  } catch {
    case scala.util.control.NonFatal(_) => false
  }

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

    (bytes: Array[Byte]) => f.hash(bytes, 0, bytes.length, 0xCAFEBABE)
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
}
