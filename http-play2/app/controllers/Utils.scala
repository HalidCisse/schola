package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{ Json, Writes }
import play.api.Routes

import schola.oadmin._, http.Façade, conversions.json._

import com.typesafe.plugin._

trait Helpers {
  this: Controller =>

  @inline def json[C: Writes](content: Any) = Ok(Json.toJson(content.asInstanceOf[C]))

  @inline implicit def set[X](list: List[X]) = list.toSet
}

object Utils extends Controller with Helpers {

  def getSessionInfo(token: String) =
    Action.async {
      implicit request: RequestHeader =>

        val params = Map(
          /* "userId" -> resourceOwner.id, // -- NOT USED YET */
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

  def javascriptRoutes = Action {
    implicit request =>
      Ok(
        Routes.javascriptRouter("jsRoutes")(
          routes.javascript.Utils.getSessionInfo,

          routes.javascript.Users.getUsers,
          routes.javascript.Users.getUser,
          routes.javascript.Users.addUser,
          routes.javascript.Users.updateUser,
          routes.javascript.Users.deleteUsers,
          routes.javascript.Users.purgeUsers,
          routes.javascript.Users.getUsersStats,
          routes.javascript.Users.getPurgedUsers,
          routes.javascript.Users.undeleteUsers,
          routes.javascript.Users.userExists,
          routes.javascript.Users.getUserRoles,
          routes.javascript.Users.grantUserRoles,
          routes.javascript.Users.revokeUserRoles,
          routes.javascript.Users.getUserTags,
          routes.javascript.Users.addUserTags,
          routes.javascript.Users.purgeUserTags,
          routes.javascript.Users.downloadAvatar,
          routes.javascript.Users.uploadAvatar,
          routes.javascript.Users.purgeAvatar,
          routes.javascript.Users.checkActivationReq,
          routes.javascript.Users.lostPasswd,
          routes.javascript.Users.resetPasswd,
          routes.javascript.Users.logout,

          routes.javascript.Roles.getRoles,
          routes.javascript.Roles.getRole,
          routes.javascript.Roles.addRole,
          routes.javascript.Roles.updateRole,
          routes.javascript.Roles.purgeRoles,
          routes.javascript.Roles.getPermissions,
          routes.javascript.Roles.getRolePermissions,
          routes.javascript.Roles.grantPermissions,
          routes.javascript.Roles.roleExists,

          routes.javascript.Tags.getTags,
          routes.javascript.Tags.addTag,
          routes.javascript.Tags.updateTag,
          routes.javascript.Tags.updateTagColor,
          routes.javascript.Tags.purgeTags)).as(JAVASCRIPT)
  }
}