package schola
package oadmin
package cl

import play.api.{ Plugin, Logger, Application }

class SessionSupport(app: Application) extends Plugin with controllers.ExecutionSystem {

  import scala.concurrent.Future, scala.util.control.Exception.allCatch

  import conversions.json.formats, domain._

  import dispatch._

  override def onStart() {
    Logger.info("[oadmin-cl] loaded session plugin: %s".format(getClass.getName))
  }

  lazy val port = app.configuration.getInt("server.port").getOrElse(3000)

  lazy val hostname = app.configuration.getString("server.hostname").getOrElse("localhost")

  val client = OAuthClient(
    "oadmin", "oadmin",
    f"http://$hostname:${port}%d")

  val xHttp = Http

  private lazy val apiHost = host(hostname, port)

  private lazy val token = apiHost / "oauth" / "token"
  private lazy val api = apiHost / "api" / API_VERSION
  private lazy val session = api / "session"

  def getSession(sessionKey: String, userAgent: String): Future[Session] =
    xHttp(session <<? Map("access_token" -> sessionKey) <:< Map("User-Agent" -> userAgent) OK as.json4s.Json) flatMap {
      json =>

        allCatch.opt { json.extract[Session] }.fold[Future[Session]](Future.failed(new ScholaException(s"Invalid session [$sessionKey]"))) {
          s =>

            def isExpired(s: Session) = s.expiresIn exists (s.issuedTime + _ * 1000 < System.currentTimeMillis)

            if (isExpired(s)) {

              if (s.refresh.isDefined) {

                xHttp(token << Map(
                  "grant_type" -> "refresh_token",
                  "client_id" -> client.id,
                  "client_secret" -> client.secret,
                  "refresh_token" -> s.refresh.get) <:< Map("User-Agent" -> userAgent) OK as.json4s.Json) flatMap { info =>

                  utils.findFieldStr(info)("access_token")
                    .fold[Future[String]](Future.failed(new ScholaException(s"Could not refresh token [$sessionKey]"))) { accessToken => Future.successful(accessToken) }

                } flatMap { accessToken =>
                  getSession(accessToken, userAgent)
                }

              } else Future.failed(new ScholaException(s"No refresh token for expired session [$sessionKey]"))

            } else Future.successful(s)
        }

    }

  def login(username: String, passwd: String, userAgent: String) =
    xHttp(token << Map(
      "grant_type" -> "password",
      "client_id" -> client.id,
      "client_secret" -> client.secret,
      "username" -> username,
      "password" -> passwd) <:< Map("User-Agent" -> userAgent) OK as.json4s.Json) flatMap { info =>

      utils.findFieldStr(info)("access_token")
        .fold[Future[String]](Future.failed(new ScholaException("Login failed"))) { sessionKey => Future.successful(sessionKey) }

    } flatMap { accessToken =>
      getSession(accessToken, userAgent)
    }

  def logout(sessionKey: String) =
    for {
      e <- xHttp(
        api / "logout" <<? Map("access_token" -> sessionKey)).either
    } yield {

      e.fold(
        _ => false,
        ryt => utils.If(ryt.getStatusCode == 200, true, false))
    }

  def genMac(session: Session, method: String, uri: String, userAgent: String) = {
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
    getSession(sessionKey, userAgent) flatMap { session =>

      if (session.user.avatar.isDefined)

        genMac(session, "GET", s"/api/$API_VERSION/avatar/${session.user.avatar.get}", userAgent) flatMap { auth =>

          xHttp(api / "avatar" / session.user.avatar.get <:< auth OK as.json4s.Json) flatMap { json =>

            allCatch.opt { json.extract[AvatarInfo] }
              .fold[Future[AvatarInfo]](Future.failed(new ScholaException("No refresh token for expired session"))) { avatarInfo => Future.successful(avatarInfo) }
          }
        }

      else Future.failed(new ScholaException("User doesn't have an avatar"))
    }

  def purgeAvatar(sessionKey: String, userAgent: String) =
    try

      getSession(sessionKey, userAgent) flatMap { session =>

        genMac(session, "DELETE", s"/api/$API_VERSION/user/${session.user.id.get.toString}/avatars/${session.user.avatar.get}", userAgent) flatMap { auth =>

          xHttp(api.DELETE / "user" / session.user.id.get.toString / "avatars" / session.user.avatar.get <:< auth OK as.json4s.Json) flatMap { json =>

            allCatch.opt { json.extract[Response] }
              .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
          }
        }
      }

    catch {
      case ex: Throwable => Future.failed(ex)
    }

  def setAvatar(sessionKey: String, userAgent: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
    getSession(sessionKey, userAgent) flatMap { session =>

      genMac(session, "POST", s"/api/$API_VERSION/user/${session.user.id.get.toString}/avatars", userAgent) flatMap { auth =>

        val fs = java.io.File.createTempFile("OAdmin-", s"-avatar_${session.user.id.get}")

        org.apache.commons.io.FileUtils.writeByteArrayToFile(fs, bytes)

        xHttp(api.POST / "user" / session.user.id.get.toString / "avatars" <:< auth addBodyPart new com.ning.http.multipart.FilePart("f", fs, contentType getOrElse "application/octet-stream; charset=UTF-8", "UTF-8") OK as.json4s.Json) flatMap { json =>

          allCatch.opt { json.extract[Response] }
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

    import org.json4s._
    import org.json4s.JsonDSL._
    import org.json4s.native.JsonMethods._

    implicit def ContactsToJson(contacts: Contacts): JValue =
      ("mobiles" -> contacts.mobiles) ~
        ("home" -> contacts.home) ~
        ("work" -> contacts.work)

    implicit def MobileNumbersToJson(mobileNumbers: MobileNumbers): JValue =
      ("mobile1" -> mobileNumbers.mobile1) ~
        ("mobile2" -> mobileNumbers.mobile2)

    implicit def ContactInfoToJson(contactInfo: ContactInfo): JValue =
      ("email" -> contactInfo.email) ~
        ("phoneNumber" -> contactInfo.phoneNumber) ~
        ("fax" -> contactInfo.fax)

    implicit def AddressInfoToJson(addressInfo: AddressInfo): JValue =
      ("city" -> addressInfo.city) ~
        ("country" -> addressInfo.country) ~
        ("postalCode" -> addressInfo.postalCode) ~
        ("streetAddress" -> addressInfo.streetAddress)

    val payload =
      ("primaryEmail" -> primaryEmail) ~
        ("givenName" -> givenName) ~
        ("familyName" -> familyName) ~
        ("password" -> password) ~
        ("gender" -> gender.toString) ~
        ("homeAddress" -> homeAddress) ~
        ("workAddress" -> workAddress) ~
        ("contacts" -> contacts)

    getSession(sessionKey, userAgent) flatMap { session =>

      genMac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.get.toString}", userAgent) flatMap { auth =>

        xHttp(api.PUT / "user" / session.user.id.get.toString << compact(render(payload)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK as.json4s.Json) map { _ =>
          true
        } recover { case _ => false }
      }
    }
  }

  def changePasswd(sessionKey: String, userAgent: String, passwd: String, newPasswd: String) = {
    import conversions.json.tojson

    import org.json4s.JsonDSL._

    getSession(sessionKey, userAgent) flatMap { session =>

      genMac(session, "PUT", s"/api/$API_VERSION/user/${session.user.id.get.toString}", userAgent) flatMap { auth =>

        xHttp(api.PUT / "user" / session.user.id.get.toString << tojson(("old_password" -> passwd) ~ ("password" -> newPasswd)) <:< auth + ("Content-Type" -> "application/json; charset=UTF-8") OK as.json4s.Json) map { _ =>
          true
        } recover { case _ => false }
      }
    }
  }

  def checkActivationReq(username: String, ky: String): Future[Boolean] = 
    xHttp(api / "users" / "check_activation_req" <<? Map("username" -> username, "key" -> ky) OK as.json4s.Json) flatMap { json =>

      allCatch.opt { json.extract[Response] }
        .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
    }  

  def createPasswdResetReq(username: String) =
    xHttp(api / "users" / "lostpassword" << Map("username" -> username) OK as.json4s.Json) flatMap { json =>

      allCatch.opt { json.extract[Response] }
        .fold[Future[Boolean]](Future.successful(false)) { response => Future.successful(response.success) }
    }

  def resetPasswd(username: String, key: String, newPasswd: String) =
    xHttp(api / "users" / "resetpassword" << Map("username" -> username, "key" -> key, "new_password" -> newPasswd) OK as.json4s.Json) flatMap { json =>

      allCatch.opt { json.extract[Response] }
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
      e <- xHttp(
        gCapcha << params OK as.String).either
    ) yield {

      e.fold(
        _ => false,
        msg => isSuccess(msg))
    }
  }
}