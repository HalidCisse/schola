package schola
package oadmin
package impl

import akka.actor._
import org.clapper.avsl.Logger

import java.util.concurrent.{ArrayBlockingQueue, TimeUnit, ThreadPoolExecutor}
import scala.concurrent.{Future, ExecutionContext}

trait CacheSystemProvider {
  val cacheSystem: CacheSystem
}

object CacheSystem {

  case object UpdateCacheForNow

}

class CacheSystem(val maxTTL: Int, updateIntervalMin: Int = 30)(implicit system: ActorSystem = system) {

  import CacheSystem._
  import CacheActor._
  import scala.concurrent.duration._

  val log = Logger("oadmin.cacheSystem")

  system.registerOnTermination {
    log.info("Terminating execution context...")
    CacheActor.cacheSystemThreadPoolExecutor.shutdown()
  }

  def createCacheActor(cacheName: String,
                       actorCreator: CacheSystem => Actor,
                       scheduleDelay: FiniteDuration = 10 seconds) = {

    val actor = system.actorOf(Props(actorCreator(this)), name = cacheName + "CacheActor")

    // TODO: use pro-active caching
    system.scheduler.schedule(scheduleDelay,
      updateIntervalMin minutes,
      actor, UpdateCacheForNow)

    //    if (!DateUtil.isNowInActiveBusinessDay) {
    //      actorSystem.scheduler.scheduleOnce(scheduleDelay, actor,
    //        UpdateCacheForPreviousBusinessDay)
    //    }

    actor
  }

  //  def findCachedObject[T](key: String,
  //                          finder: () => T): Option[T] = {
  //
  //    val element = Cache.get(key)
  //
  //    if (element eq None)
  //      findObjectForCache(key, finder)
  //    else
  //      Some(element.asInstanceOf[T])
  //  }

  def findObjectForCache[T](key: String,
                            default: () => T): Option[T] = {
    val obj = default()

    if(obj != None) {
      Cache.set(key, obj, maxTTL)
      Some(obj)
    }
    else None
  }
//    default() match {
//      case els: Traversable[_] =>
//
//        Cache.set(key, els, maxTTL)
//        Some(els.asInstanceOf[T])
//
//      case s@Some(_) =>
//
//        Cache.set(key, s, maxTTL)
//        s.asInstanceOf[Option[T]]
//
//      case _ => None
//    }


  //  def clearAllCaches() {
  //    Cache.foreach(_.removeAll())
  //  }
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

  def updateCacheForNowReceive: Receive = {
    case UpdateCacheForNow => updateCacheForNow()
  }

  def updateCacheForNow()

  def purgeValueForSender(params: Params) {
    Cache.remove(params.cacheKey)
  }

  def findValueForSender[T](params: Params, default: () => T, sender: ActorRef) {
    val key = params.cacheKey
    val elem = Cache.get(key)

    if (elem ne None)
      sender ! elem
    else
      Future {
        findObject(params, default)
      } pipeTo sender
  }

  def findObject[T](params: Params, default: () => T): Option[Any] =
    cacheSystem.findObjectForCache(params.cacheKey, default)
}

object CacheActor {

  case class FindValue[T](params: Params, default: () => T)

  case class PurgeValue(params: Params)

  trait Params {
    def cacheKey: String
  }

  // Thread pool used by findValueForSender() & utils.Avatars
  val FUTURE_POOL_SIZE = 4 // TODO: make `FUTURE_POOL_SIZE` a config value

  private[impl] lazy val cacheSystemThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_POOL_SIZE, true))

  implicit lazy val findValueExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(cacheSystemThreadPoolExecutor)
}

class OAdminCacheActor(cacheSystem: CacheSystem)
  extends CacheActor(cacheSystem) {

  override def receive =
    findValueReceive orElse purgeValueReceive orElse updateCacheForNowReceive

  def updateCacheForNow() {
    log.info("updating cache for now . . .")

    val elapsed = utils.timeF {
      for(user <- Façade.oauthService.getUsers)
        Façade.oauthService.getUser(user.id.get.toString)
    }

    log.info(s"cache successfully updated in  $elapsed msecs")
  }
}

case class UserParams(cacheKey: String)
  extends CacheActor.Params

case class UsersParams(offset: Int = 0, size: Int = 50) extends CacheActor.Params {
  def cacheKey = "users"
}
