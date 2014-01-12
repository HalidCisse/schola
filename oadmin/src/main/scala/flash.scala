package schola
package oadmin
package libs

import java.net.{ URLEncoder, URLDecoder }

import scala.util.control.NonFatal

import unfiltered.Cookie
import unfiltered.response.SetCookies

/**
 * Trait that should be extended by the Cookie helpers.
 */
trait CookieBaker[T <: AnyRef] {

  /**
   * The cookie name.
   */
  def COOKIE_NAME: String

  /**
   * Default cookie, returned in case of error or if missing in the HTTP headers.
   */
  def emptyCookie: T

  /**
   * `true` if the Cookie is signed. Defaults to false.
   */
  def isSigned: Boolean = false

  /**
   * `true` if the Cookie should have the httpOnly flag, disabling access from Javascript. Defaults to true.
   */
  def httpOnly = true

  /**
   * The cookie expiration date in seconds, `None` for a transient cookie
   */
  def maxAge: Option[Int] = None

  /**
   * The cookie domain. Defaults to None.
   */
  def domain: Option[String] = None

  /**
   * `true` if the Cookie should have the secure flag, restricting usage to https. Defaults to false.
   */
  def secure = false

  /**
   *  The cookie path.
   */
  def path = "/"

  /**
   * Encodes the data as a `String`.
   */
  def encode(data: Map[String, String]): String = {
    val encoded = data.map {
      case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
    }.mkString("&")
    if (isSigned)
      utils.Crypto.sign(encoded) + "-" + encoded
    else
      encoded
  }

  /**
   * Decodes from an encoded `String`.
   */
  def decode(data: String): Map[String, String] = {

    def urldecode(data: String) = {
      data
        .split("&")
        .map(_.split("=", 2))
        .map(p => URLDecoder.decode(p(0), "UTF-8") -> URLDecoder.decode(p(1), "UTF-8"))
        .toMap
    }

    // Do not change this unless you understand the security issues behind timing attacks.
    // This method intentionally runs in constant time if the two strings have the same length.
    // If it didn't, it would be vulnerable to a timing attack.
    def safeEquals(a: String, b: String) = {
      if (a.length != b.length) {
        false
      } else {
        var equal = 0
        for (i <- Array.range(0, a.length)) {
          equal |= a(i) ^ b(i)
        }
        equal == 0
      }
    }

    try {
      if (isSigned) {
        val splitted = data.split("-", 2)
        val message = splitted.tail.mkString("-")
        if (safeEquals(splitted(0), utils.Crypto.sign(message)))
          urldecode(message)
        else
          Map.empty[String, String]
      } else urldecode(data)
    } catch { // fail gracefully is the session cookie is corrupted
      case NonFatal(_) => Map.empty[String, String]
    }
  }

  /**
   * Encodes the data as a `Cookie`.
   */
  def encodeAsCookie(data: T): Cookie = {
    val cookie = encode(serialize(data))
    Cookie(COOKIE_NAME, cookie, domain, Some(path), maxAge, Some(secure), httpOnly)
  }

  def discard = SetCookies.discarding(COOKIE_NAME)

  /**
   * Decodes the data from a `Cookie`.
   */
  def decodeFromCookie(cookie: Option[Cookie]): T = {
    cookie.filter(_.name == COOKIE_NAME).map(c => deserialize(decode(c.value))).getOrElse(emptyCookie)
  }

  /**
   * Builds the cookie object from the given data map.
   *
   * @param data the data map to build the cookie object
   * @return a new cookie object
   */
  protected def deserialize(data: Map[String, String]): T

  /**
   * Converts the given cookie object into a data map.
   *
   * @param cookie the cookie object to serialize into a map
   * @return a new `Map` storing the key-value pairs for the given cookie
   */
  protected def serialize(cookie: T): Map[String, String]
}

/**
 * HTTP Flash scope.
 *
 * Flash data are encoded into an HTTP cookie, and can only contain simple `String` values.
 */
case class Flash(data: Map[String, String] = Map.empty[String, String]) {

  /**
   * Optionally returns the flash value associated with a key.
   */
  def get(key: String) = data.get(key)

  /**
   * Returns `true` if this flash scope is empty.
   */
  def isEmpty: Boolean = data.isEmpty

  /**
   * Adds a value to the flash scope, and returns a new flash scope.
   *
   * For example:
   * {{{
   * flash + ("success" -> "Done!")
   * }}}
   *
   * @param kv the key-value pair to add
   * @return the modified flash scope
   */
  def +(kv: (String, String)) = {
    require(kv._2 != null, "Cookie values cannot be null")
    copy(data + kv)
  }

  /**
   * Removes a value from the flash scope.
   *
   * For example:
   * {{{
   * flash - "success"
   * }}}
   *
   * @param key the key to remove
   * @return the modified flash scope
   */
  def -(key: String) = copy(data - key)

  /**
   * Retrieves the flash value that is associated with the given key.
   */
  def apply(key: String) = data(key)
}

/**
 * Helper utilities to manage the Flash cookie.
 */
object Flash extends CookieBaker[Flash] {

  val COOKIE_NAME = "_FLASH"

  override def path = "/"

  override val isSigned = true

  override val httpOnly = false

  val emptyCookie = new Flash

  def deserialize(data: Map[String, String]) = new Flash(data)

  def serialize(flash: Flash) = flash.data
}