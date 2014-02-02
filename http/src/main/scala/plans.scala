package schola
package oadmin

import org.clapper.avsl.Logger
// import org.apache.commons.validator.routines.EmailValidator

trait HandlerFactory extends (unfiltered.request.HttpRequest[_ <: javax.servlet.http.HttpServletRequest] => RouteHandler)

trait Plans {
  val f: ServiceComponentFactory with HandlerFactory
}

trait Server {

  self: Plans =>

  private val log = Logger("oadmin.server")

  import oauth2._
  import unfiltered.request._
  import unfiltered.filter._
  import unfiltered.filter.request.ContextPath
  import unfiltered.response._

  import unfiltered.filter.request.{ MultiPart, MultiPartParams }

  object Login
    extends Params.Extract("username", Params.first ~> Params.nonempty ~> Params.trimmed /* ~> Params.pred(EmailValidator.getInstance.isValid)*/ )

  private object NewPasswd extends Params.Extract("new_password", Params.first ~> Params.nonempty ~> Params.trimmed)

  object Key
    extends Params.Extract("key", Params.first ~> Params.nonempty ~> Params.trimmed)

  object Page {
    import scala.util.control.Exception.allCatch

    def unapply(params: Map[String, Seq[String]]) =
      allCatch.opt { params("page")(0).toInt } orElse Some(0)
  }

  val session = Planify {

    case req =>

      val routeHandler = f(req)

      req match {

        case GET(ContextPath(_, "/session")) =>

          routeHandler.getUserSession

        case _ => Pass
      }
  }

  val password = Planify {

    case req =>

      val routeHandler = f(req)

      req match {

        case GET(ContextPath(_, Seg("users" :: "check_activation_req" :: Nil))) & Params(Login(username) & Key(ky)) =>

            routeHandler.checkActivationReq(username, ky)      

        case POST(ContextPath(_, Seg("users" :: "lostpassword" :: Nil))) & Params(Login(username)) =>

          routeHandler.createPasswdResetReq(username)

        case POST(ContextPath(_, Seg("users" :: "resetpassword" :: Nil))) & Params(Login(username) & Key(ky) & NewPasswd(newPasswd)) =>

          routeHandler.resetPasswd(username, ky, newPasswd)

        case _ => Pass
      }
  }

  val api = async.Planify {

    case req =>

      object Name
        extends Params.Extract("name", Params.first ~> Params.nonempty ~> Params.trimmed)

      object Roles
        extends Params.Extract("roles", new Params.ParamMapper(f => Some(Set(f: _*))))

      object Permissions
        extends Params.Extract("permissions", new Params.ParamMapper(f => Some(Set(f: _*))))

      val routeHandler = f(req)

      req match {

        // Users API

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

        case GET(ContextPath(_, Seg("users" :: "stats" :: Nil))) =>

          routeHandler.getUsersStats

        case GET(ContextPath(_, Seg("users" :: "_trash" :: Nil))) =>

          routeHandler.getTrash

        case GET(ContextPath(_, Seg("userexists" :: Nil))) & Params(Login(email)) =>

          routeHandler.userExists(email)

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

        // Roles API

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

        // Security API

        case ContextPath(_, "/logout") & Token(token) =>

          routeHandler.logout(token)

        // Nothing

        case _ => req.respond { Pass }
      }
  }
}

trait RouteHandler extends Any {

  // -------------------------------------------------------------------------------------------------

  def downloadAvatar(avatarId: String)

  def purgeAvatar(userId: String, avatarId: String)

  def uploadAvatar(userId: String, filename: String, contentType: Option[String], bytes: Array[Byte])

  // -------------------------------------------------------------------------------------------------

  def addUser()

  def updateUser(userId: String)

  def removeUser(userId: String)

  def getUser(userId: String)

  def getUserSession: unfiltered.response.ResponseFunction[Any]

  def getUsersStats

  def getUsers(page: Int)

  def resetPasswd(username: String, activationKey: String, newPasswd: String): unfiltered.response.ResponseFunction[Any]

  def checkActivationReq(username: String, key: String): unfiltered.response.ResponseFunction[Any]
  
  def createPasswdResetReq(username: String): unfiltered.response.ResponseFunction[Any]

  def getTrash

  def purgeUsers(id: Set[String])

  def undeleteUsers(id: Set[String])

  def grantRoles(userId: String, roles: Set[String])

  def revokeRoles(userId: String, roles: Set[String])

  def getUserRoles(userId: String)

  def userExists(primaryEmail: String)

  // ---------------------------------------------------------------------------------------------------

  def getRoles

  def addRole()

  def roleExists(name: String)

  def updateRole(name: String)

  def getRole(name: String)

  def purgeRoles(roles: Set[String])

  def grantPermissions(name: String, permissions: Set[String])

  def revokePermissions(name: String, permissions: Set[String])

  def getPermissions

  def getRolePermissions(name: String)

  def logout(token: String)
}