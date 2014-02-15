package schola
package oadmin

import scala.concurrent.duration.Duration

/**
 * API for a Cache.
 */
trait CacheAPI {

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time in seconds (0 second means eternity).
   */
  def set(key: String, value: Any, expiration: Int)

  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  def get(key: String): Option[Any]

  /**
   * Remove a value from the cache
   */
  def remove(key: String)

  /**
   * Remove all values from the cache
   */
  def clearAll()
}

/**
 * Public Cache API.
 *
 * The underlying Cache implementation is received from plugin.
 */
object Cache {
  import com.typesafe.config.Config

  class MemcachedSettings(config: Config) extends caching.Settings {
    import scala.util.control.Exception.allCatch

    val Hosts = {
      import net.spy.memcached.AddrUtil

      val singleHost = allCatch.opt { config.getString("host") } map AddrUtil.getAddresses
      val multipleHosts = allCatch.opt { config.getString("host.1") } map {
        _ =>
          def acc(nb: Int): String =
            allCatch.opt { config.getString("host." + nb) } map {
              h => h + " " + acc(nb + 1)
            } getOrElse ""

          AddrUtil.getAddresses(acc(1))
      }

      singleHost.orElse(multipleHosts)
        .getOrElse(throw new RuntimeException("Bad configuration for memcached: missing host(s)"))
    }

    val User = allCatch.opt { config.getString("user") }

    val Passwd = allCatch.opt { config.getString("password") }

    val Namespace = config.getString("namespace")

    val Timeout = Duration(config.getString("timeout"))
  }

  private object NullApi extends CacheAPI {
    def set(key: String, value: Any, expiration: Int) {}
    def get(key: String) = None
    def remove(key: String) {}
    def clearAll() {}
  }

  private val Enabled = config.getBoolean("memcached.enabled")

  private val cacheAPI: CacheAPI = if (Enabled) new caching.Memcached(new MemcachedSettings(config getConfig "memcached")).api else NullApi

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time in seconds (0 second means eternity).
   */
  def set(key: String, value: Any, expiration: Int = 0) {
    cacheAPI.set(key, value, expiration)
  }

  /**
   * Set a value into the cache.
   *
   * @param key Item key.
   * @param value Item value.
   * @param expiration Expiration time as a [[scala.concurrent.duration.Duration]].
   */
  def set(key: String, value: Any, expiration: Duration) {
    set(key, value, expiration.toSeconds.toInt)
  }

  /**
   * Retrieve a value from the cache.
   *
   * @param key Item key.
   */
  def get(key: String): Option[Any] =
    cacheAPI.get(key)

  def remove(key: String) {
    cacheAPI.remove(key)
  }

  def clearAll() {
    cacheAPI.clearAll()
  }
}