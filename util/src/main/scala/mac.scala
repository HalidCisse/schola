package ma.epsilon.schola.utils

import scala.util.control.Exception.allCatch

/**
 * MAC request signing as defined by
 *  http://tools.ietf.org/html/draft-hammer-oauth-v2-mac-token-05
 *  The MAC protocol defines
 *  1. MAC key identifier (access token value)
 *  2. MAC key (access token secret)
 *  3. MAC algorithm - one of ("hmac-sha-1" or "hmac-sha-256")
 *  4. Issue time - time when credentials were issued to calculate the age
 */
trait Signing {
  import org.apache.commons.codec.binary.Base64.encodeBase64

  type Req = {
    def method: String
    def uri: String
    def host: String
  }

  object HostPort {

    object Int {
      def unapply(str: String) = allCatch.opt { str.toInt }
    }

    def unapply(req: Req): Option[(String, Int)] =
      req.host.split(':') match {
        case Array(host, Int(port)) => Some(host, port)
        case _                      => Some(req.host, 80)
      }
  }

  val HmacSha1 = "HmacSHA1"
  val HmacSha256 = "HmacSHA256"
  val charset = "UTF8"
  val MacAlgorithms = Map("hmac-sha-1" -> HmacSha1, "hmac-sha-256" -> HmacSha256)
  private val JAlgorithms = Map(HmacSha1 -> "SHA-1", HmacSha256 -> "SHA-256")

  implicit def s2b(s: String) = s.getBytes(charset)

  /** @return Either[String error, String hashed value] */
  def hash(data: Array[Byte])(algo: String) =
    JAlgorithms.get(algo) match {
      case Some(h) =>
        val msg = java.security.MessageDigest.getInstance(h)
        msg.update(data)
        Right(msg.digest)
      case unsup => Left("unsupported algorithm %s" format unsup)
    }

  /** @return Either[String error, String hashed value] */
  def macHash(alg: String, key: String)(body: String) =
    if (MacAlgorithms.isDefinedAt(alg)) {
      val macAlg = MacAlgorithms(alg)
      val mac = javax.crypto.Mac.getInstance(macAlg)
      mac.init(new javax.crypto.spec.SecretKeySpec(key, macAlg))
      Right(new String(encodeBase64(mac.doFinal(body)), charset))
    } else Left("unsupported mac algorithm %s" format alg)

  def bodyhash(body: Array[Byte])(alg: String) =
    hash(body)(alg).fold({ Left(_) }, { h => Right(new String(encodeBase64(h), charset)) })

  /** @return signed request for a given key, request, and algorithm */
  def sign(r: Req, nonce: String, ext: Option[String],
           bodyHash: Option[String], key: String, alg: String): Either[String, String] =
    requestString(r, alg, nonce, ext, bodyHash).fold({ Left(_) }, { rstr =>
      sign(key, rstr, alg)
    })

  /** @return Either[String error, String mac signed req] */
  def sign(key: String, request: String, alg: String): Either[String, String] =
    macHash(alg, key)(request)

  /** calculates the normalized the request string from a request */
  def requestString(r: Req, alg: String,
                    nonce: String, ext: Option[String], bodyHash: Option[String]): Either[String, String] =
    MacAlgorithms.get(alg) match {
      case None => Left("unsupported mac algorithm %s" format alg)
      case Some(macAlg) =>
        r match {
          case HostPort(hostname, port) =>
            Right(requestString(nonce, r.method, r.uri,
              hostname, port, "", ext.getOrElse("")))
        }
    }

  /** calculates the normalized request string from parts of a request */
  def requestString(nonce: String, method: String, uri: String, hostname: String,
                    port: Int, bodyhash: String, ext: String): String =
    nonce :: method :: uri :: hostname :: port ::
      bodyhash :: ext :: Nil mkString ("", "\n", "\n")
}