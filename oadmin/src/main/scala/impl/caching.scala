package schola
package oadmin

package impl

trait CachingAccessControlServicesComponentImpl extends CachingServicesComponent with AccessControlServicesComponent{
  self: impl.CacheSystemProvider =>

  trait CachingAccessControlServicesImpl extends AccessControlServices {

    abstract override def getRoles =
      cachingServices.get[List[RoleLike]](RoleParams) { super.getRoles } getOrElse Nil

    abstract override def getRole(roleName: String) =
      cachingServices.get[Option[RoleLike]](Params(roleName)) { super.getRole(roleName) } flatten

    abstract override def saveRole(name: String, parent: Option[String], createdBy: Option[String]) =
      super.saveRole(name, parent, createdBy) collect{
        case my =>
          cachingServices.evict(Params(my.name))
          cachingServices.evict(RoleParams)
          my
      }

    abstract override def updateRole(name: String, newName: String, parent: Option[String]) =
      super.updateRole(name, newName, parent) && {
        cachingServices.evict(Params(newName))
        cachingServices.evict(RoleParams)
        true
      }

    abstract override def purgeRoles(roles: Set[String]) {
      super.purgeRoles(roles)
      roles foreach(o => cachingServices.evict(Params(o)))
      cachingServices.evict(RoleParams)
    }
  }
}

trait CachingOAuthServicesComponentImpl extends CachingServicesComponent with OAuthServicesComponent{
  self: impl.CacheSystemProvider =>

  trait CachingOAuthServicesImpl extends OAuthServices {

    private def pageOf(id: String) = {
      import scala.slick.jdbc.{StaticQuery=>T}
      import T.interpolation

      val page = sql""" SELECT (row_number() OVER () - 1) / $MaxResults as page FROM users WHERE id = CAST($id AS UUID); """.as[Int]

      () => S.withSession { implicit session =>
        page.firstOption getOrElse 0
      }
    }

    abstract override def getUsers(page: Int) =
      cachingServices.get[List[UserLike]](UserParams(page)) { super.getUsers(page) } getOrElse Nil

    abstract override def getUser(id: String) =
      cachingServices.get[Option[UserLike]](Params(id)) { super.getUser(id) } flatten

    abstract override def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: domain.Contacts, changePasswordAtNextLogin: Boolean) =
      super.saveUser(username, password, givenName, familyName, createdBy, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) collect{
        case my =>
          cachingServices.evict(Params(my.id.get.toString))

          cachingServices.evict(UserParams(page = pageOf(my.id.get.toString)))

          my
      }

    abstract override def updateUser(id: String, spec: utils.UserSpec) =
      super.updateUser(id, spec) && {

        cachingServices.evict(Params(id))

        cachingServices.evict(UserParams(page = pageOf(id)))

        true
      }

    abstract override def removeUser(id: String): Boolean =
      super.removeUser(id) && {
        cachingServices.evict(Params(id))

        cachingServices.evict(UserParams(page = pageOf(id)))

        true
      }

    abstract override def purgeUsers(users: Set[String]) {
      super.purgeUsers(users)
      users foreach(o => {cachingServices.evict(Params(o)); cachingServices.evict(UserParams(page = pageOf(o)))})
    }
  }
}

trait CachingServicesComponentImpl extends CachingServicesComponent {
  self: impl.CacheSystemProvider =>

  import scala.concurrent.duration._
  import akka.pattern._
  import akka.util.Timeout
  import scala.util.control.Exception.allCatch
  import scala.concurrent.Await

  protected val cachingServices = new CachingServicesImpl

  protected lazy val cacheActor = cacheSystem.createCacheActor("OAdmin", new impl.OAdminCacheActor(_))

  class CachingServicesImpl extends CachingServices {

    def get[T : scala.reflect.ClassTag](params: impl.CacheActor.Params)(default: => T): Option[T] = {
      implicit val tm = Timeout(60 seconds)

      val q = (cacheActor ? impl.CacheActor.FindValue(params, () => default)).mapTo[Option[T]]

      allCatch.opt {
        Await.result(q, tm.duration)
      } getOrElse None
    }

    def evict(params: impl.CacheActor.Params) {
      cacheActor ! impl.CacheActor.PurgeValue(params)
    }
  }
}