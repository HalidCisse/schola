package schola
package oadmin
package impl

import akka.actor._

//import scala.concurrent.duration.FiniteDuration
import java.util.concurrent.{ArrayBlockingQueue, TimeUnit, ThreadPoolExecutor}
import scala.concurrent.{Future, ExecutionContext}

trait CacheSystemProvider {
  val cacheSystem: CacheSystem
}

class CacheSystem(val maxTTL: Int)(implicit system: ActorSystem = system) {

  def createCacheActor(cacheName: String,
//                       scheduleDelay: FiniteDuration,
                       actorCreator: CacheSystem => Actor) = {

    val actor = system.actorOf(Props(actorCreator(this)), name = cacheName + "CacheActor")

    // TODO: use pro-active caching
    //    system.scheduler.schedule(scheduleDelay,
    //      updateIntervalMin minutes,
    //      actor, UpdateCacheForNow)

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

    val domainObj = default()

    if (domainObj != None) {
      Cache.set(key, domainObj, maxTTL)
      Some(domainObj)
    } else None
  }

  //  def clearAllCaches() {
  //    Cache.foreach(_.removeAll())
  //  }
}

abstract class CacheActor(cacheSystem: CacheSystem)
  extends Actor with ActorLogging {

  import CacheActor._
  import akka.pattern._

  def findValueReceive: Receive = {
    case FindValue(params, default) => findValueForSender(params, default, sender)
  }

  def purgeValueReceive: Receive = {
    case PurgeValue(params) => purgeValueForSender(params)
  }

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

  // Thread pool used by findValueForSender()
  val FUTURE_POOL_SIZE = 2 // TODO: make `FUTURE_POOL_SIZE` a config value

  private lazy val findValueThreadPoolExecutor =
    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
      1, TimeUnit.MINUTES,
      new ArrayBlockingQueue(FUTURE_POOL_SIZE, true))

  implicit lazy val findValueExecutionContext: ExecutionContext =
    ExecutionContext.fromExecutor(findValueThreadPoolExecutor)
}

class OAdminCacheActor(cacheSystem: CacheSystem)
  extends CacheActor(cacheSystem) {

  override def receive = findValueReceive orElse purgeValueReceive

  //  override def updateCacheForDate(date: Date) {
  //    import DateCacheActor._
  //    Future { findObject(new Service1Params(date, true)) }
  //    Future { findObject(new Service1Params(date, false)) }
  //  }

//  def finder(params: CacheActor.Params) = {
//    () =>
//      params match {
//        case UserParams(cacheKey) => Façade.oauthService.getUser(cacheKey)
//        case _: UsersParams => Façade.oauthService.getUsers
//        case _ => throw new IllegalArgumentException("unmatched params in UserCacheActor")
//      }
//  }
}

case class UserParams(cacheKey: String)
  extends CacheActor.Params

case class UsersParams(offset: Int = 0, size: Int = 50) extends CacheActor.Params {
  def cacheKey = "users"
}
