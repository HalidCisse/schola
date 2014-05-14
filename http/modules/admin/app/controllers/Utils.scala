package controllers.admin

import play.api.mvc._
import play.api.Routes

object Utils extends Controller {

  def javascriptRoutes = Action {
    implicit request =>
      Ok(
        Routes.javascriptRouter("adminRoutes")(
          routes.javascript.Users.getUsers,
          routes.javascript.Users.getUser,
          routes.javascript.Users.getUserByCIN,
          routes.javascript.Users.addUser,
          routes.javascript.Users.updateUser,
          routes.javascript.Users.deleteUsers,
          routes.javascript.Users.purgeUsers,
          routes.javascript.Users.getUsersStats,
          routes.javascript.Users.getPurgedUsers,
          routes.javascript.Users.undeleteUsers,
          routes.javascript.Users.userExists,

          //          routes.javascript.Users.getUserRoles,
          //          routes.javascript.Users.grantUserRoles,
          //          routes.javascript.Users.revokeUserRoles,

          routes.javascript.Users.getUserTags,
          routes.javascript.Users.addUserTags,
          routes.javascript.Users.purgeUserTags,

          routes.javascript.Users.downloadAvatar,
          routes.javascript.Users.uploadAvatar,
          routes.javascript.Users.purgeAvatar,

          routes.javascript.Users.checkActivationReq,
          routes.javascript.Users.lostPasswd,
          routes.javascript.Users.resetPasswd)).as(JAVASCRIPT)

    //          routes.javascript.Roles.getRoles,
    //          routes.javascript.Roles.getRole,
    //          routes.javascript.Roles.addRole,
    //          routes.javascript.Roles.updateRole,
    //          routes.javascript.Roles.purgeRoles,
    //          routes.javascript.Roles.getPermissions,
    //          routes.javascript.Roles.getRolePermissions,
    //          routes.javascript.Roles.grantPermissions,
    //          routes.javascript.Roles.revokePermissions,
    //          routes.javascript.Roles.roleExists)).as(JAVASCRIPT)
  }
}