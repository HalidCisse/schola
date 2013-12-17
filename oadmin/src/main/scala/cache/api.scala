package schola
package oadmin

import org.apache.commons.lang3.reflect.TypeUtils

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
}

/**
 * Public Cache API.
 *
 * The underlying Cache implementation is received from plugin.
 */
object Cache {

  private object MemcachedSettings extends caching.Settings {
    import scala.util.control.Exception.allCatch

    val Hosts = {
      import net.spy.memcached.AddrUtil

      val singleHost = allCatch.opt(config.getString("memcached.host")).map(AddrUtil.getAddresses)
      val multipleHosts = allCatch.opt(config.getString("memcached.1.host")).map {
        _ =>
          def acc(nb: Int): String =
            allCatch.opt(config.getString("memcached." + nb + ".host")).map {
              h => h + " " + acc(nb + 1)
            }.getOrElse("")

          AddrUtil.getAddresses(acc(1))
      }

      singleHost.orElse(multipleHosts)
        .getOrElse(throw new RuntimeException("Bad configuration for memcached: missing host(s)"))
    }

    val User = allCatch.opt(config.getString("memcached.user"))

    val Passwd = allCatch.opt(config.getString("memcached.password"))

    val Namespace = allCatch.opt { config.getString("memcached.namespace") } getOrElse ""

    val Timeout = {
      import java.util.concurrent.TimeUnit

      lazy val timeout: Long = allCatch.opt(config.getLong("memcached.timeout")).getOrElse(1L)

      lazy val timeunit: TimeUnit = {
        allCatch.opt(config.getString("memcached.timeunit")).getOrElse("seconds") match {
          case "seconds" => TimeUnit.SECONDS
          case "milliseconds" => TimeUnit.MILLISECONDS
          case "microseconds" => TimeUnit.MICROSECONDS
          case "nanoseconds" => TimeUnit.NANOSECONDS
          case _ => TimeUnit.SECONDS
        }
      }

      Duration(timeout, timeunit)
    }

    val Hash = allCatch.opt(config.getString("memcached.hashkeys"))

    val Enabled = !allCatch.opt(config.getString("memcached")).filter(_ == "disabled").isDefined
  }

  private def cacheAPI: CacheAPI = new caching.Memcached(MemcachedSettings).api

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

  /**
   * Retrieve a value from the cache, or set it from a default function.
   *
   * @param key Item key.
   * @param expiration expiration period in seconds.
   * @param orElse The default function to invoke if the value was found in cache.
   */
  def getOrElse[A: scala.reflect.ClassTag](key: String, expiration: Int = 0)(orElse: => A): A =
    getAs[A](key) getOrElse {
      val value = orElse
      set(key, value, expiration)
      value
    }

  /**
   * Retrieve a value from the cache for the given type
   *
   * @param key Item key.
   * @return result as Option[T]
   */
  def getAs[T: scala.reflect.ClassTag](key: String): Option[T] =
    get(key) flatMap {
      item =>
        if (TypeUtils.isInstance(item, implicitly[scala.reflect.ClassTag[T]].runtimeClass)) Some(item.asInstanceOf[T]) else None
    }

  def remove(key: String) {
    cacheAPI.remove(key)
  }
}