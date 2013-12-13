package schola
package oadmin

import org.clapper.avsl.Logger

object Plans extends oadmin.Plans(Façade)

class Plans(val factory: HandlerFactory) {

  val log = Logger("oadmin.plans")

  import oauth2._
  import unfiltered.request._
  import unfiltered.filter.request.ContextPath
  import unfiltered.response._

  import scala.util.control.Exception.allCatch

  import unfiltered.filter.request.{MultiPart, MultiPartParams}

  val routes = unfiltered.filter.Planify {
    case req =>

      val routeHandler = factory(req)

      type Intent = unfiltered.filter.Plan.Intent

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

        case GET(ContextPath(_, Seg("users" :: "trash" :: Nil))) =>

          routeHandler.getTrash

        case GET(ContextPath(_, Seg("userexists" :: Nil))) & Params(params) =>

          allCatch.opt {
            params("email")(0)
          } match {
            case Some(email) => routeHandler.userExists(email)
            case _ => BadRequest
          }

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

        case PUT(ContextPath(_, Seg("user" :: userId :: "roles" :: Nil))) & Params(p) =>

          routeHandler.grantRoles(userId, p("roles[]").toSet)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "roles" :: Nil))) & Params(p) =>

          routeHandler.revokeRoles(userId, p("roles[]").toSet)

        case PUT(ContextPath(_, Seg("user" :: userId :: "contacts" :: Nil))) =>

          routeHandler.addContacts(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "contacts" :: Nil))) =>

          routeHandler.removeContacts(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "purge" :: Nil))) =>

          routeHandler.purgeUsers(Set(userId))
      }

      val rolesIntent: Intent = {
        case POST(ContextPath(_, "/roles")) =>

          routeHandler.addRole()

        case GET(ContextPath(_, Seg("roleexists" :: Nil))) & Params(params) =>

          allCatch.opt {
            params("name")(0)
          } match {
            case Some(name) => routeHandler.roleExists(name)
            case _ => BadRequest
          }

        case PUT(ContextPath(_, Seg("role" :: name :: Nil))) =>

          routeHandler.updateRole(name)

        case GET(ContextPath(_, "/permissions")) =>

          routeHandler.getPermissions

        case GET(ContextPath(_, Seg("role" :: name :: "permissions" :: Nil))) =>

          routeHandler.getRolePermissions(name)

        case PUT(ContextPath(_, Seg("role" :: name :: "permissions[]" :: Nil))) & Params(p) =>

          routeHandler.grantPermissions(name, p("permissions").toSet)

        case DELETE(ContextPath(_, Seg("role" :: name :: "permissions[]" :: Nil))) & Params(p) =>

          routeHandler.revokePermissions(name, p("permissions").toSet)

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
      app(req)
  }
}