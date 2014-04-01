package ma.epsilon.schola

package impl

trait AppsImpl extends Apps {
  this: AppsRepo =>

  class AppServicesImpl extends AppServices {

    def getApps = appsServiceRepo.getApps

    def addApp(name: String, scopes: Seq[String]) = appsServiceRepo.addApp(name, scopes)

    def removeApp(id: String) = appsServiceRepo.removeApp(id)
  }
}

trait AppsRepoImpl extends AppsRepo {

  import schema._
  import Q._

  protected val db: Database

  protected val appsServiceRepo = new AppServicesRepoImpl

  class AppServicesRepoImpl extends AppServicesRepo {

    import conversions.jdbc.scopeSeqTypeMapper

    private[this] object oq {
      val apps =
        Compiled {
          for {
            (app, accessRight) <- Apps leftJoin AccessRights on (_.id is _.appId)
          } yield (app.id, app.name, app.scopes, accessRight.id?, accessRight.name?, accessRight.scopes?)
        }

      val app = {
        def getApp(id: Column[java.util.UUID]) = Apps where (_.id is id)
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
            accessRights = if (app._4.isDefined) domain.AccessRight(app._5.get, app._1, app._6.get, id = app._4) :: rest.filter(_._4.isDefined).map(o => domain.AccessRight(o._5.get, o._1, o._6.get, id = o._4)) else rest.filter(_._4.isDefined).map(o => domain.AccessRight(o._5.get, o._1, o._6.get, id = o._4)),
            id = Some(app._1)) :: Nil

      }.toList
    }

    def addApp(name: String, scopes: Seq[String]) =
      db.withTransaction {
        implicit session =>
          Apps insert (name, scopes)
      }

    def removeApp(id: String) = db.withTransaction { implicit session => oq.app(uuid(id)).delete }
  }
}