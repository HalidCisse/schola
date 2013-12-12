package schola
package oadmin
package test

import oauth2._
import domain._

import unfiltered.oauth2._
import org.json4s.native.Serialization
import org.clapper.avsl.Logger

object PlansSpec extends org.specs.Specification
with org.specs.mock.Mockito
with unfiltered.spec.jetty.Served {

  import dispatch.classic._

  import org.mockito.Mockito._
  import org.mockito.Matchers._

  import conversions.json._

  val log = Logger("oadmin.tests.PlansSpec")

  def withMac[T](method: String, uri: String)(f: Map[String, String] => T) = {

    def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

    val nonce = _genNonce(session.issuedTime)

    val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, "localhost", port, "", "")
    unfiltered.mac.Mac.macHash(MacAlgo, session.secret)(normalizedRequest).fold({
      fail(_)
    }, { mac =>

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
          import org.apache.http.{HttpResponse=>HcResponse}
          def getLocationURI(res: HcResponse, ctx: HttpContext) = null
          def isRedirectRequested(res: HcResponse, ctx: HttpContext) = false
        })
        c
      }
    }
    try { h.x(handler) }
    finally { h.shutdown() }
  }

  def json[T](body: String)(f: Map[String, Any] => T) =
    scala.util.parsing.json.JSON.parseFull(body) match {
      case Some(obj) => f(
        obj.asInstanceOf[Map[String, Any]]
      )
      case _ => fail("could not parse body")
    }

  val api = host / "api" / "v1"

  val client = domain.OAuthClient(
    "oadmin", "oadmin",
    "http://localhost:%s/api" format port
  )

  val sPassword = config.getString("super-user-password")
  val owner = new ResourceOwner { val id = SuperUser.id.map(_.toString).get; val password = Some(sPassword) }

  val appServices = mock[Façade]

  val oauth2Services = mock[appServices.OAuthServicesImpl]
  val accessControlServices = mock[appServices.AccessControlServicesImpl]

  val session = {
    def generateToken = utils.SHA3Utils digest s"${client.id}:${owner.id}:${System.nanoTime}"
    def generateRefreshToken(accessToken: String) = utils.SHA3Utils digest s"$accessToken:${owner.id}:${System.nanoTime}"
    def generateMacKey = utils.genPasswd(s"${owner.id}:${System.nanoTime}")

    val key = generateToken

    Session(
      key,
      generateMacKey,
      client.id,
      System.currentTimeMillis,
      None,
      Some(generateRefreshToken(key)),
      System.currentTimeMillis,
      SuperUser,
      "Chrome"
    )
  }

  def init() {
    when(appServices.apply(anyObject())) thenCallRealMethod()

    appServices.oauthService returns oauth2Services
    appServices.accessControlService returns accessControlServices

    oauth2Services getUserSession Map("bearerToken" -> session.key, "userAgent" -> session.userAgent) returns Some(session)

    oauth2Services getTokenSecret session.key returns Some(session.secret)
  }

  def setup = {
    server =>

      init()

      case class TestOAuth2Protection(source: AuthSource) extends ProtectionLike {

        object OAuth2MacAuth extends MacAuth {
          val algorithm = MacAlgo
          def tokenSecret(key: String) = oauth2Services.getTokenSecret(key)
        }

        val schemes = Seq(OAuth2MacAuth)
      }

      val oadminAuthSource = mock[unfiltered.oauth2.AuthSource]

      oadminAuthSource authenticateToken(anyObject(), anyObject()) returns Right((owner, client.id, Seq()))

      server/*.context("/oauth") {
        _.filter(unfiltered.filter.Planify {
          case unfiltered.request.UserAgent(uA) & req =>
            OAuthorization(new AuthServerProvider(uA).auth).intent(req) // Too much objects created !?
        })
      }
        */.context("/api/v1") {
        _.filter(TestOAuth2Protection(oadminAuthSource))
          .filter(/*utils.ValidatePasswd(*/new Plans(appServices).routes/*)*/)
      }
  }

  "user plans" should {

    "save user" in {
      val id = java.util.UUID.randomUUID

      val toSave = User(Some(id), "cisse.amadou.9@gmail.com", "amsayk", "Amadou", "Cisse", createdBy = SuperUser.id, passwordValid = true)

      oauth2Services.saveUser(
        any[java.lang.String],
        any[java.lang.String],
        any[java.lang.String],
        any[java.lang.String],
        any[Option[java.lang.String]],
        any[Gender.Value],
        any[Option[AddressInfo]],
        any[Option[AddressInfo]],
        any[Set[ContactInfo]],
        any[Boolean]
      ) returns Some(toSave)

      withMac("POST", "/api/v1/users") { auth =>
        val sres = http(api / "users" <:< auth << (tojson(toSave), "application/json; charset=UTF-8") as_str)

        there was one(oauth2Services).saveUser(
          any[java.lang.String],
          any[java.lang.String],
          any[java.lang.String],
          any[java.lang.String],
          any[Option[java.lang.String]],
          any[Gender.Value],
          any[Option[AddressInfo]],
          any[Option[AddressInfo]],
          any[Set[ContactInfo]],
          any[Boolean]
        )

        Serialization.read[User](sres) must be equalTo toSave
      }
    }

    "force password change on non-validation" in {}

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
}
