package schola
package oadmin
package test

import oauth2._

import unfiltered.oauth2._

object OAuth2Specification extends org.specs.Specification
  with unfiltered.spec.jetty.Served {

  import dispatch.classic._
  import unfiltered.request.&
  import unfiltered.response.Pass

  import scala.util.parsing.json.JSON

  val token = host / "oauth" / "token"
  val api = host / "api" / "v1"

  val client = domain.OAuthClient(
    "oadmin", "oadmin",
    "http://localhost:%s/api" format port
  )

  val sPassword = config.getString("super-user-password")
  val owner = new ResourceOwner { val id = SuperUser.email; val password = Some(sPassword) }

  def setup = { server =>
    try drop() catch { case _: Throwable => }
    initialize()

    server.context("/oauth") {
      _.filter(unfiltered.filter.Planify{
        case unfiltered.request.UserAgent(uA) & req =>
          OAuthorization(new AuthServerProvider(uA).auth).intent.lift(req).getOrElse(Pass) // Too much objects created !?
      })
    }
    .context("/api/v1") {
      _.filter(OAuth2Protection(new OAdminAuthSource))
      .filter(/*utils.ValidatePasswd(*/Plans.routes/*)*/)
    }
  }

  def initialize() = Façade.init(SuperUser.id.get)

  def drop() = Façade.drop()

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
    JSON.parseFull(body) match {
      case Some(obj) => f(
        obj.asInstanceOf[Map[String, Any]]
      )
      case _ => fail("could not parse body")
    }

  val ErrorQueryString = """error=(\S+)&error_description=(\S+)$""".r

  //
  // resource owner password credentials flow
  //
  "OAuth2 requests for grant type password" should {

    "accept our user's password credentials" in {
      val (head, body) = http(token << Map(
        "grant_type" -> "password",
        "client_id" -> client.id,
        "client_secret" -> client.secret,
        "username" -> owner.id,
        "password" -> sPassword
      ) <:< Map("User-Agent" -> "Chrome") >+ { r => (r >:> { h => r }, r as_str ) })
      json(body) { map =>
        map must haveKey("access_token")
      }
    }

    "make an oauth2 request with valid access token" in {
      val (head, body) = http(token << Map(
        "grant_type" -> "password",
        "client_id" -> client.id,
        "client_secret" -> client.secret,
        "username" -> owner.id,
        "password" -> sPassword
      ) <:< Map("User-Agent" -> "Chrome") >+ { r => (r >:> { h => r }, r as_str ) })

      json(body) { map =>
        map must haveKey("access_token")

        def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

        val (key, nonce, method,  uri, hostname, hport) = (
          map("secret").toString, _genNonce(map("issued_time").toString.toLong), "GET", "/api/v1/session",  "localhost", port)

        val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, hostname, hport, "", "")
        unfiltered.mac.Mac.macHash(MACAlgorithm, key)(normalizedRequest).fold({
          fail(_)
        }, { mac =>
          val auth = Map(
            "Authorization" -> """MAC id="%s",nonce="%s",mac="%s" """.format(map("access_token"), nonce, mac),
            "User-Agent" -> "Chrome"
          )

          val rres = http(api / "session" <:< auth as_str)

          rres startsWith  """{"key":"""
        })
      }
    }

    "be able to logout user and revoked token" in {
      val (head, body) = http(token << Map(
        "grant_type" -> "password",
        "client_id" -> client.id,
        "client_secret" -> client.secret,
        "username" -> owner.id,
        "password" -> sPassword
      ) <:< Map("User-Agent" -> "Chrome") >+ { r => (r >:> { h => r }, r as_str ) })

      json(body) { map : Map[String, Any] =>
        map must haveKey("access_token")

        def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

        {
          val (key, nonce, method,  uri, hostname, hport) = (
            map("secret").toString, _genNonce(map("issued_time").toString.toLong), "GET", "/api/v1/logout",  "localhost", port)

          val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, hostname, hport, "", "")
          unfiltered.mac.Mac.macHash(MACAlgorithm, key)(normalizedRequest).fold({
            fail(_)
          }, { mac =>
            val auth = Map(
              "Authorization" -> """MAC id="%s",nonce="%s",mac="%s" """.format(map("access_token"), nonce, mac),
              "User-Agent" -> "Chrome"
            )

            val rres = http(api / "logout" <:< auth as_str)

            rres mustEqual """{"success": true}"""
          })

          {
            val (key, nonce, method,  uri, hostname, hport) = (
              map("secret").toString, _genNonce(map("issued_time").toString.toLong), "GET", "/api/session",  "http://localhost", 80)

            val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, hostname, hport, "", "")
            unfiltered.mac.Mac.macHash(MACAlgorithm, key)(normalizedRequest).fold({
              fail(_)
            }, { mac =>
              val auth = Map(
                "Authorization" -> """MAC id="%s",nonce="%s",mac="%s" """.format(map("access_token"), nonce, mac),
                "User-Agent" -> "Chrome"
              )

              val rres2 = http(api / "session" <:< auth as_str)

              rres2 must be equalTo "error=\"invalid token\""
            })
          }
        }
      }
    }

    // http://tools.ietf.org/html/draft-ietf-oauth-v2-21#section-4.2.2
    "have appropriate Cache-Control and Pragma headers" in {
      val headers = http(token << Map(
        "grant_type" -> "password",
        "client_id" -> client.id,
        "client_secret" -> client.secret,
        "username" -> owner.id,
        "password" -> sPassword
      ) <:< Map("User-Agent" -> "Chrome") >:> { h => h })
      headers must haveKey("Cache-Control")
      headers must haveKey("Pragma")
      headers("Cache-Control") must be_==(Set("no-store"))
      headers("Pragma") must be_==(Set("no-cache"))
    }

    "refresh token for a valid access_token" in {

      val req = token << Map(
        "grant_type" -> "password",
        "client_id" -> client.id,
        "client_secret" -> client.secret,
        "username" -> owner.id,
        "password" -> sPassword
      ) <:< Map("User-Agent" -> "Chrome") as_!(client.id, client.secret)

      // requesting access token
      val (header, ares) =
        http(req >+ { r => (r >:> { h => h }, r as_str ) })
      // http://tools.ietf.org/html/draft-ietf-oauth-v2-21#section-4.2.2:
      header must haveKey("Cache-Control")
      header must haveKey("Pragma")
      header("Cache-Control") must be_== (Set("no-store"))
      header("Pragma") must be_== (Set("no-cache"))
      json(ares) { map =>
        map must haveKey("access_token")
        map must haveKey("expires_in")
        map must haveKey("refresh_token")

        // refreshing token
        val rres = http(token << Map(
          "grant_type" -> "refresh_token",
          "client_id" -> client.id,
          "client_secret" -> client.secret,
          "refresh_token" -> map("refresh_token").toString
        ) <:< Map("User-Agent" -> "Chrome") as_str)
        json(rres) { map2  =>
          map2 must haveKey("access_token")
          map2 must haveKey("expires_in")
          map2 must haveKey("refresh_token")

          map2("refresh_token") must not be equalTo(map("refresh_token"))
          map2("access_token") must not be equalTo(map("access_token"))

          def _genNonce(issuedAt: Long) = s"${System.currentTimeMillis - issuedAt}:${utils.randomString(4)}"

          // old access token should be revoked
          {
            val (key, nonce, method,  uri, hostname, hport) = (
              map("secret").toString, _genNonce(map("issued_time").toString.toLong), "GET", "/api/session",  "localhost", port)

            val normalizedRequest = unfiltered.mac.Mac.requestString(nonce, method, uri, hostname, hport, "", "")
            unfiltered.mac.Mac.macHash(MACAlgorithm, key)(normalizedRequest).fold({
              fail(_)
            }, { mac =>
              val auth = Map(
                "Authorization" -> """MAC id="%s",nonce="%s",mac="%s" """.format(map("access_token"), nonce, mac),
                "User-Agent" -> "Chrome"
              )

              val rres = http(api / "session" <:< auth as_str)

              rres must be equalTo "error=\"invalid token\""
            })
          }
        }
      }
    }
  }

  doAfterSpec { drop() }
}
