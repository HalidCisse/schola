package ma.epsilon.schola

package impl

trait AppsImpl extends Apps {
  this: AppsRepo =>

  class AppServicesImpl extends AppServices {

    def getApps = appsServiceRepo.getApps

    def addApp(name: String, scopes: List[String]) = appsServiceRepo.addApp(name, scopes)

    def removeApp(id: Uuid) = appsServiceRepo.removeApp(id)
  }
}

trait AppsRepoImpl extends AppsRepo {
  this: jdbc.WithDatabase =>

  import schema._
  import jdbc.Q._

  protected val appsServiceRepo = new AppServicesRepoImpl

  class AppServicesRepoImpl extends AppServicesRepo {

    import conversions.jdbc.scopesTypeMapper

    private[this] object oq {
      val apps =
        Compiled {
          for {
            (app, accessRight) <- Apps leftJoin AccessRights on (_.id === _.appId)
          } yield (
            app.id,
            app.name,
            app.scopes,
            accessRight.id?,
            accessRight.alias?,
            accessRight.displayName?,
            accessRight.redirectUri?,
            accessRight.scopes?,
            accessRight.grantOptions?)
        }

      val app = {
        def getApp(id: Column[Uuid]) = Apps filter (_.id === id)
        Compiled(getApp _)
      }
    }

    def getApps = {
      val apps = oq.apps

      val result = db.withSession {
        implicit session => apps.list
      }

      result.groupBy(_._1).flatMap {
        case (id, app :: rest) =>

          domain.App(
            app._2,
            app._3,
            accessRights =
              if (app._4.isDefined)
                domain.AccessRight(
                app._5.get,
                app._6.get,
                app._7.get,
                app._1,
                app._8.get,
                app._9.get,
                id = app._4) :: rest.filter(_._4.isDefined).map(o => domain.AccessRight(o._5.get, o._6.get, o._7.get, o._1, o._8.get, o._9.get, id = o._4))
              else rest.filter(_._4.isDefined).map(o => domain.AccessRight(o._5.get, o._6.get, o._7.get, o._1, o._8.get, o._9.get, id = o._4)),
            id = Some(app._1)) :: Nil

      }.toList
    }

    def addApp(name: String, scopes: List[String]) =
      db.withTransaction {
        implicit session =>
          Apps insert (name, scopes)
      }

    def removeApp(id: Uuid) = db.withTransaction { implicit session => oq.app(id).delete }
  }
}