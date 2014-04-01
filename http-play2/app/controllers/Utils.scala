package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.Routes

import ma.epsilon.schola._, http.{ Façade, HttpHelpers }, utils.tryo, conversions.json._

import com.typesafe.plugin._

object Utils extends Controller with HttpHelpers {

  def getSessionInfo(token: String) =
    Action.async {
      implicit request =>

        val params = Map(
          "bearerToken" -> token,
          "userAgent" -> request.headers.get("User-Agent").getOrElse(""))

        scala.concurrent.Future {

          render {
            case Accepts.Json() =>

              use[Façade].oauthService.getUserSession(params) match {
                case Some(session) => json[domain.Session](session)
                case _             => NotFound
              }
          }
        }
    }

  def getApps = Action.async {
    implicit request =>

      scala.concurrent.Future {

        render {
          case Accepts.Json() => json[List[domain.App]](use[Façade].appService.getApps)
        }
      }
  }

  def logout(token: String) =
    Action {

      tryo { use[Façade].oauthService.revokeToken(token) }

      Ok
    }

  def javascriptRoutes = Action {
    implicit request =>
      Ok(
        Routes.javascriptRouter("jsRoutes")(
          routes.javascript.Utils.getSessionInfo,
          routes.javascript.Utils.getApps,

          routes.javascript.Tags.getTags,
          routes.javascript.Tags.addTag,
          routes.javascript.Tags.updateTag,
          routes.javascript.Tags.updateTagColor,
          routes.javascript.Tags.purgeTags)).as(JAVASCRIPT)
  }
}