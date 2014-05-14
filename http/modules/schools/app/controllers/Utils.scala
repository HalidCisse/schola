package controllers.school

import play.api.mvc._
import play.api.Routes

object Utils extends Controller {

  def javascriptRoutes = Action {
    implicit request =>
      Ok(
        Routes.javascriptRouter("schoolsRoutes")()).as(JAVASCRIPT)
  }
}
