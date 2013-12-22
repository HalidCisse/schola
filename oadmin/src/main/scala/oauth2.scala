package schola
package oadmin

import unfiltered.oauth2._
import unfiltered.request.HttpRequest

import org.clapper.avsl.Logger
import unfiltered.response.HttpResponse

package object oauth2 {

  val log = Logger("oadmin.oauth2")

  case class NotFoundException(msg: String) extends Exception

  case class BadTokenException(msg: String) extends Exception

  class AuthServerProvider(val userAgent: String) extends AuthorizationProvider {

    trait Clients extends ClientStore {
      def client(clientId: String, secret: Option[String]) =
      Façade.oauthService.getClient(clientId, secret getOrElse "") map {
        case domain.OAuthClient(cId, cSecret, cRedirectUri) =>
          new Client {
            val id = cId
            val secret = cSecret
            val redirectUri = cRedirectUri
          }
      }
    }

    trait Tokens extends TokenStore {

      // Given a refresh token gives a new access token
      def refresh(other: Token) = Façade.oauthService.exchangeRefreshToken(other.refresh getOrElse "") map {
        case domain.OAuthToken(sAccessToken, sClientId, sRedirectUri, sOwnerId, sRefreshToken, macKey, _, sExpires, sRefreshExpiresIn, sCreatedAt, _, sTokenType, sScopes) =>
          new Token {
            val tokenType = Some(sTokenType)
            val redirectUri = sRedirectUri
            val expiresIn = sExpires map (_.toInt)
            val owner = sOwnerId.toString
            val scopes = sScopes.toSeq
            val refresh = sRefreshToken
            val value = sAccessToken
            val clientId = sClientId
            override val extras = Map("secret" -> macKey, "issuedTime" -> sCreatedAt.toString, "algorithm" -> MACAlgorithm)
          }
      } getOrElse (throw NotFoundException("could not refresh token"))

      def token(code: String): Option[Token] = ???

      // Returns the refreshToken for the accessToken
      def refreshToken(refreshToken: String) =
        Façade.oauthService.getRefreshToken(refreshToken) map {
          case domain.OAuthToken(sAccessToken, sClientId, sRedirectUri, sOwnerId, sRefreshToken, macKey, uA, sExpires, sRefreshExpiresIn, sCreatedAt, _, sTokenType, sScopes) if uA == userAgent =>
            new Token {
              val tokenType = Some(sTokenType)
              val redirectUri = sRedirectUri
              val expiresIn = sExpires map (_.toInt)
              val owner = sOwnerId.toString
              val scopes = sScopes.toSeq
              val refresh = sRefreshToken
              val value = sAccessToken
              val clientId = sClientId
              override val extras = Map("secret" -> macKey, "issuedTime" -> sCreatedAt.toString, "algorithm" -> MACAlgorithm)
            }
        }

      def exchangeAuthorizationCode(other: Token): Token = ???

      def generateAuthorizationCode(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
                                    scopes: Seq[String], redirectURI: String) = ???

      def generateImplicitAccessToken(responseTypes: Seq[String], owner: ResourceOwner, client: Client,
                                      scopes: Seq[String], redirectURI: String) = ???

      def generateClientToken(client: Client, scopes: Seq[String]) = ???

      def generatePasswordToken(owner: ResourceOwner, client: Client,
                                scopes: Seq[String]) = {

        def generateToken = utils.SHA3Utils digest s"${client.id}:${owner.id}:${System.nanoTime}"
        def generateRefresh(accessToken: String) = utils.SHA3Utils digest s"$accessToken:${owner.id}:${System.nanoTime}"
        def generateMacKey = utils.genPasswd(s"${owner.id}:${System.nanoTime}")

        try {

          val accessToken = generateToken
          Façade.oauthService.saveToken(
            accessToken, Some(generateRefresh(accessToken)), macKey = generateMacKey, userAgent, client.id, client.redirectUri, owner.id, Some(AccessTokenSessionLifeTime), Some(RefreshTokenSessionLifeTime), Set(scopes : _*)
          ) match {
            case Some(domain.OAuthToken(sAccessToken, sClientId, sRedirectUri, sOwnerId, sRefreshToken, macKey, _, sExpires, sRefreshExpiresIn, sCreatedAt, _, sTokenType, sScopes)) =>
              new Token {
                val tokenType = Some(sTokenType)
                val redirectUri = sRedirectUri
                val expiresIn = sExpires map (_.toInt)
                val owner = sOwnerId.toString
                val scopes = sScopes.toSeq
                val refresh = sRefreshToken
                val value = sAccessToken
                val clientId = sClientId
                override val extras = Map("secret" -> macKey, "issuedTime" -> sCreatedAt.toString, "algorithm" -> MACAlgorithm)
              }

            case _ => throw BadTokenException("Token not created")
          }

        }
        catch {
          case e: Throwable => throw e
        }

      }
    }

    trait OAuthService extends Service {

      import unfiltered.request._
      import unfiltered.response._
      import unfiltered.request.{HttpRequest => Req}

      def errorUri(err: String) = None

      def login[T](bundle: RequestBundle[T]): ResponseFunction[Any] = ???
//        utils.Scalate(bundle.request, "login.jade")

      def requestAuthorization[T](bundle: RequestBundle[T]): ResponseFunction[Any] = Ok

      def invalidRedirectUri[T](req: HttpRequest[T], uri: Option[String], client: Option[Client]) = {
        ResponseString("missing or invalid redirect_uri")
      }

      def invalidClient[T](req: HttpRequest[T]) = ResponseString("invalid client")

      def resourceOwner[T](req: Req[T]): Option[ResourceOwner] = ??? /* Not for password requests */

      def resourceOwner(userName: String, password: String): Option[ResourceOwner] =
        Façade.oauthService.authUser(userName, password) map {
          uId =>
            new ResourceOwner {
              val password = None
              val id = uId
            }
        }

      def accepted[T](r: Req[T]) = {
        // would normally inspect the request for user approval here
        true
      }

      def denied[T](r: Req[T]) = {
        // would normally inspect the reuqest for user denial here
        false
      }

      def validScopes(scopes: Seq[String]) = true

      def validScopes[T](owner: ResourceOwner, scopes: Seq[String], req: Req[T]) = {
        // would normally validate that the scopes are valid for the owner here
        true
      }
    }

    object OAuthorizationServer
      extends AuthorizationServer
      with Clients with Tokens with OAuthService/* with DefaultAuthorizationPaths {

      import unfiltered.oauth2.ResourceOwner
      import OAuthorization._
      import AuthorizationServer._

      val log = Logger("oadmin.authorizationServer")

      object User {
        def unapply(owner: Option[ResourceOwner]) =
          owner flatMap(o => Façade.oauthService.getUser(o.id))
      }

      override def apply(r: AccessRequest): AccessResponse = r match {
        case PasswordRequest(userName, password, clientId, clientSecret, scope) =>
          client(clientId, Some(clientSecret)) match {
            case Some(c) =>
              resourceOwner(userName, password) match {
                case User(owner) =>

                  if(owner.passwordValid) {

                    val tok = generatePasswordToken(new ResourceOwner { val id = owner.id.get.toString; val password = None}, c, scope)
                    AccessTokenResponse(
                      tok.value, tok.tokenType, tok.expiresIn, tok.refresh, scope, None, tok.extras
                    )
                  }

                  else ErrorResponse("changepasswd", owner.id.get.toString)

                case _ => ErrorResponse(
                  InvalidRequest, UnauthorizedClient, errorUri(InvalidClient), None)
              }
            case _ => ErrorResponse(
              InvalidRequest, UnknownClientMsg, errorUri(InvalidClient), None)
          }

        case x => super.apply(x)
      }
    }*/

    val auth = OAuthorizationServer
  }

  class OAdminAuthSource extends AuthSource {
    def authenticateToken[T](accessToken: AccessToken, request: HttpRequest[T]): Either[String, (ResourceOwner, String, Seq[String])] =
      accessToken match {
        case MacAuthToken(key, secret, nonce, _, _) =>

          val uA = unfiltered.request.UserAgent.unapply(request).get

          val params = Map(
            "bearerToken" -> key,
            "userAgent" -> uA
          )

          /*
          *   TODO: make sure nonce has not been used . . . ?
          *
          * */
          Façade.oauthService.getUserSession(params) match {

            case Some(
              domain.Session(_, _, clientId, issuedTime, expiresIn, _, _, _,
                domain.User(Some(userId), _, _, _, _, _, _, _, _, _, _, _, _, _, _, _), userAgent, _, _, scopes)
            ) if userAgent == uA =>

              expiresIn match {
                case Some(expiry) =>

                  if (issuedTime + expiry * 1000 > System.currentTimeMillis)
                    Right((new ResourceOwner { val password = None; val id = userId.toString }, clientId, scopes.toSeq))
                  else {

                    // TODO: spawn expired tokens deletion service
                    Left("Token Expired")
                  }

                case _ =>
                  Right((new ResourceOwner { val password = None; val id = userId.toString }, clientId, scopes.toSeq))
              }

            case _ => Left("Bad Token")
          }

        case _ => Left("Bad Token")
      }

    override def realm: Option[String] = Some("schola")
  }

  object Token {
    object BearerHeader {
      val HeaderPattern = """Bearer ([\w\d!#$%&'\(\)\*+\-\.\/:<=>?@\[\]^_`{|}~\\,;]+)""".r

      def unapply(hval: String) = hval match {
        case HeaderPattern(token) => Some(token)
        case _ => None
      }
    }

    object BearerParam
      extends unfiltered.request.Params.Extract("access_token", unfiltered.request.Params.first ~> unfiltered.request.Params.nonempty)

    def unapply[T](req: unfiltered.request.HttpRequest[T]) = req match {
      case unfiltered.mac.MacAuthorization(id, _, _, _, _) => Some(id)
      case unfiltered.request.Authorization(BearerHeader(token)) => Some(token)
      case unfiltered.request.Params(BearerParam(token)) => Some(token)
      case _ => None
    }
  }

  case class OAuth2Protection(source: AuthSource) extends ProtectionLike {

    object OAuth2MacAuth extends MacAuth {
      val algorithm = MACAlgorithm
      def tokenSecret(key: String) = Façade.oauthService.getTokenSecret(key)
    }

    override def fallback = {
      case null => null
    }

    val schemes = Seq(OAuth2MacAuth)
  }
}