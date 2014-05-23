package misc

import play.api.{ GlobalSettings, Application }
import play.api.mvc.WithFilters
import play.api.Play.current

import ma.epsilon.schola._
import com.typesafe.plugin._

object Global extends WithFilters(LoggingFilter, PagingFilter) with GlobalSettings {

  private val log = Logger("http.Global")

  override def onStart(app: Application) {
    log.info("Starting application . . .")
  }

  override def onStop(app: Application) {
    log.info("Application clean up . . .")
    // Cache.clearAll()
  }
}
