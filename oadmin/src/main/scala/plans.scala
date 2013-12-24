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

  private lazy val xHttp = dispatch.Http // TODO: configure dispatch-reboot executor service

  system.registerOnTermination {
    log.info("Stopping http client . . . ")
    xHttp.shutdown()
  }

  object Username
    extends Params.Extract("username", Params.first ~> Params.nonempty ~> Params.trimmed ~> Params.pred(EmailValidator.getInstance.isValid))

  object Passwd extends Params.Extract("password", Params.first ~> Params.nonempty ~> Params.trimmed)

  object NewPasswd extends Params.Extract("new_password", Params.first ~> Params.nonempty ~> Params.trimmed)

  object SessionKey {
    def unapply[T](req: HttpRequest[T]) =
      Cookies.unapply(req) flatMap (_("_session_key")) map (_.value)
  }

  def / = async.Planify {

    case UserAgent(uA) & HostPort(hostname, port) & req =>

      import dispatch._
      import scala.concurrent.ExecutionContext.Implicits.global

      object UserPasswd {
        def unapply(params: Map[String, scala.Seq[String]]) =
          allCatch.opt {
            (Username.unapply(params).get,
              Passwd.unapply(params).get,
              NewPasswd.unapply(params),
              params("remember_me") nonEmpty)
          }
      }

      val localhost = host(hostname, port)

      val token = localhost / "oauth" / "token"
      val api = localhost / "api" / "v1"

      val client = domain.OAuthClient(
        "oadmin", "oadmin",
        localhost.toString
      )

      // --------------------------------------------------------------------------------------------------------------

      /*
      *
      *  Get associated session and refresh it if it is expired . . !
      *
      * */
      def inSession[T, B, C](req: HttpRequest[B] with unfiltered.Async.Responder[C], sessionKey: String, userAgent: String)(fn: Option[Façade.oauthService.SessionLike] => T) {
        val session =
          Façade.oauthService.getUserSession(
            Map(
              "bearerToken" -> sessionKey,
              "userAgent" -> userAgent)
          )

        def isExpired(s: Façade.oauthService.SessionLike) = s.expiresIn exists (s.issuedTime + _ * 1000 < System.currentTimeMillis)

        session match {

          case o@Some(s) =>

            if (isExpired(s)) {

              if(s.refresh.isDefined)

                for (e <- xHttp(token << Map(
                  "grant_type" -> "refresh_token",
                  "client_id" -> client.id,
                  "client_secret" -> client.secret,
                  "refresh_token" -> s.refresh.get
                ) <:< Map("User-Agent" -> userAgent) OK as.json4s.Json).either) {

                  e.fold(
                    _ => fn(None),
                    info => {

                      def findVal(field: String) =
                        info findField {
                          case org.json4s.JField(`field`, _) => true
                          case _ => false
                        } collect {
                          case org.json4s.JField(_, org.json4s.JString(st)) => st
                        }

                      val key = findVal("access_token").get
                      val session = Façade.oauthService.getUserSession(Map("bearerToken" -> key, "userAgent" -> userAgent))

                      fn(session)
                    }
                  )
                }

              else

                withMac(s, "GET", "/api/v1/logout", uA) {
                  auth =>

                    for (_ <- xHttp(
                      api / "logout" <:< auth OK as.String
                    ).either) {

                      req.respond(
                        SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/"))
                    }
                }

            }

            else fn(o)

          case _ => fn(None)
        }
      }

      def withMac[T](session: Façade.oauthService.SessionLike, method: String, uri: String, uA: String)(f: Map[String, String] => T) {

        def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

        val nonce = _genNonce(session.issuedTime)

        val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, hostname, port, "", "")
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

      def withSession[T](username: String, passwd: String, uA: String)(fn: Either[Throwable, Façade.oauthService.SessionLike] => T) {

        for (e <- xHttp(token << Map(
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

      req match {

        case GET(ContextPath(_, "/session")) & SessionKey(key) & Jsonp.Optional(cb) =>

          inSession(req, key, uA) {
            session =>

              req.respond(session map {
                s =>
                  JsonContent ~> ResponseString(
                    cb wrap conversions.json.tojson(session))
              } getOrElse SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/Login"))
          }

        case ContextPath(_, "/") & SessionKey(key) =>

          inSession(req, key, uA) {
            session =>

              req.respond(session map {
                s =>
                  if (s.user.passwordValid) utils.Scalate(req, "index.jade", "session" -> s)
                  else Redirect("/ChangePasswd")
              } getOrElse SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/Login"))
          }

        case GET(ContextPath(_, "/EditProfile")) & SessionKey(key) =>

          inSession(req, key, uA) {
            session =>

              req.respond(session map {
                s =>
                  utils.Scalate(req, "editprofile.jade", "sessionKey" -> key, "profile" -> s.user)
              } getOrElse SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/Login"))
          }

        case POST(ContextPath(_, "/EditProfile")) & SessionKey(key) =>

          req.respond(Redirect("/"))

        case GET(ContextPath(_, "/ChangePasswd")) & SessionKey(key) =>

          req.respond(utils.Scalate(req, "changepasswd.jade", "sessionKey" -> key))

        case POST(ContextPath(_, "/ChangePasswd")) & SessionKey(key) & Params(UserPasswd(_, passwd, Some(newPasswd), _)) =>

          inSession(req, key, uA) {
            case Some(session) =>

              import conversions.json.tojson
              import org.json4s.JsonDSL._

              withMac(session, "PUT", s"/api/v1/user/${session.user.id.get.toString}", uA) {
                auth =>

                  for (e <- xHttp(
                    api / "user" / session.user.id.get.toString << tojson(("old_password" -> passwd) ~ ("password" -> newPasswd)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8")
                  ).either) {

                    req.respond(e.fold(
                      _ => utils.Scalate(req, "changepasswd.jade", "error" -> true),
                      _ => Redirect("/")
                    ))
                  }
              }

            case _ =>

              req.respond(
                SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/"))
          }

        case GET(ContextPath(_, "/Logout")) & SessionKey(key) =>

          inSession(req, key, uA) {
            case Some(session) =>

              withMac(session, "GET", "/api/v1/logout", uA) {
                auth =>

                  for (_ <- xHttp(
                    api / "logout" <:< auth OK as.String
                  ).either) {

                    req.respond(
                      SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/"))
                  }
              }

            case _ =>

              req.respond(
                SetCookies(unfiltered.Cookie("_session_key", "", maxAge = Some(0))) ~> Redirect("/"))
          }

        case SessionKey(_) =>

          req.respond(Redirect("/"))

        case GET(ContextPath(_, "/Login")) & Cookies(cookies) =>

          req.respond(utils.Scalate(req, "login.jade", "rememberMe" -> cookies("_session_rememberMe").isDefined))

        case POST(ContextPath(_, "/Login")) & Params(UserPasswd(username, passwd, _, rememberMe)) => // TODO: recherche more on remember-me

          withSession(username, passwd, uA) {
            case Left(_) =>
              req.respond(utils.Scalate(req, "login.jade", "error" -> true, "rememberMe" -> rememberMe))

            case Right(session) =>

              withMac(session, "GET", "/api/v1/session", uA) {
                auth =>

                  for {
                    e <- xHttp(
                      api / "session" <:< auth
                    ).either
                  } {
                    req.respond(e.fold(
                      _ => utils.Scalate(req, "login.jade", "error" -> true, "rememberMe" -> rememberMe),
                      _ =>
                        SetCookies(
                          unfiltered.Cookie(
                            "_session_key",
                            session.key,
                            maxAge = if (rememberMe) session.refreshExpiresIn map (_.toInt) else None,
                            httpOnly = true
                          ), unfiltered.Cookie("_session_rememberMe", "remember-me", maxAge = if (rememberMe) session.refreshExpiresIn map (_.toInt) else Some(0))
                        ) ~> (if (session.user.passwordValid) Redirect("/") else Redirect("/ChangePasswd"))

                    ))
                  }
              }
          }

        case _ => req.respond(Redirect("/Login"))
      }
  }

  val routes = unfiltered.filter.async.Planify {
    case req =>

      object Name extends Params.Extract("name", Params.first ~> Params.nonempty ~> Params.trimmed)

      object Roles extends Params.Extract("roles", new Params.ParamMapper(f => Some(Set(f: _*))))

      object Permissions extends Params.Extract("permissions", new Params.ParamMapper(f => Some(Set(f: _*))))

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

        case GET(ContextPath(_, Seg("userexists" :: Nil))) & Params(Username(email)) =>

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
