package ma.epsilon.schola
package http

import play.api.{ Plugin, Application }
import play.api.Play.current
import play.api.mvc._
import play.api.libs.json.{ Json, Writes }
import Results._

import com.typesafe.plugin._

import scala.concurrent.Future

import unfiltered.request.QParams

trait HttpHelpers {
  this: Controller =>

  @inline def json[C: Writes](content: Any) = Ok(Json.toJson(content.asInstanceOf[C]))

  @inline implicit def set[X](list: List[X]) = list.toSet
}

trait Secured {

  def errorString(status: String, description: String) =
    """error="%s" error_description="%s" """.trim format (status, description)

  /**
   * The WWW-Authenticate challenge returned to the client tin a 401 response
   * for invalid requests
   */
  val challenge: String = "Mac"

  /**
   * An error header, consisting of the challenge and possibly an error and error_description attribute
   * (this depends on the authentication scheme).
   */
  def errorHeader(error: Option[String] = None, description: Option[String] = None) = {
    val attrs = List("error" -> error, "error_description" -> description).collect {
      case (key, Some(value)) => key -> value
    }
    attrs.tail.foldLeft(
      attrs.headOption.foldLeft(challenge) {
        case (current, (key, value)) => s"""$current $key="$value" """
      }) {
        //        case (current, (key, value)) => current + s""",\n$key="$value" """
        case (current, (key, value)) => current + s""",$key="$value" """
      }
  }

  /**
   * The response for failed authentication attempts. Intended to be overridden by authentication schemes that have
   * differing requirements.
   */
  val failedAuthenticationResponse: (String => SimpleResult) = {
    msg =>
      Unauthorized(errorString("invalid_token", msg)).withHeaders("WWW-Authenticate" -> errorHeader(Some("invalid_token"), Some(msg)))
  }

  /** Return a function representing an error response */
  def errorResponse(status: Status, description: String,
                    request: RequestHeader): Future[SimpleResult] =
    Future.successful {

      (status, description) match {
        case (Unauthorized, "") => Unauthorized(challenge) withHeaders ("WWW-Authenticate" -> challenge)
        case (Unauthorized, _)  => failedAuthenticationResponse(description)
        case (BadRequest, _)    => status(errorString("invalid_request", description))
        case (Forbidden, _)     => status(errorString("insufficient_scope", description))
        case _                  => status(errorString("Unknown error", description))
      }
    }

  def tokenSecret(key: String): Option[String] = use[Façade].oauthService.getTokenSecret(key)

  /** Returns access token response to client */
  def doAuth(token: MacAuthToken, request: RequestHeader)(next: ResourceOwner => SimpleResult)(errorResp: String => SimpleResult) =
    Future.successful {

      use[AuthSource].authenticateToken(token, request) match {
        case Left(msg)                                       => errorResp(msg)
        case Right((userId, _ /*clientId*/ , _ /*scopes*/ )) => next(userId)
      }
    }

  def withAuth[A](bodyParser: BodyParser[A])(f: ResourceOwner => Request[A] => SimpleResult) =
    Action.async(bodyParser) {
      request =>

        authenticated(request) {
          id =>
            f(id)(request)
        }
    }

  final def withAuth[A >: AnyContent](f: ResourceOwner => Request[A] => SimpleResult): EssentialAction =
    withAuth(BodyParsers.parse.anyContent)(f)

  final def withAuth(f: => SimpleResult): EssentialAction =
    withAuth((_: ResourceOwner) => (_: Request[_ >: AnyContent]) => f)

  def authenticated[A](req: Request[A])(next: ResourceOwner => SimpleResult): Future[SimpleResult] = req match {

    case MacAuthorization(id, nonce, bodyhash, ext, mac) =>

      tokenSecret(id) match {

        case Some(key) =>
          // compare a signed request with the signature provided
          Mac.sign(req, nonce, ext, bodyhash, key, MACAlgorithm).fold({
            err =>
              errorResponse(Unauthorized, err, req)
          }, {
            sig =>
              if (sig == mac) doAuth(MacAuthToken(id, key, nonce, bodyhash, ext), req)(next)(failedAuthenticationResponse)
              else errorResponse(Unauthorized, "invalid MAC signature", req)
          })

        case _ =>

          errorResponse(Unauthorized, "invalid token", req)
      }

    case _ => errorResponse(Unauthorized, "invalid MAC header.", req)
  }
}

object Mac extends utils.Signing

/**
 * MAC Authorization extractor
 * See also http://tools.ietf.org/html/draft-ietf-oauth-v2-http-mac-00
 */
object MacAuthorization {
  val Id = "id"
  val Nonce = "nonce"
  val BodyHash = "bodyhash"
  val Ext = "ext"
  val MacKey = "mac"

  object MacHeader {

    import QParams._

    val NonceFormat = """^(\d+)[:](\S+)$""".r
    val KeyVal = """(\w+)="([\w|=|:|\/|.|%|-|+]+)" """.trim.r
    val keys = Id :: Nonce :: BodyHash :: Ext :: MacKey :: Nil
    val headerSpace = "MAC" + " "

    def unapply(hvals: List[String]): Option[(String, String, Option[String], Option[String], String)] =
      hvals match {
        case x :: xs if x startsWith headerSpace =>

          val map = Map(hvals map {
            _.replace(headerSpace, "")
          } flatMap {
            case KeyVal(k, v) if keys.contains(k) => Seq(k -> Seq(v))
            case e =>
              Nil
          }: _*)

          val expect = for {
            id <- lookup(Id) is nonempty("id is empty") is required("id is required")
            nonce <- lookup(Nonce) is nonempty("nonce is empty") is required("nonce is required") is
              pred({
                NonceFormat.findFirstIn(_).isDefined
              }, _ + " is an invalid format")
            bodyhash <- lookup(BodyHash) is optional[String, String]
            ext <- lookup(Ext) is optional[String, String]
            mac <- lookup(MacKey) is nonempty("mac is empty") is required("mac is required")
          } yield {
            Some(id.get, nonce.get, bodyhash.get, ext.get, mac.get)
          }

          expect(map) orFail {
            _ =>
              None
          }

        case _ => None
      }
  }

  /** @return (id, nonce, Option[bodyhash], Option[ext], mac) */
  def unapply(req: RequestHeader): Option[(String, String, Option[String], Option[String], String)] =
    req.headers.get("Authorization") match {

      case Some(value) => value.split(',').toList match {

        case MacHeader(id, nonce, bodyhash, ext, mac) =>

          Some(id, nonce, bodyhash, ext, mac)

        case _ => None
      }

      case _ => None
    }
}

case class ResourceOwner(id: String)

case class MacAuthToken(
  id: String,
  secret: String,
  nonce: String,
  bodyhash: Option[String],
  ext: Option[String])

trait AuthSource extends Plugin {
  /**
   * Given an deserialized access token and request, extract the resource owner, client id, and list of scopes
   * associated with the request, if there is an error return it represented as a string message
   * to return the the oauth client
   */
  def authenticateToken(
    token: MacAuthToken,
    request: RequestHeader): Either[String, (ResourceOwner, String, Seq[String])]

  /**
   * Auth sources which
   */
  def realm: Option[String] = None
}

class DefaultAuthSource(app: Application) extends AuthSource {

  def authenticateToken(accessToken: MacAuthToken, request: RequestHeader) =
    accessToken match {
      case MacAuthToken(key, secret, nonce, _, _) =>

        val uA = request.headers.get("User-Agent").getOrElse("")

        val params = Map(
          "bearerToken" -> key,
          "userAgent" -> uA)

        /*
        *   TODO: make sure nonce has not been used . . . ?
        *
        * */
        use[Façade].oauthService.getUserSession(params) match {

          case Some(
            domain.Session(_, _, clientId, issuedTime, expiresIn, _, _, _, _, _ /* TODO: authenticate Suspended token??? */ , _,
              domain.Profile(userId, _, _, _, _, _, _, _, _, _, _, _), userAgent, accessRights)
            ) if userAgent == uA =>

            expiresIn match {
              case Some(expirationTime) =>

                if (issuedTime + expirationTime * 1000 > System.currentTimeMillis)
                  Right(ResourceOwner(userId.toString), clientId, accessRights.flatMap(_.scopes.map(_.name)).toSeq)
                else {

                  // TODO: spawn expired tokens deletion service
                  Left("Token Expired")
                }

              case _ =>
                Right(ResourceOwner(userId.toString), clientId, accessRights.flatMap(_.scopes.map(_.name)).toSeq)
            }

          case _ => Left("Bad Token")
        }

      case _ => Left("Bad Token")
    }

  override def realm: Option[String] = Some("Mac")
}