package misc

import play.api.mvc._
import play.api.Play.current

import schola.oadmin._, domain._, http.Façade
import com.typesafe.plugin._

object Global extends GlobalSettings with controllers.ExecutionSystem {

  var cleanUp: Option[() => Unit] = None

  override def onStart(app: Application) {
     Logger.info("Starting application . . .")

    try use[Façade].drop() catch {
      case _: Throwable => Logger.info("DROP failed:")
    }

    use[Façade].init(U.SuperUser.id.get)

    cleanUp = use[Façade].genFixtures   
  }

  override def onStop(app: Application) = {
    Logger.info("Application clean up . . .")
    cleanUp.foreach(_())
    Cache.clearAll()
  }
}
