package ma.epsilon.schola
package cli

import play.api.{ Plugin, Logger, Application }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.libs.json.{ JsValue, Json => PlayJson }

import domain._

trait SessionSupport extends Plugin {

  def session(sessionKey: String, userAgent: String): scala.concurrent.Future[Session]

  def login(username: String, passwd: String, userAgent: String): scala.concurrent.Future[Session]

  def logout(sessionKey: String): scala.concurrent.Future[Boolean]

  def mac(session: Session, method: String, uri: String, userAgent: String): scala.concurrent.Future[Map[String, String]]

  // Profile management

  def downloadAvatar(sessionKey: String, userAgent: String): scala.concurrent.Future[AvatarInfo]

  def purgeAvatar(sessionKey: String, userAgent: String): scala.concurrent.Future[Boolean]

  def uploadAvatar(sessionKey: String, userAgent: String, filename: String, contentType: Option[String], bytes: Array[Byte]): scala.concurrent.Future[Boolean]

  def updateAccount(
    sessionKey: String,
    userAgent: String,
    primaryEmail: String,
    oldPassword: Option[String],
    password: Option[String],
    givenName: String,
    familyName: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts]): scala.concurrent.Future[Boolean]

  def changePasswd(sessionKey: String, userAgent: String, passwd: String, newPasswd: String): scala.concurrent.Future[Boolean]

  def checkActivationReq(username: String, ky: String): scala.concurrent.Future[Boolean]

  def createPasswdResetReq(username: String): scala.concurrent.Future[Boolean]

  def resetPasswd(username: String, key: String, newPasswd: String): scala.concurrent.Future[Boolean]

  def PublicKey: String

  def PrivateKey: String

  def reCaptchaVerify(challenge: String, response: String, remoteAddr: String): scala.concurrent.Future[Boolean]
}

class DefaultSessionSupport(app: Application) extends SessionSupport {

  import conversions.json._

  import dispatch._

  object Json extends (com.ning.http.client.Response => JsValue) {
    def apply(r: com.ning.http.client.Response) =
      (dispatch.as.String andThen (s => PlayJson.parse(s)))(r)
  }

  override def onStart() {
    Logger.info("[schola-cli] loaded session plugin: %s".format(getClass.getName))
  }

  val hostname = "localhost"

  val client = OAuthClient(
    "schola", "schola",
    s"http://$hostname")

  private val apiHost = host(hostname)

  private val token = apiHost / "oauth" / "token"
  private val api = apiHost / "api" / API_VERSION
  private val session = api / "session"

  def session(sessionKey: String, userAgent: String): scala.concurrent.Future[Session] =
    Http(session <<? Map("token" -> sessionKey) <:< Map("User-Agent" -> userAgent) OK Json) flatMap {
      json =>

        json.asOpt[Session].fold[scala.concurrent.Future[Session]](scala.concurrent.Future.failed(new ScholaException(s"Invalid session [$sessionKey]"))) {
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
                    .fold[scala.concurrent.Future[String]](scala.concurrent.Future.failed(new ScholaException(s"Could not refresh token [$sessionKey]"))) { accessToken => scala.concurrent.Future.successful(accessToken) }

                } flatMap { accessToken =>
                  session(accessToken, userAgent)
                }

              } else scala.concurrent.Future.failed(new ScholaException(s"No refresh token for expired session [$sessionKey]"))

            } else scala.concurrent.Future.successful(s)
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
        .fold[scala.concurrent.Future[String]](scala.concurrent.Future.failed(new ScholaException("Login failed"))) { sessionKey => scala.concurrent.Future.successful(sessionKey) }

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

    val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, "localhost", 80, "", "")
    unfiltered.mac.Mac.macHash(MACAlgorithm, session.secret)(normalizedRequest).fold({
      errMsg => scala.concurrent.Future.failed(new ScholaException(s"Invalid request: $errMsg"))
    }, {
      mac =>

        val auth = Map(
          "Authorization" -> s"""MAC id="${session.key}",nonce="$nonce",mac="$mac" """,
          "User-Agent" -> userAgent)

        scala.concurrent.Future.successful(auth)
    })
  }

  // Profile management

  def downloadAvatar(sessionKey: String, userAgent: String) =
    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "GET", s"/api/$API_VERSION/avatar/${session.user.id.toString}", userAgent) flatMap { auth =>

        Http(api / "avatar" / session.user.id.toString <:< auth + ("Accept" -> "application/json") OK Json) flatMap { json =>

          json.asOpt[AvatarInfo]
            .fold[scala.concurrent.Future[AvatarInfo]](scala.concurrent.Future.failed(new ScholaException("Avatar not found."))) { avatarInfo => scala.concurrent.Future.successful(avatarInfo) }
        }
      }
    }

  def purgeAvatar(sessionKey: String, userAgent: String) =
    try

      session(sessionKey, userAgent) flatMap { session =>

        mac(session, "DELETE", s"/api/$API_VERSION/user/${session.user.id.toString}/avatar", userAgent) flatMap { auth =>

          Http(api.DELETE / "user" / session.user.id.toString / "avatar" <:< auth OK Json) flatMap { json =>

            json.asOpt[Response]
              .fold[scala.concurrent.Future[Boolean]](scala.concurrent.Future.successful(false)) { response => scala.concurrent.Future.successful(response.success) }
          }
        }
      }

    catch {
      case scala.util.control.NonFatal(ex) => scala.concurrent.Future.failed(ex)
    }

  def uploadAvatar(sessionKey: String, userAgent: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.toString}/avatar?filename=${java.net.URLEncoder.encode(filename, "UTF-8")}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.toString / "avatar" <<? Map("filename" -> filename) <:< auth + ("Content-Type" -> contentType.getOrElse("application/octet-stream; charset=UTF-8")) setBody (bytes) OK Json) flatMap { json =>

          json.asOpt[Response]
            .fold[scala.concurrent.Future[Boolean]](scala.concurrent.Future.successful(false)) { response => scala.concurrent.Future.successful(response.success) }
        }
      }
    }

  def updateAccount(
    sessionKey: String,
    userAgent: String,
    primaryEmail: String,
    oldPassword: Option[String],
    password: Option[String],
    givenName: String,
    familyName: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts]) = {

    val payload = // Validation already done at Profile.update
      PlayJson.obj(
        "primaryEmail" -> primaryEmail,
        "givenName" -> givenName,
        "familyName" -> familyName,
        "oldPassword" -> oldPassword,
        "password" -> password,
        "gender" -> gender,
        "homeAddress" -> homeAddress,
        "workAddress" -> workAddress,
        "contacts" -> contacts)

    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.toString}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.toString << PlayJson.stringify(payload) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK Json) map { _ =>
          true
        } recover { case scala.util.control.NonFatal(_) => false }
      }
    }
  }

  def changePasswd(sessionKey: String, userAgent: String, passwd: String, newPasswd: String) = {

    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.toString}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.toString << PlayJson.stringify(PlayJson.obj("oldPassword" -> passwd, "password" -> newPasswd)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK Json) map { _ =>
          true
        } recover { case scala.util.control.NonFatal(_) => false }
      }
    }
  }

  def checkActivationReq(username: String, ky: String): scala.concurrent.Future[Boolean] =
    Http(api / "users" / "check_activation_req" <<? Map("username" -> username, "key" -> ky) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[scala.concurrent.Future[Boolean]](scala.concurrent.Future.successful(false)) { response => scala.concurrent.Future.successful(response.success) }
    }

  def createPasswdResetReq(username: String) =
    Http(api / "users" / "lostpassword" << Map("username" -> username) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[scala.concurrent.Future[Boolean]](scala.concurrent.Future.successful(false)) { response => scala.concurrent.Future.successful(response.success) }
    }

  def resetPasswd(username: String, key: String, newPasswd: String) =
    Http(api / "users" / "resetpassword" << Map("username" -> username, "key" -> key, "newPassword" -> newPasswd) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[scala.concurrent.Future[Boolean]](scala.concurrent.Future.successful(false)) { response => scala.concurrent.Future.successful(response.success) }
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