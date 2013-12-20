package schola
package oadmin

import org.clapper.avsl.Logger
import org.apache.commons.validator.routines.EmailValidator

object Plans extends Plans(Façade)

class Plans(val factory: HandlerFactory) {

  val log = Logger("oadmin.plans")

  import oauth2._
  import unfiltered.request._
  import unfiltered.filter.request.ContextPath
  import unfiltered.response._

  import unfiltered.filter.request.{MultiPart, MultiPartParams}

  val routes = unfiltered.filter.async.Planify {
    case req =>

      object Name extends Params.Extract("name", Params.first ~> Params.nonempty ~> Params.trimmed)

      object Email extends Params.Extract("email", Params.first ~> Params.nonempty ~> Params.trimmed ~> Params.pred(EmailValidator.getInstance.isValid))

      object Roles extends Params.Extract("roles[]", new Params.ParamMapper(f => Some(Set(f: _*))))

      object Permissions extends Params.Extract("permissions[]", new Params.ParamMapper(f => Some(Set(f: _*))))

      val routeHandler = factory(req)

      type Intent = unfiltered.filter.async.Plan.Intent

      val usersIntent: Intent = {

        case GET(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) =>

          routeHandler.downloadAvatar(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) =>

          routeHandler.purgeAvatar(userId)

        case POST(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) & MultiPart(_) =>
          val fs = MultiPartParams.Memory(req).files("f")

          fs match {
            case Seq(f, _*) =>

              routeHandler.uploadAvatar(userId, domain.AvatarInfo(f.contentType), f.bytes)

            case _ => BadRequest
          }

        case POST(ContextPath(_, "/users")) =>

          routeHandler.addUser()

        case GET(ContextPath(_, "/users")) =>

          routeHandler.getUsers

        case GET(ContextPath(_, Seg("users" :: "_trash" :: Nil))) =>

          routeHandler.getTrash

        case GET(ContextPath(_, Seg("userexists" :: Nil))) & Params(Email(email)) =>

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

        case PUT(ContextPath(_, Seg("user" :: userId :: "contacts" :: Nil))) =>

          routeHandler.addContacts(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "contacts" :: Nil))) =>

          routeHandler.removeContacts(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "_purge" :: Nil))) =>

          routeHandler.purgeUsers(Set(userId))
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
