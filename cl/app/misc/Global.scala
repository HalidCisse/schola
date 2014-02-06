package misc

import play.api._
import play.api.mvc._
import play.api.mvc.Results._


object Global extends GlobalSettings with controllers.ExecutionSystem {

  override def onStart(app: Application) {
  }

  override def onHandlerNotFound(request: RequestHeader) = scala.concurrent.Future{
    Redirect(controllers.routes.Application.index)
  }

  override def onStop(app: Application) = {
  }

}
