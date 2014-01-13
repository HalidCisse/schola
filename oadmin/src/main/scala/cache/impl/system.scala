package schola
package oadmin
package impl

import akka.actor._
import org.clapper.avsl.Logger

import java.util.concurrent.{ ArrayBlockingQueue, TimeUnit, ThreadPoolExecutor }
import scala.concurrent.{ Future, ExecutionContext }

trait CacheSystemProvider {
  val cacheSystem: CacheSystem
}

object CacheSystem {

  case object UpdateCacheForNow

}

class CacheSystem(val TTL: Int, updateIntervalMin: Int = 30)(implicit system: ActorSystem = system) {

  import CacheSystem._
  import CacheActor._
  import scala.concurrent.duration._

  val log = Logger("oadmin.cacheSystem")

  system.registerOnTermination {
    log.info("Terminating cacheSystem execution context...")
    cacheSystemThreadPoolExecutor.shutdown()
  }

  def createCacheActor(cacheName: String,
                       actorCreator: CacheSystem => Actor,
                       scheduleDelay: FiniteDuration = 10 seconds) = {

    val actor = system.actorOf(Props(actorCreator(this)), name = cacheName + "CacheActor")

    // TODO: use pro-active caching
    system.scheduler.schedule(scheduleDelay,
      updateIntervalMin minutes,
      actor, UpdateCacheForNow)

    actor
  }

  def findObjectForCache[T](key: String,
                            default: () => T): Option[T] = {
    val obj = default()

    if (obj != None) {
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

  def updateCacheForNowReceive: Receive = {
    case UpdateCacheForNow => updateCacheForNow()
  }

  def updateCacheForNow()

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

  sealed trait Params {
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

  override def receive = _busy

  private val _busy = findValueReceive orElse purgeValueReceive orElse updateCacheForNowReceive

  def updateCacheForNow() { // TODO: Fix UpdateCacheForNow
    log.info("updating cache for now . . .")

    context.become({ case _ => () })

    val elapsed = utils.timeF {
      import Façade.nocache._

      def pageCount() = {
        import scala.slick.jdbc.{ StaticQuery => T }
        import T.interpolation

        val pages = sql""" SELECT (count(*) OVER () / $MaxResults) + 1 as pages FROM users; """.as[Int]

        S.withSession { implicit session =>
          pages.firstOption getOrElse 1
        }
      }

      for (page <- 0 until pageCount()) {
        val users = oauthService.getUsers(page)
        Cache.set(s"users_$page", users)

        users foreach (user => Cache.set(user.id.get.toString, Some(user)))
      }

      val roles = accessControlService.getRoles
      Cache.set("roles", roles)
      roles foreach (role => Cache.set(role.name, Some(role)))
    }

    context.become(_busy)

    log.info(s"cache successfully updated in  ${elapsed / 1000} secs")
  }
}

case class Params(cacheKey: String)
  extends CacheActor.Params

case object RoleParams extends CacheActor.Params {
  val cacheKey = "roles"
}

private[impl] class UserParams private (page: () => Int) extends CacheActor.Params {
  lazy val cacheKey = s"users_${page()}"
}

object UserParams {
  def apply(page: Int = 0): UserParams = apply(() => page)
  def apply(page: () => Int) = new UserParams(page)
}
