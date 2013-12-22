package schola
package oadmin
package test

import oauth2._
import domain._

import unfiltered.oauth2._
import org.json4s.native.Serialization
import org.clapper.avsl.Logger

object PlansSpec extends org.specs.Specification
with unfiltered.spec.jetty.Served {

  import dispatch.classic._

  import unfiltered.request.&
  import unfiltered.response.Pass

  /*
  *   Necessary to avoid removing password from
  *
  * */
  object formats {

    import domain._

    import org.json4s._
    import org.json4s.native.Serialization

    val userSerializer = FieldSerializer[User](FieldSerializer.ignore("_deleted"))

    val tokenSerializer = FieldSerializer[OAuthToken](
      FieldSerializer.ignore("macKey") orElse FieldSerializer.ignore("refreshExpiresIn") orElse FieldSerializer.ignore("tokenType") orElse FieldSerializer.ignore("scopes")
    )

    implicit lazy val my =
      new org.json4s.Formats {
        override val typeHintFieldName = "type"
        val dateFormat = DefaultFormats.lossless.dateFormat
        override val typeHints = ShortTypeHints(List(classOf[Email], classOf[PhoneNumber], classOf[Fax], classOf[HomeContactInfo], classOf[WorkContactInfo], classOf[MobileContactInfo]))
      } +
        new conversions.jdbc.EnumNameSerializer(Gender) +
        userSerializer +
        tokenSerializer +
        new conversions.json.UUIDSerializer

    implicit def tojson[T](v: T) = Serialization.write(v.asInstanceOf[AnyRef])
  }

  val log = Logger("oadmin.tests.PlansSpec")

  def withMac[T](session: Façade.oauthService.SessionLike, method: String, uri: String)(f: Map[String, String] => T) = {

    def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

    val nonce = _genNonce(session.issuedTime)

    val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, "localhost", port, "", "")
    unfiltered.mac.Mac.macHash(MACAlgorithm, session.secret)(normalizedRequest).fold({
      fail(_)
    }, {
      mac =>

        val auth = Map(
          "Authorization" -> s"""MAC id="${session.key}",nonce="$nonce",mac="$mac" """,
          "User-Agent" -> "Chrome"
        )

        f(auth)
    })
  }

  // turning off redirects for validation
  override def http[T](handler: Handler[T]): T = {
    val h = new Http {
      override def make_client = {
        val c = new ConfiguredHttpClient(credentials)
        c.setRedirectHandler(new org.apache.http.client.RedirectHandler() {

          import org.apache.http.protocol.HttpContext
          import org.apache.http.{HttpResponse => HcResponse}

          def getLocationURI(res: HcResponse, ctx: HttpContext) = null

          def isRedirectRequested(res: HcResponse, ctx: HttpContext) = false
        })
        c
      }
    }
    try {
      h.x(handler)
    }
    finally {
      h.shutdown()
    }
  }

  def json[T](body: String)(f: Either[String, Map[String, Any]] => T) =
    scala.util.parsing.json.JSON.parseFull(body) match {
      case Some(obj) =>
        f(
          Right(obj.asInstanceOf[Map[String, Any]])
        )

      case _ => f(Left(body))
    }

  val token = host / "oauth" / "token"
  val api = host / "api" / "v1"

  val client = domain.OAuthClient(
    "oadmin", "oadmin",
    "http://localhost:%s/api" format port
  )

  val sPassword = config.getString("super-user-password")
  val owner = new ResourceOwner { val id = SuperUser.email; val password = Some(sPassword) }

  def init() {
    Façade.init(SuperUser.id.get)
  }

  def drop() = Façade.drop()

  def setup = {
    server =>

      try drop() catch {
        case _: Throwable =>
      }
      init()

      server.context("/oauth") {
        _.filter(unfiltered.filter.Planify {
          case unfiltered.request.UserAgent(uA) & req =>
            OAuthorization(new AuthServerProvider(uA).auth).intent.lift(req).getOrElse(Pass) // Too much objects created !?
        })
      }
        .context("/api/v1") {
        _.filter(OAuth2Protection(new OAdminAuthSource))
          .filter(Plans.routes)
      }
  }

  case class ConnectInfo(username: String, passwd: String, clientId: String = client.id, clientSecret: String = client.secret)

  def withSession[T](connectInfo: ConnectInfo = ConnectInfo(SuperUser.email, SuperUser.password.get))(f: Either[String, Façade.oauthService.SessionLike] => T) = {
    val body = http(token << Map(
      "grant_type" -> "password",
      "client_id" -> connectInfo.clientId,
      "client_secret" -> connectInfo.clientSecret,
      "username" -> connectInfo.username,
      "password" -> connectInfo.passwd
    ) <:< Map("User-Agent" -> "Chrome") as_str)

    json(body) {
      case Right(map) =>

        val key = map("access_token").toString
        val session = Façade.oauthService.getUserSession(Map("bearerToken" -> key, "userAgent" -> "Chrome"))

        session must not be empty

        try f(Right(session.get))
        finally Façade.oauthService.revokeToken(session.get.key)

      case Left(res) => f(Left(res))
    }
  }


  "user plans" should {

    "save user" in {
      val toSave = User(None, "cisse.amadou.9@gmail.com", Some("amsayk"), "Amadou", "Cisse", createdBy = SuperUser.id, passwordValid = true)

      withSession() {
        case Right(session) =>

          withMac(session, "POST", "/api/v1/users") {
            auth =>

              val sres = http(api / "users" <:< auth <<(formats.tojson(toSave), "application/json; charset=UTF-8") as_str)

              import formats.my
              import scala.util.control.Exception.allCatch

              val user = allCatch.opt { Serialization.read[User](sres) }

              user must not be empty

              user.get.email must be equalTo toSave.email
              user.get.firstname must be equalTo toSave.firstname
              user.get.lastname must be equalTo toSave.lastname

              withMac(session, "DELETE", s"/api/v1/user/${user.get.id.get.toString}") {
                auth =>
                  val sres = http(api.DELETE / "user" / user.get.id.get.toString <:< auth as_str)

                  sres must be equalTo """{"success": true}"""

                  withMac(session, "DELETE", s"/api/v1/user/${user.get.id.get.toString}/_purge") {
                    auth =>
                      val sres = http(api.DELETE / "user" / user.get.id.get.toString / "_purge" <:< auth as_str)

                      sres must be equalTo """{"success": true}"""
                  }
              }
          }

        case _ =>
      }
    }

    "force password change on non-validation" in {
      val toSave = User(None, "cisse.amadou.9@gmail.com", Some("amsayk"), "Amadou", "Cisse", createdBy = SuperUser.id, passwordValid = false)

      withSession() {
        case Right(session) =>

          withMac(session, "POST", "/api/v1/users") {
            auth =>

              val sres = http(api / "users" <:< auth <<(formats.tojson(toSave), "application/json; charset=UTF-8") as_str)

              import formats.my
              import scala.util.control.Exception.allCatch

              val user = allCatch.opt { Serialization.read[User](sres) }

              user must not be empty

              user.get.email must be equalTo toSave.email
              user.get.firstname must be equalTo toSave.firstname
              user.get.lastname must be equalTo toSave.lastname

//              withSession(ConnectInfo(user.get.email, toSave.password.get)) {
//                case Right(body) =>
//
//                  body must startWith("<h1>Please change your password.")
//
//                case _ => fail("passwordValid doesn't work")
//              }

              withMac(session, "DELETE", s"/api/v1/user/${user.get.id.get.toString}/_purge") {
                auth =>
                  val sres = http(api.DELETE / "user" / user.get.id.get.toString / "_purge" <:< auth as_str)

                  sres must be equalTo """{"success": true}"""
              }
          }

        case _ =>
      }
    }

    "update user" in {}

    "remove user" in {}

    "purge user" in {}

    "test email existence" in {}

    "add/remove address" in {}

    "add/remove contacts" in {}

    "upload/download/purge avatars" in {}

    "change password" in {}

    "list users" in {}

    "get user" in {}

    "get trash" in {}

    "get session" in {}

    "grant/revoke/get user roles" in {}

    "get session" in {}
  }

  "role plans" should {

    "save role" in {}

    "update role" in {}

    "delete roles" in {}

    "list one or roles" in {}

    "grant/revoke permissions" in {}

    "get role permissions" in {}
  }

  doAfterSpec {
    drop()
  }
}
