package misc

import play.api.{ GlobalSettings, Application }
import play.api.Play.current

import schola.oadmin._
import com.typesafe.plugin._

object Global extends GlobalSettings {

  private val log = Logger("misc.Global")

  override def onStart(app: Application) {
    log.info("Starting application . . .")
  }

  override def onStop(app: Application) = {
    log.info("Application clean up . . .")
    // Cache.clearAll()
  }
}
