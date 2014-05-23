package misc

import play.api.{ Application, GlobalSettings }
import play.api.mvc._
import play.filters.gzip.GzipFilter
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Results._

object Global extends WithFilters(LoggingFilter, new GzipFilter( /*shouldGzip = (request, response) => response.headers.get("Content-Type").exists(_.startsWith("text/html"))*/ )) with GlobalSettings {

  override def onStart(app: Application) {}

  override def onHandlerNotFound(request: RequestHeader) = scala.concurrent.Future {
    Redirect(controllers.routes.Application.index)
  }

  override def onStop(app: Application) {}
}
