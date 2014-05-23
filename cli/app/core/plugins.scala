package ma.epsilon.schola
package cli

import play.api.{ Plugin, Logger, Application }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import play.api.libs.json.{ JsValue, Json => PlayJson }

import scala.concurrent.{ Future => F }
import scala.util.control.NonFatal

import domain._

trait SessionSupport extends Plugin {

  def session(sessionKey: String, userAgent: String): F[Session]

  def login(username: String, passwd: String, userAgent: String): F[Session]

  def logout(sessionKey: String): F[Boolean]

  def mac(session: Session, method: String, uri: String, userAgent: String): F[Map[String, String]]

  // Profile management

  def downloadAvatar(sessionKey: String, userAgent: String): F[AvatarInfo]

  def purgeAvatar(sessionKey: String, userAgent: String): F[Boolean]

  def uploadAvatar(sessionKey: String, userAgent: String, filename: String, contentType: Option[String], bytes: Array[Byte]): F[Boolean]

  def updateAccount(
    sessionKey: String,
    userAgent: String,
    primaryEmail: String,
    oldPassword: Option[String],
    password: Option[String],
    givenName: String,
    familyName: String,
    jobTitle: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts]): F[Boolean]

  def changePasswd(sessionKey: String, userAgent: String, passwd: String, newPasswd: String): F[Boolean]

  def checkActivationReq(username: String, ky: String): F[Boolean]

  def createPasswdResetReq(username: String): F[Boolean]

  def resetPasswd(username: String, key: String, newPasswd: String): F[Boolean]

  def PublicKey: String

  def PrivateKey: String

  def reCaptchaVerify(challenge: String, response: String, remoteAddr: String): F[Boolean]
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

  val client = OAuthClient(
    OAUTH_CLIENT, OAUTH_CLIENT_SECRET, OAUTH_REDIRECT_URI)

  private val apiHost = url(OAUTH_REDIRECT_URI)

  private val token = apiHost / "oauth" / "token"
  private val api = apiHost / "api" / API_VERSION
  private val session = api / "session"

  def session(sessionKey: String, userAgent: String): F[Session] =
    Http(session <<? Map("token" -> sessionKey) <:< Map("User-Agent" -> userAgent) OK Json) flatMap {
      json =>

        json.asOpt[Session].fold[F[Session]](F.failed(new ScholaException(s"Invalid session [$sessionKey]"))) {
          s =>

            def isExpired(s: Session) = s.expiresIn exists (expiration => s.issuedTime.plusSeconds(expiration.getSeconds) isBefore now) // s.expiresIn exists (s.issuedTime + _ * 1000 < System.currentTimeMillis)

            if (isExpired(s)) {

              if (s.refresh.isDefined) {

                Http(token << Map(
                  "grant_type" -> "refresh_token",
                  "client_id" -> client.id,
                  "client_secret" -> client.secret,
                  "refresh_token" -> s.refresh.get) <:< Map("User-Agent" -> userAgent) OK Json) flatMap { info =>

                  (info \ "access_token")
                    .asOpt[String]
                    .fold[F[String]](F.failed(new ScholaException(s"Could not refresh token [$sessionKey]"))) { accessToken => F.successful(accessToken) }

                } flatMap { accessToken =>
                  session(accessToken, userAgent)
                }

              } else F.failed(new ScholaException(s"No refresh token for expired session [$sessionKey]"))

            } else F.successful(s)
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
        .fold[F[String]](F.failed(new ScholaException("Login failed"))) { sessionKey => F.successful(sessionKey) }

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
    def _genNonce(issuedTime: java.time.Instant) = s"${now.getEpochSecond - issuedTime.getEpochSecond}:${utils.randomString(4)}"

    val nonce = _genNonce(session.issuedTime)

    val normalizedRequest = Mac.requestString(nonce, method, uri, "localhost", 80, "", "")
    Mac.macHash(MACAlgorithm, session.secret)(normalizedRequest).fold({
      errMsg => F.failed(new ScholaException(s"Invalid request: $errMsg"))
    }, {
      mac =>

        val auth = Map(
          "Authorization" -> s"""MAC id="${session.key}",nonce="$nonce",mac="$mac" """,
          "User-Agent" -> userAgent)

        F.successful(auth)
    })
  }

  // Profile management

  def downloadAvatar(sessionKey: String, userAgent: String) =
    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "GET", s"/api/$API_VERSION/avatar/${session.user.id.toString}", userAgent) flatMap { auth =>

        Http(api / "avatar" / session.user.id.toString <:< auth + ("Accept" -> "application/json") OK Json) flatMap { json =>

          json.asOpt[AvatarInfo]
            .fold[F[AvatarInfo]](F.failed(new ScholaException("Avatar not found."))) { avatarInfo => F.successful(avatarInfo) }
        }
      }
    }

  def purgeAvatar(sessionKey: String, userAgent: String) =
    try

      session(sessionKey, userAgent) flatMap { session =>

        mac(session, "DELETE", s"/api/$API_VERSION/user/${session.user.id.toString}/avatar", userAgent) flatMap { auth =>

          Http(api.DELETE / "user" / session.user.id.toString / "avatar" <:< auth OK Json) flatMap { json =>

            json.asOpt[Response]
              .fold[F[Boolean]](F.successful(false)) { response => F.successful(response.success) }
          }
        }
      }

    catch {
      case NonFatal(ex) => F.failed(ex)
    }

  def uploadAvatar(sessionKey: String, userAgent: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.toString}/avatar?filename=${java.net.URLEncoder.encode(filename, "UTF-8")}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.toString / "avatar" <<? Map("filename" -> filename) <:< auth + ("Content-Type" -> contentType.getOrElse("application/octet-stream; charset=UTF-8")) setBody (bytes) OK Json) flatMap { json =>

          json.asOpt[Response]
            .fold[F[Boolean]](F.successful(false)) { response => F.successful(response.success) }
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
    jobTitle: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts]) = {

    val payload = // Validation already done at Profile.update
      PlayJson.obj(
        "primaryEmail" -> primaryEmail,
        "givenName" -> givenName,
        "familyName" -> familyName,
        "jobTitle" -> jobTitle,
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
        } recover { case NonFatal(_) => false }
      }
    }
  }

  def changePasswd(sessionKey: String, userAgent: String, passwd: String, newPasswd: String) = {

    session(sessionKey, userAgent) flatMap { session =>

      mac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.toString}", userAgent) flatMap { auth =>

        Http(api.PUT / "user" / session.user.id.toString << PlayJson.stringify(PlayJson.obj("oldPassword" -> passwd, "password" -> newPasswd)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK Json) map { _ =>
          true
        } recover { case NonFatal(_) => false }
      }
    }
  }

  def checkActivationReq(username: String, ky: String): F[Boolean] =
    Http(api / "users" / "check_activation_req" <<? Map("username" -> username, "key" -> ky) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[F[Boolean]](F.successful(false)) { response => F.successful(response.success) }
    }

  def createPasswdResetReq(username: String) =
    Http(api / "users" / "lostpassword" << Map("username" -> username) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[F[Boolean]](F.successful(false)) { response => F.successful(response.success) }
    }

  def resetPasswd(username: String, key: String, newPasswd: String) =
    Http(api / "users" / "resetpassword" << Map("username" -> username, "key" -> key, "newPassword" -> newPasswd) OK Json) flatMap { json =>

      json.asOpt[domain.Response]
        .fold[F[Boolean]](F.successful(false)) { response => F.successful(response.success) }
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

object Mac extends utils.Signing