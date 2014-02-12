package schola
package oadmin
package cl

import play.api.{ Plugin, Logger, Application }

import play.api.libs.json.{ JsValue, Json => PlayJson }

class SessionSupport(app: Application) extends Plugin with controllers.ExecutionSystem {

  import scala.concurrent.Future

  import conversions.json._, domain._

  import dispatch._

  object Json extends (com.ning.http.client.Response => JsValue) {
    def apply(r: com.ning.http.client.Response) =
      (dispatch.as.String andThen (s => PlayJson.parse(s)))(r)
  }

  override def onStart() {
    Logger.info("[oadmin-cl] loaded session plugin: %s".format(getClass.getName))
  }

  val hostname = "localhost"

  val client = OAuthClient(
    "oadmin", "oadmin",
    f"http://$hostname")

  private val apiHost = host(hostname)

  private val token = apiHost / "oauth" / "token"
  private val api = apiHost / "api" / API_VERSION
  private val session = api / "session"

  def session(sessionKey: String, userAgent: String): Future[Session] =
    Http(session <<? Map("token" -> sessionKey) <:< Map("User-Agent" -> userAgent) OK Json) flatMap {
      json =>

        json.asOpt[Session].fold[Future[Session]](Future.failed(new ScholaException(s"Invalid session [$sessionKey]"))) {
          s =>

            def isExpired(s: Session) = s.expiresIn exists (s.issuedTime + _ * 1000 < System.currentTimeMillis)

            if (isExpired(s)) {

              if (s.refresh.isDefined) {

                Http(token << Map(
                  "grant_type" -> "refresh_token",
                  "client_id" -> client.id,
                  "client_secret" -> client.secret,
                  "refresh_token" -> s.refresh.get) <:< Map("User-Agent" -> userAgent) OK Json) flatMap { info =>

                  (info \ "access_token")
                    .asOpt[String]
                    .fold[Future[String]](Future.failed(new ScholaException(s"Could not refresh token [$sessionKey]"))) { accessToken => Future.successful(accessToken) }

                } flatMap { accessToken =>
                  session(accessToken, userAgent)
                }

              } else Future.failed(new ScholaException(s"No refresh token for expired session [$sessionKey]"))

            } else Future.successful(s)
        }

    }

  def login(username: String, passwd: String, userAgent: String) =
    Http(token << Map(
      "grant_type" -> "password",
      "client_id" -> client.id,
      "client_secret" -> client.secret,
      "username" -> username,
      "password" -> passwd) <:< Map("User-Agent" -> userAgent) OK Json) flatMap { info =>

      (info \ "access_token")
        .asOpt[String]
        .fold[Future[String]](Future.failed(new ScholaException("Login failed"))) { sessionKey => Future.successful(sessionKey) }

    } flatMap { accessToken =>
      session(accessToken, userAgent)
    }

  def logout(sessionKey: String) =
    for {
      e <- Http(
        api / "logout" <<? Map("token" -> sessionKey)).either
    } yield {

      e.fold(
        _ => false,
        ryt => utils.If(ryt.getStatusCode == 200, true, false))
    }

  def mac(session: Session, method: String, uri: String, userAgent: String) = {
    def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

    val nonce = _genNonce(session.issuedTime)

    val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, Hostname, Port, "", "")
    unfiltered.mac.Mac.macHash(MACAlgorithm, session.secret)(normalizedRequest).fold({
      errMsg => Future.failed(new ScholaException(s"Invalid request: $errMsg"))
    }, {
      mac =>

        val auth = Map(
          "Authorization" -> s"""MAC id="${session.key}",nonce="$nonce",mac="$mac" """,
          "User-Agent" -> userAgent)

        Future.successful(auth)
    })
  }

  // Profile management

  def getAvatar(sessionKey: String, userAgent: String) =
    session(sessionKey, userAgent) flatMap { session =>

      if (session.user.avatar.isDefined)

        mac(session, "GET", s"/api/$API_VERSION/avatar/${session.user.avatar.get}", userAgent) flatMap { auth =>

          Http(api / "avatar" / session.user.avatar.get <:< auth + ("Accept" -> "application/json") OK Json) flatMap { json =>

            json.asOpt[AvatarInfo]
              .fold[Future[AvatarInfo]](Future.failed(new ScholaException("Avatar not found."))) { avatarInfo => Future.successful(avatarInfo) }
          }
        }

      else Future.failed(new ScholaException("Avatar not found."))
    }

  def purgeAvatar(sessionKey: String, userAgent: String) =
    try

      session(sessionKey, userAgent) flatMap { session =>

        mac(session, "DELETE", s"/api/$API_VERSION/user/${session.user.id.get.toString}/avatars/${session.user.avatar.get}", userAgent) flatMap { auth =>

          Http(api.DELETE / "user" / session.user.id.get.toString / "avatars" / session.user.avatar.get <:< auth OK Json) flatMap { json =>

            json.asOpt[Response]
              .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
          }
        }
      }

    catch {
      case ex: Throwable => Future.failed(ex)
    }

  def setAvatar(sessionKey: String, userAgent: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "POST", s"/api/$API_VERSION/user/${session.user.id.get.toString}/avatars", userAgent) flatMap { auth =>

        val fs = java.io.File.createTempFile("OAdmin-", s"-avatar_${session.user.id.get}")

        org.apache.commons.io.FileUtils.writeByteArrayToFile(fs, bytes)

        Http(api.POST / "user" / session.user.id.get.toString / "avatars" <:< auth addBodyPart new com.ning.http.multipart.FilePart("f", fs, contentType getOrElse "application/octet-stream; charset=UTF-8", "UTF-8") OK Json) flatMap { json =>

          json.asOpt[Response]
            .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
        }
      }
    }

  def updateAccount(
    sessionKey: String,
    userAgent: String,
    primaryEmail: String,
    password: Option[String],
    givenName: String,
    familyName: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Contacts) = {

    val payload = // Validation already done at Profile.update
      PlayJson.obj(
        "primaryEmail" -> primaryEmail,
        "givenName" -> givenName,
        "familyName" -> familyName,
        "password" -> password,
        "gender" -> gender,
        "homeAddress" -> homeAddress,
        "workAddress" -> workAddress,
        "contacts" -> contacts)

    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.get.toString}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.get.toString << PlayJson.stringify(payload) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK Json) map { _ =>
          true
        } recover { case _ => false }
      }
    }
  }

  def changePasswd(sessionKey: String, userAgent: String, passwd: String, newPasswd: String) = {

    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.get.toString}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.get.toString << PlayJson.stringify(PlayJson.obj("old_password" -> passwd, "password" -> newPasswd)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK Json) map { _ =>
          true
        } recover { case _ => false }
      }
    }
  }

  def checkActivationReq(username: String, ky: String): Future[Boolean] =
    Http(api / "users" / "check_activation_req" <<? Map("username" -> username, "key" -> ky) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
    }

  def createPasswdResetReq(username: String) =
    Http(api / "users" / "lostpassword" << Map("username" -> username) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
    }

  def resetPasswd(username: String, key: String, newPasswd: String) =
    Http(api / "users" / "resetpassword" << Map("username" -> username, "key" -> key, "newPassword" -> newPasswd) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
    }

  private val gCapcha = url("http://www.google.com/recaptcha/api/verify")

  lazy val PublicKey = app.configuration.getString("application.recaptcha.public-key").getOrElse(throw new RuntimeException("No public key found for re-captcha"))
  lazy val PrivateKey = app.configuration.getString("application.recaptcha.private-key").getOrElse(throw new RuntimeException("No private key found for re-captcha"))

  def reCaptchaVerify(challenge: String, response: String, remoteAddr: String) = {
    val params = Map(
      "challenge" -> challenge,
      "response" -> response,
      "remoteip" -> remoteAddr,
      "privatekey" -> PrivateKey)

    def isSuccess(msg: String) = {
      val x = msg.split("""\n""")
      x.length > 0 && x(0) == "true"
    }

    for (
      e <- Http(
        gCapcha << params OK as.String).either
    ) yield {

      e.fold(
        _ => false,
        msg => isSuccess(msg))
    }
  }
}