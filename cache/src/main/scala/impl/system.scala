package ma.epsilon.schola
package caching
package impl

import akka.actor._

import java.util.concurrent.{ ArrayBlockingQueue, TimeUnit, ThreadPoolExecutor }
import scala.concurrent.{ Future, ExecutionContext }

trait CacheSystemProvider {
  val cacheSystem: CacheSystem
}

class CacheSystem(val TTL: Int, updateIntervalMin: Int = 30)(implicit system: ActorSystem) {

  import CacheSystem._
  import CacheActor._
  import scala.concurrent.duration._

  private[this] val log = Logger("schola.cacheSystem")

  system.registerOnTermination {
    log.info("Terminating cacheSystem execution context...")
    cacheSystemThreadPoolExecutor.shutdown()
  }

  def createCacheActor(cacheName: String,
                       actorCreator: CacheSystem => Actor,
                       scheduleDelay: FiniteDuration = 10 seconds) =
    system.actorOf(Props(actorCreator(this)), name = cacheName + "CacheActor")

  def findObjectForCache[T](key: String,
                            default: () => T): Option[T] = {
    val obj = default()

    if (obj.asInstanceOf[AnyRef] ne None) {
      Cache.set(key, obj, TTL)
      Some(obj)
    } else None
  }
}

abstract class CacheActor(cacheSystem: CacheSystem)
    extends Actor with ActorLogging {

  import CacheActor._
  import CacheSystem._
  import akka.pattern._

  def findValueReceive: Receive = {
    case FindValue(params, default) => findValueForSender(params, default, sender)
  }

  def purgeValueReceive: Receive = {
    case PurgeValue(params) => purgeValueForSender(params)
  }

  def purgeValueForSender(params: CacheActor.Params) {
    Cache.remove(params.cacheKey)
  }

  def findValueForSender[T](params: CacheActor.Params, default: () => T, sender: ActorRef) {
    val key = params.cacheKey
    val elem = Cache.get(key)

    if (elem ne None)
      sender ! elem
    else
      Future {
        findObject(params, default)
      } pipeTo sender
  }

  def findObject[T](params: CacheActor.Params, default: () => T): Option[Any] =
    cacheSystem.findObjectForCache(params.cacheKey, default)
}

object CacheActor {

  case class FindValue[T](params: Params, default: () => T)

  case class PurgeValue(params: Params)

  trait Params {
    def cacheKey: String
  }

  // Thread pool used by findValueForSender()
  val FUTURE_POOL_SIZE = config.getInt("cache.pool-size")

  private[impl] lazy val cacheSystemThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_POOL_SIZE, true))

  implicit lazy val findValueExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(cacheSystemThreadPoolExecutor)
}

class OAdminCacheActor(cacheSystem: CacheSystem)
    extends CacheActor(cacheSystem) {

  override def receive = findValueReceive orElse purgeValueReceive
}

object Params {
  def apply(uuid: Uuid): Params = Params(uuid.toString)
}

case class Params(cacheKey: String)
  extends CacheActor.Params