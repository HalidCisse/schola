package schola
package oadmin

import org.clapper.avsl.Logger
import org.apache.commons.validator.routines.EmailValidator

object Plans extends Plans(Façade)

class Plans(val factory: HandlerFactory) {

  val log = Logger("oadmin.plans")

  import oauth2._
  import unfiltered.request._
  import unfiltered.filter._
  import unfiltered.filter.request.ContextPath
  import unfiltered.response._

  import scala.util.control.Exception.allCatch

  import unfiltered.filter.request.{MultiPart, MultiPartParams}

  private lazy val xHttp = dispatch.Http // TODO: configure executor service

  system.registerOnTermination {
    log.info("Stopping http client . . . ")
    xHttp.shutdown()
  }

  object Email
    extends Params.Extract("email", Params.first ~> Params.nonempty ~> Params.trimmed ~> Params.pred(EmailValidator.getInstance.isValid))

  object Passwd extends Params.Extract("password", Params.first ~> Params.nonempty ~> Params.trimmed)

  def /(protectionPlan: Plan) = async.Planify {

    case req =>

      def isAuthorized = protectionPlan.intent.isDefinedAt(req)

      object UserPasswd {
        def unapply(params: Map[String, scala.Seq[String]]) =
          allCatch.opt { (Email.unapply(params).get, Passwd.unapply(params).get) }
      }

      req match {

        case ContextPath(_, "/") & UserAgent(uA) if isAuthorized =>

          val session =
            Façade.oauthService.getUserSession(
              Map(
                "bearerToken" -> oauth2.Token.unapply(req).get,
                "userAgent" -> uA)
            ).get

          req.respond(
            if(session.user.passwordValid) utils.Scalate(req, "index.jade", "session" -> session)
            else utils.Scalate(req, "changepasswd.jade", "session" -> session)
          )

        case GET(ContextPath(_, "/")) =>

          req.respond(utils.Scalate(req, "login.jade"))


        case POST(ContextPath(_, "/")) & UserAgent(uA) & Params(UserPasswd(username, passwd)) =>

          import dispatch._
          import scala.concurrent.ExecutionContext.Implicits.global

          val localhost = host("localhost", 3000)

          val token = localhost / "oauth" / "token"
          val api = localhost / "api" / "v1"

          val client = domain.OAuthClient(
            "oadmin", "oadmin",
            localhost.toString
          )

          // --------------------------------------------------------------------------------------------------------------

          def withMac[T](session: Façade.oauthService.SessionLike, method: String, uri: String)(f: Map[String, String] => T) {

            def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

            val nonce = _genNonce(session.issuedTime)

            val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, "localhost", 3000, "", "")
            unfiltered.mac.Mac.macHash(MACAlgorithm, session.secret)(normalizedRequest).fold({
              _ => req.respond(utils.Scalate(req, "login.jade", "error" -> true))
            }, {
              mac =>

                val auth = Map(
                  "Authorization" -> s"""MAC id="${session.key}",nonce="$nonce",mac="$mac" """,
                  "User-Agent" -> uA
                )

                f(auth)
            })
          }

          def withSession[T](username: String, passwd: String)(fn: Either[Throwable, Façade.oauthService.SessionLike] => T) {

            for(e <- xHttp(token << Map(
              "grant_type" -> "password",
              "client_id" -> client.id,
              "client_secret" -> client.secret,
              "username" -> username,
              "password" -> passwd
            ) <:< Map("User-Agent" -> uA) OK as.json4s.Json).either) {

              e.fold(
                res => fn(Left(res)),
                info => {

                  def findVal(field: String) =
                    info findField {
                      case org.json4s.JField(`field`, _) => true
                      case _ => false
                    } collect {
                      case org.json4s.JField(_, org.json4s.JString(s)) => s
                    }

                  val key = findVal("access_token").get
                  val session = Façade.oauthService.getUserSession(Map("bearerToken" -> key, "userAgent" -> uA))

                  fn(Right(session.get))
                }
              )
            }
          }

          withSession(username, passwd) {
            case Left(e) =>
              req.respond(utils.Scalate(req, "login.jade", "error" -> true))

            case Right(session) =>

              withMac(session, "GET", "/api/v1/session") { auth =>

                for {
                  e <- xHttp(
                    api / "session" << auth OK as.String
                  ).either
                } {
                  req.respond(e.fold(
                    _ => utils.Scalate(req, "login.jade", "error" -> true),
                    _ =>
                      if(session.user.passwordValid) utils.Scalate(req, "index.jade", "session" -> session)
                      else utils.Scalate(req, "changepasswd.jade", "session" -> session)
                  ))
                }
              }
          }

        case _ => Pass
      }
  }

  val routes = unfiltered.filter.async.Planify {
    case req =>

      object Name extends Params.Extract("name", Params.first ~> Params.nonempty ~> Params.trimmed)

      object Roles extends Params.Extract("roles[]", new Params.ParamMapper(f => Some(Set(f: _*))))

      object Permissions extends Params.Extract("permissions[]", new Params.ParamMapper(f => Some(Set(f: _*))))

      type Intent = async.Plan.Intent

      val routeHandler = factory(req)

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
