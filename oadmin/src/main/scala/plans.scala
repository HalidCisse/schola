package schola
package oadmin

import org.clapper.avsl.Logger
// import org.apache.commons.validator.routines.EmailValidator

object Plans extends Plans(Façade)

class Plans(val f: ServiceComponentFactory with HandlerFactory) {

  val log = Logger("oadmin.plans")

  import oauth2._
  import unfiltered.request._
  import unfiltered.filter._
  import unfiltered.filter.request.ContextPath
  import unfiltered.response._

  import scala.util.control.Exception.allCatch

  import unfiltered.filter.request.{MultiPart, MultiPartParams}

  import libs.Flash

  import f.simple._

//  private val xHttpThreadPoolExecutor = {
//    import java.util.concurrent._
//
//    val FUTURE_POOL_SIZE = 8 // TODO: make `FUTURE_POOL_SIZE` a config value
//
//    new ThreadPoolExecutor(FUTURE_POOL_SIZE, FUTURE_POOL_SIZE,
//      1, TimeUnit.MINUTES,
//      new LinkedBlockingQueue(FUTURE_POOL_SIZE))
//  }

  private val xHttp =
    dispatch.Http // configure(_.setExecutorService(xHttpThreadPoolExecutor)) // TODO: configure dispatch-reboot executor service

  system.registerOnTermination {
    log.info("Stopping http client . . . ")
    xHttp.shutdown()
  }

  object Username
    extends Params.Extract("username", Params.first ~> Params.nonempty ~> Params.trimmed/* ~> Params.pred(EmailValidator.getInstance.isValid)*/)

  object Passwd extends Params.Extract("password", Params.first ~> Params.nonempty ~> Params.trimmed)

  object NewPasswd extends Params.Extract("new_password", Params.first ~> Params.nonempty ~> Params.trimmed)

  object SessionKey {
    def unapply[T](req: HttpRequest[T]) =
      Cookies.unapply(req) flatMap (_(SESSION_KEY) flatMap (c=>utils.Crypto.extractSignedToken(c.value)))
  }

  val / = (hostname: String, port: Int) => {

    import dispatch._
    import scala.concurrent.ExecutionContext.Implicits.global // TODO: change this to a real ExecutionContext

    object UserPasswd {
      def unapply(params: Map[String, Seq[String]]) =
        allCatch.opt {
          (Username.unapply(params).get,
            Passwd.unapply(params).get,
            params("remember_me") exists(_ == "remember-me"))
        }
    }
    object Passwds {
      def unapply(params: Map[String, Seq[String]]) =
        allCatch.opt {
          (Passwd.unapply(params).get,
            NewPasswd.unapply(params).get)
        }
    }

    object ReCaptcha {
      val PublicKey = config.getString("recaptcha.public_key")
      
      val PrivateKey = config.getString("recaptcha.private_key")
      
      def unapply(params: Map[String, Seq[String]]) =
        allCatch.opt(
            params("recaptcha_challenge_field")(0),
            params("recaptcha_response_field")(0))
    }

    val localhost = host(hostname, port)

    val token = localhost / "oauth" / "token"
    val api = localhost / "api" / API_VERSION

    val client = domain.OAuthClient(
      "oadmin", "oadmin",
      "http://%s:%d" format(hostname, port)
    )

    // --------------------------------------------------------------------------------------------------------------

    implicit class WithFlash(rf: unfiltered.response.ResponseFunction[javax.servlet.http.HttpServletResponse]) {      
      def flashing(flash: Flash)  =
        SetCookies(Flash.encodeAsCookie(flash)) ~> rf

      def flashing(values: (String, String)*): unfiltered.response.ResponseFunction[javax.servlet.http.HttpServletResponse] = flashing(Flash(values.toMap))
    }


    def Logout[B, C](req: HttpRequest[B] with unfiltered.Async.Responder[C], s: oauthService.SessionLike, userAgent: String) {
      withMac(req, s, "GET", "/api/$API_VERSION/logout", userAgent) {
        auth =>

          for (_ <- xHttp(
            api / "logout" <:< auth
          ).either) {

            req.respond(
              SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/"))
          }
      }
    }

    /*
    *
    *  Get associated session and refresh it if it is expired . . !
    *
    * */
    def withSession[T, B, C](req: HttpRequest[B] with unfiltered.Async.Responder[C], sessionKey: String, userAgent: String)(fn: Either[(unfiltered.response.ResponseFunction[C], oauthService.SessionLike), Option[oauthService.SessionLike]] => T) {
      val session =
        oauthService.getUserSession(
          Map(
            "bearerToken" -> sessionKey,
            "userAgent" -> userAgent)
        )

      def isExpired(s: oauthService.SessionLike) = s.expiresIn exists (s.issuedTime + _ * 1000 < System.currentTimeMillis)

      session match {

        case o@Some(s) =>

          if (isExpired(s)) {

            if (s.refresh.isDefined) {

              for (e <- xHttp(token << Map(
                "grant_type" -> "refresh_token",
                "client_id" -> client.id,
                "client_secret" -> client.secret,
                "refresh_token" -> s.refresh.get
              ) <:< Map("User-Agent" -> userAgent) OK as.json4s.Json).either) {

                e.fold(
                  msg => fn(Right(None)),
                  info => {

                    def findVal(field: String) =
                      info findField {
                        case org.json4s.JField(`field`, _) => true
                        case _ => false
                      } collect {
                        case org.json4s.JField(_, org.json4s.JString(st)) => st
                      }

                    val key = findVal("access_token").get
                    val ss = oauthService.getUserSession(Map("bearerToken" -> key, "userAgent" -> userAgent)).get
                    val rememberMe = Cookies(req).get("_session_rememberMe").isDefined

                    fn(Left(SetCookies(
                      unfiltered.Cookie(
                        SESSION_KEY,
                        utils.Crypto.signToken(ss.key),
                        maxAge = if (rememberMe) ss.refreshExpiresIn map (_.toInt) else None,
                        httpOnly = true
                      )), ss))
                  }
                )
              }
            }

            else Logout(req, s, userAgent)

          }

          else fn(Right(o))

        case _ => fn(Right(None))
      }
    }

    def withMac[T, B, C](req: HttpRequest[B] with unfiltered.Async.Responder[C], session: oauthService.SessionLike, method: String, uri: String, userAgent: String)(f: Map[String, String] => T) {

      def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

      val nonce = _genNonce(session.issuedTime)

      val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, hostname, port, "", "")
      unfiltered.mac.Mac.macHash(MACAlgorithm, session.secret)(normalizedRequest).fold({
        _ => req.respond(SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/"))
      }, {
        mac =>

          val auth = Map(
            "Authorization" -> s"""MAC id="${session.key}",nonce="$nonce",mac="$mac" """,
            "User-Agent" -> userAgent
          )

          f(auth)
      })
    }

    def Login[T](username: String, passwd: String, userAgent: String)(fn: Either[Throwable, oauthService.SessionLike] => T) {

      for (e <- xHttp(token << Map(
        "grant_type" -> "password",
        "client_id" -> client.id,
        "client_secret" -> client.secret,
        "username" -> username,
        "password" -> passwd
      ) <:< Map("User-Agent" -> userAgent) OK as.json4s.Json).either) {

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
            val session = oauthService.getUserSession(Map("bearerToken" -> key, "userAgent" -> userAgent))

            fn(Right(session.get))
          }
        )
      }
    }

    val gCapcha = url("http://www.google.com/recaptcha/api/verify")

    def reCaptchaVerify[T, Rq<:javax.servlet.http.HttpServletRequest, Rp<:javax.servlet.http.HttpServletResponse](req: HttpRequest[Rq] with unfiltered.Async.Responder[Rp], challenge: String, response: String, remoteAddr: String)(onSuccess: => T) {
      val params = Map(
        "challenge" -> challenge,
        "response" -> response,
        "remoteip" -> remoteAddr,
        "privatekey" -> ReCaptcha.PrivateKey)

      def isSuccess(msg: String) = {
        val x = msg.split("""\n""")
        x.length > 0 && x(0) == "true"
      }

      def onError() =
        req.respond(Redirect("/LostPasswd") flashing("error" -> "T", "Msg" -> "Invalid captcha."))

      for(e <- xHttp(
        gCapcha << params OK as.String).either) {

        e.fold(
          _ => onError(),
          msg => if(isSuccess(msg)) onSuccess else onError()
        )
      }
    }

    async.Planify {

      case UserAgent(uA) & req =>

        object LoginP
          extends Params.Extract("login", Params.first ~> Params.nonempty ~> Params.trimmed/* ~> Params.pred(EmailValidator.getInstance.isValid)*/)

        object Key
          extends Params.Extract("key", Params.first ~> Params.nonempty ~> Params.trimmed)

        req match {

          case SessionKey(key) =>

            req match {

              case GET(ContextPath(_, "/session")) & Jsonp.Optional(cb) =>

                withSession(req, key, uA) {
                  case Right(session) =>

                    req.respond(session map {
                      s =>
                        JsonContent ~> ResponseString(
                          cb wrap conversions.json.tojson(session))
                    } getOrElse (SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/")))

                  case Left((rf, session)) =>

                    req.respond(
                      rf ~> JsonContent ~> ResponseString(
                        cb wrap conversions.json.tojson(session)))
                }

              case ContextPath(_, "/") =>

                withSession(req, key, uA) {
                  case Right(session) =>

                    req.respond(session map {
                      s =>
                        if (s.user.changePasswordAtNextLogin) Redirect("/ChangePasswd") flashing("forcePasswdChange" -> "T")
                        else utils.Scalate(req, "index.jade", "session" -> s)
                    } getOrElse (SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/")))

                  case Left((rf, session)) =>

                    req.respond(
                      rf ~> (if (session.user.changePasswordAtNextLogin) Redirect("/ChangePasswd") flashing("forcePasswdChange" -> "T")
                      else utils.Scalate(req, "index.jade", "session" -> session)))
                }

              case GET(ContextPath(_, "/Avatar")) => // TODO: implement Cache & If-Not-Modified

                def getAvatar(user: oauthService.UserLike) =
                  oauthService.getAvatar(user.id map(_.toString) get) match {
                    case Some((domain.AvatarInfo(mimeType, _), data)) =>
                      CharContentType(mimeType) ~> ResponseBytes(com.owtelse.codec.Base64.decode(data))

                    case _ =>
                      CharContentType("image/png") ~> (if(user.gender eq domain.Gender.Male)
                        ResponseBytes(com.owtelse.codec.Base64.decode(DefaultAvatars.M))
                      else ResponseBytes(com.owtelse.codec.Base64.decode(DefaultAvatars.F)))
                  }

                withSession(req, key, uA) {
                  case Right(session) =>

                    req.respond(session map {
                      s => getAvatar(s.user)
                    } getOrElse (SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/")))

                  case Left((rf, session)) =>

                    req.respond(getAvatar(session.user))
                }

              case DELETE(ContextPath(_, "/Avatar")) & Jsonp.Optional(cb) =>
                
                def ERROR(redirect: Boolean = false) = JsonContent ~> ResponseString(cb wrap s"""{"success": false, "redirect": $redirect}""")

                def PurgeAvatar[B<:javax.servlet.http.HttpServletRequest, C<:javax.servlet.http.HttpServletResponse](req: HttpRequest[B] with unfiltered.Async.Responder[C], session: oauthService.SessionLike)(fn: unfiltered.response.ResponseFunction[C]=> unfiltered.response.ResponseFunction[C]) {

                  def SUCCESS = JsonContent ~> ResponseString(cb wrap s"""{"success": true}""")

                  withMac(req, session, "DELETE", s"/api/$API_VERSION/user/${session.user.id.get.toString}/avatars", uA) {
                    auth => 

                    for(e <- xHttp(
                      api.DELETE / "user" / session.user.id.get.toString / "avatars" <:< auth
                      ).either) {

                        req.respond {
                          fn {
                            e.fold(
                              _ => ERROR(),
                              ryt => utils.If(ryt.getStatusCode == 200, SUCCESS, ERROR())
                            )
                          }
                        }
                      }
                  }
                }

                withSession(req, key, uA) {
                  case Right(session) =>

                    session map {
                      s => PurgeAvatar(req, s) { rf => rf }
                    } getOrElse {
                      req.respond {
                        SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> ERROR(redirect = true)
                      }
                    }

                  case Left((rf, session)) =>

                    PurgeAvatar(req, session) {
                      xrf => rf ~> xrf
                    }
                }
                  
              case POST(ContextPath(_, "/Avatar")) & MultiPart(_) & Jsonp.Optional(cb) =>

                def ERROR(redirect: Boolean = false) = JsonContent ~> ResponseString(cb wrap s"""{"success": false, "redirect": $redirect}""")

                def AddAvatar[B<:javax.servlet.http.HttpServletRequest, C<:javax.servlet.http.HttpServletResponse](req: HttpRequest[B] with unfiltered.Async.Responder[C], session: oauthService.SessionLike)(fn: unfiltered.response.ResponseFunction[C]=> unfiltered.response.ResponseFunction[C]) {

                  def SUCCESS = JsonContent ~> ResponseString(cb wrap s"""{"success": true}""")

                  MultiPartParams.Disk(req).files("f") match {
                    case Seq(fp, _*) =>                      

                      fp.write(java.io.File.createTempFile("OAdmin-", s"-avatar_${session.user.id.get}")).fold(req.respond {
                        fn(ERROR())
                      }) {
                        fs =>

                        withMac(req, session, "POST", s"/api/$API_VERSION/user/${session.user.id.get.toString}/avatars", uA) {
                          auth => 

                          for(e <- xHttp(
                            api.POST / "user" / session.user.id.get.toString / "avatars" <:< auth addBodyPart new com.ning.http.multipart.FilePart("f", fs, fp.contentType, "UTF-8")
                            ).either) {

                              req.respond {
                                fn {
                                  e.fold(
                                    _ => ERROR(),
                                    ryt => utils.If(ryt.getStatusCode == 200, SUCCESS, ERROR())
                                  )
                                }
                              }
                            }
                        }
                      }

                    case _ => req.respond(fn(ERROR()))
                  }
                }

                withSession(req, key, uA) {
                  case Right(session) =>

                    session map {
                      s => AddAvatar(req, s) { rf => rf }
                    } getOrElse {
                      req.respond {
                        SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> ERROR(redirect = true)
                      }
                    }

                  case Left((rf, session)) =>

                    AddAvatar(req, session) {
                      xrf => rf ~> xrf
                    }
                }

              case GET(ContextPath(_, "/EditAccount")) =>

                withSession(req, key, uA) {
                  case Right(session) =>

                    req.respond(session map {
                      s =>
                        utils.Scalate(req, "editprofile.jade", "editPrimaryEmail" -> s.hasRole(domain.R.SuperUserR.name), "profile" -> s.user)
                    } getOrElse (SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/")))


                  case Left((rf, session)) =>

                    req.respond(
                      rf ~> utils.Scalate(req, "editprofile.jade", "editPrimaryEmail" -> session.hasRole(domain.R.SuperUserR.name), "profile" -> session.user))
                }

              case POST(ContextPath(_, "/EditAccount")) =>

                import domain.U.Params._

                def EditAccount[B<:javax.servlet.http.HttpServletRequest, C<:javax.servlet.http.HttpServletResponse](req: HttpRequest[B] with unfiltered.Async.Responder[C], session: oauthService.SessionLike)(fn: unfiltered.response.ResponseFunction[C]=> unfiltered.response.ResponseFunction[C]) {
                  import org.json4s._
                  import org.json4s.JsonDSL._
                  import org.json4s.native.JsonMethods._

                  val param = {
                    val mData = Params.unapply(req).get

                    def cleaned(k: Seq[String]) = if(k.isEmpty) None else Some(k(0).trim)

                    (aKey: String) => 
                      cleaned(mData(aKey))
                  }

                  def read(ps: List[(String, String)]) =
                    (JObject() /: ps) {
                      case (js, (p, aKey)) => param(p) map(v => (aKey, v) ~ js) getOrElse js
                    }

                  val dft =
                    (JObject() /: DParams) {
                      case (js, p) => param(p) map(v => if(p.contains("password") && v.isEmpty) js else (p, v) ~ js) getOrElse js
                    }

                  val jsonInfo = 
                    dft ~
                    ("homeAddress" -> read(HomeAddressParams)) ~
                    ("workAddress" -> read(WorkAddressParams)) ~
                    ("contacts" -> (
                      ("work" -> read(WorkContactParams)) ~ 
                        ("home" -> read(HomeContactParams)) ~ 
                          ("mobiles" -> (
                            ("mobile1" -> read(Mobile1ContactParams)) ~ 
                              ("mobile2" -> read(Mobile2ContactParams))))))

                  withMac(req, session, "PUT", s"/api/$API_VERSION/user/${session.user.id.get.toString}", uA) {
                    auth =>

                      for (e <- xHttp(
                          api.PUT / "user" / session.user.id.get.toString << compact(render(jsonInfo)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8")
                        ).either) {

                        req.respond(fn(e.fold(
                          _ => Redirect("/EditAccount") flashing("error" -> "T"),
                          ryt => utils.If(ryt.getStatusCode == 200, Redirect("/") flashing("success" -> "T", "Msg" -> "Account updated."), Redirect("/EditAccount") flashing("error" -> "T"))
                        )))
                      }
                  }
                }

                withSession(req, key, uA) {
                  case Right(Some(session)) =>

                    EditAccount(req, session) {
                      rf => rf
                    }

                  case Left((rf, session)) =>

                    EditAccount(req, session) {
                      xrf => rf ~> xrf
                    }

                  case _ =>

                    req.respond(
                      SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/"))
                }

              case GET(ContextPath(_, "/ChangePasswd")) =>

                req.respond(utils.Scalate(req, "changepasswd.jade"))

              case POST(ContextPath(_, "/ChangePasswd")) & Params(Passwds(passwd, newPasswd)) =>

                def ChangePasswd[T, B<:javax.servlet.http.HttpServletRequest, C<:javax.servlet.http.HttpServletResponse](req: HttpRequest[B] with unfiltered.Async.Responder[C], session: oauthService.SessionLike)(fn: unfiltered.response.ResponseFunction[C]=> unfiltered.response.ResponseFunction[C]) {
                  withMac(req, session, "PUT", s"/api/$API_VERSION/user/${session.user.id.get.toString}", uA) {
                    auth =>

                      import conversions.json.tojson
                      import org.json4s.JsonDSL._

                      for (e <- xHttp(
                        api.PUT / "user" / session.user.id.get.toString << tojson(("old_password" -> passwd) ~ ("password" -> newPasswd)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8")
                      ).either) {

                        req.respond(fn(e.fold(
                          _ => Redirect("/ChangePasswd") flashing("error" -> "T"), // utils.Scalate(req, "changepasswd.jade", "error" -> true),
                          ryt => utils.If(ryt.getStatusCode == 200, Redirect("/") flashing("success" -> "T", "Msg" -> "Password changed."), Redirect("/ChangePasswd") flashing("error" -> "T"))
                        )))
                      }
                  }
                }

                withSession(req, key, uA) {
                  case Right(Some(session)) =>

                    ChangePasswd(req, session) {
                      rf => rf
                    }

                  case Left((rf, session)) =>

                    ChangePasswd(req, session) {
                      xrf => rf ~> xrf
                    }

                  case _ =>

                    req.respond(
                      SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/"))
                }

              case GET(ContextPath(_, "/Logout")) =>

                oauthService.getUserSession(
                  Map(
                    "bearerToken" -> key,
                    "userAgent" -> uA)
                ) match {
                  case Some(s) =>
                    Logout(req, s, uA)

                  case _ =>
                    req.respond(
                      SetCookies.discarding(SESSION_KEY, Flash.COOKIE_NAME) ~> Redirect("/"))
                }

              case _ => req.respond(Redirect("/"))
            }

          /* Requests without being logged in */

          case GET(ContextPath(_, "/Login")) & Cookies(cookies) =>

            req.respond(utils.Scalate(req, "login.jade", "_session_rememberMe" -> cookies("_session_rememberMe").exists(_.value == "remember-me")))

          case POST(ContextPath(_, "/Login")) & Params(UserPasswd(username, passwd, rememberMe)) => // TODO: recherche more on remember-me

            Login(username, passwd, uA) {
              case Left(_) =>
                req.respond(Redirect("/Login") flashing("error" -> "T", "rememberMe" -> (if(rememberMe) "T" else "F")))

              case Right(session) =>

                withMac(req, session, "GET", s"/api/$API_VERSION/session", uA) {
                  auth =>

                    for {
                      e <- xHttp(
                        api / "session" <:< auth
                      ).either
                    } {
                      req.respond(e.fold(
                        _ => Redirect("/Login") flashing("error" -> "T", "rememberMe" -> (if(rememberMe) "T" else "F")), // utils.Scalate(req, "login.jade", "error" -> true, "rememberMe" -> rememberMe),
                        ryt => 
                          utils.If(
                            ryt.getStatusCode == 200, 
                            
                            SetCookies(
                              unfiltered.Cookie(
                                SESSION_KEY,
                                utils.Crypto.signToken(session.key),
                                maxAge = if (rememberMe) session.refreshExpiresIn map (_.toInt) else None,
                                httpOnly = true
                              ), unfiltered.Cookie("_session_rememberMe", "remember-me", maxAge = Some(if (rememberMe) 31536000 /* 1 year */ else 0 /* expire it */), httpOnly = true)
                            ) ~> Flash.discard ~> Redirect("/"),

                            Redirect("/Login") flashing("error" -> "T", "rememberMe" -> (if(rememberMe) "T" else "F"))
                          )
                        )
                      )
                    }
                }
            }

          case GET(ContextPath(_, "/RstPasswd")) & Params(LoginP(login) & Key(ky)) =>

            req.respond {
              if(oauthService.checkActivationReq(login, ky))
                utils.Scalate(req, "rstpasswd.jade", "login" -> login, "key" -> ky)
              else Redirect("/") flashing("error" -> "T", "Msg" -> "Invalid request.")
            }

          case POST(ContextPath(_, "/RstPasswd")) & Params(LoginP(login) & Key(ky) & NewPasswd(newPasswd)) =>

            req.respond {

              if(oauthService.checkActivationReq(login, ky))
                if(oauthService.changePasswd(login, ky, newPasswd)) Redirect("/Login") flashing("success" -> "T", "Msg" -> "Password updated.")
                else Redirect("/RstPasswd") flashing("error" -> "T")

              else Redirect("/RstPasswd") flashing("error" -> "T")
            }

          case GET(ContextPath(_, "/LostPasswd")) =>

            req.respond(utils.Scalate(req, "lostpasswd.jade", "publicKey" -> ReCaptcha.PublicKey))

          case POST(ContextPath(_, "/LostPasswd")) & RemoteAddr(remoteAddr) & Params(ReCaptcha(challenge, response) & Username(username)) =>

            reCaptchaVerify(req, challenge, response, remoteAddr) {

              // Save hashed as user_activation_key
              req.respond {

                try {
                  oauthService.createPasswdResetReq(username)
                  Redirect("/") flashing("success" -> "T", "Msg" -> "Your request has been sent. Please check your email.")
                } catch {
                  case ex: Throwable => Redirect("/LostPasswd") flashing("error" -> "T")
                }
              }
            }

          /* Any request in this context */

          case _ => req.respond(Redirect("/Login"))
        }
    }
  }

  val routes = unfiltered.filter.async.Planify {
    case req =>

      object Name extends Params.Extract("name", Params.first ~> Params.nonempty ~> Params.trimmed)

      object Roles extends Params.Extract("roles", new Params.ParamMapper(f => Some(Set(f: _*))))

      object Permissions extends Params.Extract("permissions", new Params.ParamMapper(f => Some(Set(f: _*))))

      type Intent = async.Plan.Intent

      val routeHandler = f(req)

      val usersIntent: Intent = {

        case GET(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) =>

          routeHandler.downloadAvatar(userId)

        case DELETE(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) =>

          routeHandler.purgeAvatar(userId)

        case POST(ContextPath(_, Seg("user" :: userId :: "avatars" :: Nil))) & MultiPart(_) =>
          val fs = MultiPartParams.Memory(req).files("f")

          fs match {
            case Seq(fp, _*) =>

              routeHandler.uploadAvatar(userId, domain.AvatarInfo(fp.contentType), fp.bytes)

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
