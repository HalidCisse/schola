package schola
package oadmin

package object caching {

  import net.spy.memcached.auth.{PlainCallbackHandler, AuthDescriptor}
  import net.spy.memcached.{ConnectionFactoryBuilder, AddrUtil, MemcachedClient}
  import net.spy.memcached.transcoders.{Transcoder, SerializingTranscoder}

  import org.clapper.avsl.Logger

  trait Settings {
    val Hosts: java.util.List[java.net.InetSocketAddress]
    val User: Option[String]
    val Passwd: Option[String]
    val Namespace: String
    val Timeout: scala.concurrent.duration.Duration
    val Hash: Option[String]
    val Enabled: Boolean
  }

  class Memcached(settings: Settings) {
    import settings._

    lazy val log = Logger("oadmin.memcached")

    lazy val client =
      User.map {
        memcacheUser =>
          val memcachePassword = Passwd getOrElse {
            throw new RuntimeException("Bad configuration for memcached: missing password")
          }

          // Use plain SASL to connect to memcached
          val ad = new AuthDescriptor(Array("PLAIN"),
            new PlainCallbackHandler(memcacheUser, memcachePassword))
          val cf = new ConnectionFactoryBuilder()
            .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
            .setAuthDescriptor(ad)
            .build()

          new MemcachedClient(cf, Hosts)
      }.getOrElse {
        new MemcachedClient(Hosts)
      }

    import java.io._

    class CustomSerializing extends SerializingTranscoder {

      // You should not catch exceptions and return nulls here,
      // because you should cancel the future returned by asyncGet() on any exception.
      override protected def deserialize(data: Array[Byte]): java.lang.Object = {
        new java.io.ObjectInputStream(new java.io.ByteArrayInputStream(data)) {
          override protected def resolveClass(desc: ObjectStreamClass) = {
            Class.forName(desc.getName /*, false, play.api.Play.current.classloader*/) // TODO: set classloader
          }
        }.readObject()
      }

      // We don't catch exceptions here to make it corresponding to `deserialize`.
      override protected def serialize(obj: java.lang.Object) = {
        val bos: ByteArrayOutputStream = new ByteArrayOutputStream()
        // Allows serializing `null`.
        // See https://github.com/mumoshu/play2-memcached/issues/7
        new ObjectOutputStream(bos).writeObject(obj)
        bos.toByteArray
      }
    }

    lazy val tc = new CustomSerializing().asInstanceOf[Transcoder[Any]]

    lazy val api = new CacheAPI {

      def get(key: String) =
        if (key.isEmpty) {
          None
        } else {
          log.debug("Getting the cached for key " + Namespace + key)
          val future = client.asyncGet(Namespace + hash(key), tc)
          try {
            val any = future.get(Timeout.length, Timeout.unit)
            if (any != null) {
              log.debug("any is " + any.getClass)
            }
            Option(
              any match {
                case x: java.lang.Byte => x.byteValue()
                case x: java.lang.Short => x.shortValue()
                case x: java.lang.Integer => x.intValue()
                case x: java.lang.Long => x.longValue()
                case x: java.lang.Float => x.floatValue()
                case x: java.lang.Double => x.doubleValue()
                case x: java.lang.Character => x.charValue()
                case x: java.lang.Boolean => x.booleanValue()
                case x => x
              }
            )
          } catch {
            case e: Throwable =>
              log.error("An error has occured while getting the value from memcached", e)
              future.cancel(false)
              None
          }
        }

      def set(key: String, value: Any, expiration: Int) {
        if (!key.isEmpty) {
          client.set(Namespace + hash(key), expiration, value, tc)
        }
      }

      def remove(key: String) {
        if (!key.isEmpty) {
          client.delete(Namespace + hash(key))
        }
      }
    }

    // you may override hash implementation to use more sophisticated hashes, like xxHash for higher performance
    protected def hash(key: String): String =
      Hash.map {key => java.security.MessageDigest.getInstance(key).digest(key.getBytes).map("%02x".format(_)).mkString } getOrElse key
  }
}

