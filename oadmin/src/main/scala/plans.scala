package schola
package oadmin

import org.clapper.avsl.Logger
// import org.apache.commons.validator.routines.EmailValidator

trait Plans {
  val f: ServiceComponentFactory with HandlerFactory
}

object Plans extends Plans with Server with clients.Root {
  val f = Façade
}

trait Server { self: Plans =>

  private val log = Logger("oadmin.server")

  import oauth2._
  import unfiltered.request._
  import unfiltered.filter._
  import unfiltered.filter.request.ContextPath
  import unfiltered.response._

  import unfiltered.filter.request.{ MultiPart, MultiPartParams }

  object Login
    extends Params.Extract("username", Params.first ~> Params.nonempty ~> Params.trimmed /* ~> Params.pred(EmailValidator.getInstance.isValid)*/ )

  object Page {
    import scala.util.control.Exception.allCatch

    def unapply(params: Map[String, Seq[String]]) =
      allCatch.opt { params("page")(0).toInt } orElse Some(0)
  }

  val api = async.Planify {

    case req =>

      object Name
        extends Params.Extract("name", Params.first ~> Params.nonempty ~> Params.trimmed)

      object Roles
        extends Params.Extract("roles", new Params.ParamMapper(f => Some(Set(f: _*))))

      object Permissions
        extends Params.Extract("permissions", new Params.ParamMapper(f => Some(Set(f: _*))))

      type Intent = async.Plan.Intent

      val routeHandler = f(req)

      val usersIntent: Intent = {

        case GET(ContextPath(_, Seg("avatar" :: avatarId :: Nil))) =>

          routeHandler.downloadAvatar(avatarId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "avatars" :: avatarId :: Nil))) =>

          routeHandler.purgeAvatar(userId, avatarId)

        case POST(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) & MultiPart(_) =>
          val fs = MultiPartParams.Memory(req).files("f")

          fs match {
            case Seq(fp, _*) =>

              routeHandler.uploadAvatar(userId, fp.name, Some(fp.contentType), fp.bytes)

            case _ => BadRequest
          }

        case POST(ContextPath(_, "/users")) =>

          routeHandler.addUser()

        case GET(ContextPath(_, "/users")) & Params(Page(num)) =>

          routeHandler.getUsers(num)

        case GET(ContextPath(_, Seg("users" :: "_trash" :: Nil))) =>

          routeHandler.getTrash

        case GET(ContextPath(_, Seg("userexists" :: Nil))) & Params(Login(email)) =>

          routeHandler.userExists(email)

        case GET(ContextPath(_, "/session")) =>

          routeHandler.getUserSession

        case GET(ContextPath(_, Seg("user" :: userId :: Nil))) =>

          routeHandler.getUser(userId)

        case PUT(ContextPath(_, Seg("user" :: userId :: Nil))) =>

          routeHandler.updateUser(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: Nil))) =>

          routeHandler.removeUser(userId)

        case GET(ContextPath(_, Seg("user" :: userId :: "roles" :: Nil))) =>

          routeHandler.getUserRoles(userId)

        case PUT(ContextPath(_, Seg("user" :: userId :: "roles" :: Nil))) & Params(Roles(roles)) =>

          routeHandler.grantRoles(userId, roles)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "roles" :: Nil))) & Params(Roles(roles)) =>

          routeHandler.revokeRoles(userId, roles)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "_purge" :: Nil))) =>

          routeHandler.purgeUsers(Set(userId))

        case POST(ContextPath(_, Seg("user" :: userId :: "_undelete" :: Nil))) =>

          routeHandler.undeleteUsers(Set(userId))
      }

      val rolesIntent: Intent = {
        case POST(ContextPath(_, "/roles")) =>

          routeHandler.addRole()

        case GET(ContextPath(_, Seg("roleexists" :: Nil))) & Params(Name(name)) =>

          routeHandler.roleExists(name)

        case PUT(ContextPath(_, Seg("role" :: name :: Nil))) =>

          routeHandler.updateRole(name)

        case GET(ContextPath(_, "/permissions")) =>

          routeHandler.getPermissions

        case GET(ContextPath(_, Seg("role" :: name :: "permissions" :: Nil))) =>

          routeHandler.getRolePermissions(name)

        case PUT(ContextPath(_, Seg("role" :: "_" :: "permissions" :: Nil))) & Params(Name(name) & Permissions(permissions)) =>

          routeHandler.grantPermissions(name, permissions)

        case DELETE(ContextPath(_, Seg("role" :: "_" :: "permissions" :: Nil))) & Params(Name(name) & Permissions(permissions)) =>

          routeHandler.revokePermissions(name, permissions)

        case GET(ContextPath(_, "/roles")) =>

          routeHandler.getRoles

        case GET(ContextPath(_, Seg("role" :: name :: Nil))) =>

          routeHandler.getRole(name)

        case DELETE(ContextPath(_, Seg("role" :: name :: Nil))) =>

          routeHandler.purgeRoles(Set(name))
      }

      val authIntent: Intent = {

        case ContextPath(_, "/logout") & Token(token) =>

          routeHandler.logout(token)
      }

      val app = usersIntent orElse rolesIntent orElse authIntent
      app.lift(req) getOrElse Pass
  }
}
