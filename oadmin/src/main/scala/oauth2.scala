package schola
package oadmin

import unfiltered.oauth2._
import unfiltered.request.HttpRequest

package object oauth2 {

  case class NotFoundException(msg: String) extends Exception

  case class BadTokenException(msg: String) extends Exception

  class AuthServerProvider extends AuthorizationProvider {

    trait Clients extends ClientStore {
      def client(clientId: String, secret: Option[String]) =
      façade.oauthService.getClient(clientId, secret getOrElse "") map {
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
      def refresh(other: Token) = façade.oauthService.exchangeRefreshToken(other.refresh getOrElse "") map {
        case domain.OAuthToken(sAccessToken, sClientId, sRedirectUri, sOwnerId, sRefreshToken, macKey, sExpires, sRefreshExpiresIn, sCreatedAt, _, sTokenType, sScopes) =>
          new Token {
            val tokenType = Some(sTokenType)
            val redirectUri = sRedirectUri
            val expiresIn = sExpires map (_.toInt)
            val owner = sOwnerId.toString
            val scopes = sScopes.toSeq
            val refresh = sRefreshToken
            val value = sAccessToken
            val clientId = sClientId
            override val extras = Map("secret" -> macKey, "issued_time" -> sCreatedAt.toString, "algorithm" -> MacAlgo)
          }
      } getOrElse (throw NotFoundException("could not refresh token"))

      def token(code: String): Option[Token] = ???

      // Returns the refreshToken for the accessToken
      def refreshToken(refreshToken: String) =
        façade.oauthService.getRefreshToken(refreshToken) map {
          case domain.OAuthToken(sAccessToken, sClientId, sRedirectUri, sOwnerId, sRefreshToken, macKey, sExpires, sRefreshExpiresIn, sCreatedAt, _, sTokenType, sScopes) =>
            new Token {
              val tokenType = Some(sTokenType)
              val redirectUri = sRedirectUri
              val expiresIn = sExpires map (_.toInt)
              val owner = sOwnerId.toString
              val scopes = sScopes.toSeq
              val refresh = sRefreshToken
              val value = sAccessToken
              val clientId = sClientId
              override val extras = Map("secret" -> macKey, "issued_time" -> sCreatedAt.toString, "algorithm" -> MacAlgo)
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

        def generateToken = util.SHA3Utils digest s"${client.id}:${owner.id}:${System.nanoTime}"
        def generateRefresh(accessToken: String) = util.SHA3Utils digest s"$accessToken:${owner.id}:${System.nanoTime}"
        def generateMacKey = util.genPasswd(s"${owner.id}:${System.nanoTime}")

        try {

          val accessToken = generateToken
          façade.oauthService.saveToken(
            accessToken, Some(generateRefresh(accessToken)), macKey = generateMacKey, client.id, client.redirectUri, owner.id, Some(AccessTokenSessionLifeTime), Some(RefreshTokenSessionLifeTime), Set(scopes : _*)
          ) match {
            case Some(domain.OAuthToken(sAccessToken, sClientId, sRedirectUri, sOwnerId, sRefreshToken, macKey, sExpires, sRefreshExpiresIn, sCreatedAt, _, sTokenType, sScopes)) =>
              new Token {
                val tokenType = Some(sTokenType)
                val redirectUri = sRedirectUri
                val expiresIn = sExpires map (_.toInt)
                val owner = sOwnerId.toString
                val scopes = sScopes.toSeq
                val refresh = sRefreshToken
                val value = sAccessToken
                val clientId = sClientId
                override val extras = Map("secret" -> macKey, "issued_time" -> sCreatedAt.toString, "algorithm" -> MacAlgo)
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

      def login[T](bundle: RequestBundle[T]): ResponseFunction[Any] = {
        // would normally show login here
        Ok
      }

      def requestAuthorization[T](bundle: RequestBundle[T]): ResponseFunction[Any] = {
        // would normally prompt for auth here
        Ok
      }

      def invalidRedirectUri[T](req: HttpRequest[T], uri: Option[String], client: Option[Client]) = {
        ResponseString("missing or invalid redirect_uri")
      }

      def invalidClient[T](req: HttpRequest[T]) = ResponseString("invalid client")

      def resourceOwner[T](req: Req[T]): Option[ResourceOwner] = req match {
        // would normally look for a resource owners session here
        case OAuthIdentity(userId, clientId, scopes) => Some(new ResourceOwner { val password = None; val id = userId })
      }

      def resourceOwner(userName: String, password: String): Option[ResourceOwner] =
      façade.oauthService.authUser(userName, password) map {
        // would normally authenticate the resource owner
        case domain.User(uId, _, _, _, _, _, _, _, _, _, _, _, _, _) =>
          new ResourceOwner {
            val password = None
            val id = uId.toString
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

    object AuthorizationServer
      extends AuthorizationServer
      with Clients with Tokens with OAuthService

    val auth = AuthorizationServer
  }

  class OAdminAuthSource extends AuthSource {
    def authenticateToken[T](accessToken: AccessToken, request: HttpRequest[T]): Either[String, (ResourceOwner, String, Seq[String])] =
      accessToken match {
        case MacAuthToken(id, secret, nonce, _, _) =>

          /*
          *   TODO: make sure nonce has not been used . . .
          *
          * */

          façade.oauthService.getToken(id) match {
            case Some(domain.OAuthToken(_, clientId, _, userId, _, _, Some(expiresIn), _, sCreatedAt, _, _, scopes)) =>
              if (sCreatedAt + expiresIn * 1000 > System.currentTimeMillis) Right((new ResourceOwner { val password = None; val id = userId.toString }, clientId, scopes.toSeq))
              else Left("Token expired")
            case Some(domain.OAuthToken(_, clientId, _, userId, _, _, None, _, _, _, _, scopes)) => Right((new ResourceOwner { val password = None; val id = userId.toString }, clientId, scopes.toSeq))
            case _ => Left("Bad token")
          }

        case _ => Left("Bad token")
      }

    override def realm: Option[String] = Some("schola")
  }

  object BearerHeader {
    val HeaderPattern = """Bearer ([\w\d!#$%&'\(\)\*+\-\.\/:<=>?@\[\]^_`{|}~\\,;]+)""".r

    def unapply(hval: String) = hval match {
      case HeaderPattern(token) => Some(token)
      case _ => None
    }
  }

  object BearerParam
    extends unfiltered.request.Params.Extract("access_token", unfiltered.request.Params.first ~> unfiltered.request.Params.nonempty)

  object Token {
    def unapply[T](req: unfiltered.request.HttpRequest[T]) = req match {
      case unfiltered.request.Authorization(BearerHeader(token)) => Some(token)
      case unfiltered.request.Params(BearerParam(token)) => Some(token)
      case unfiltered.mac.MacAuthorization(id, _, _, _, _) => Some(id)
      case _ => None
    }
  }

  case class OAuth2Protection(source: AuthSource) extends ProtectionLike {

    object OAuth2MacAuth extends MacAuth {
      val algorithm = MacAlgo
      def tokenSecret(key: String) = façade.oauthService.getTokenSecret(key)
    }

    val schemes = Seq(OAuth2MacAuth)
  }
}