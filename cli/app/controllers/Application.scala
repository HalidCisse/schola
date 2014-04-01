package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import ma.epsilon.schola._, cli.SessionSupport

import com.typesafe.plugin._

import scala.concurrent.Future

import play.api.Play.current
import play.api.libs.json.{ Json, Writes }

trait Helpers {
  this: Controller =>

  @inline def json[C: Writes](content: Any) = Ok(Json.toJson(content.asInstanceOf[C]))

  def userAgent(implicit request: RequestHeader) = request.headers.get("User-Agent").getOrElse("")

  def getToken(implicit request: RequestHeader) =
    request.cookies.get(SESSION_KEY)
      .flatMap(c => utils.Crypto.extractSignedToken(c.value))
}

/**
 * Main application controller.
 */
object Application extends Controller with Helpers {

  import play.api.libs.json.Json

  def session = Action.async {
    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          import play.api.libs.json.Json
          import conversions.json._

          use[SessionSupport].session(sessionKey, userAgent) map {
            session =>

              def ok = json[domain.Session](session)

              if (sessionKey eq session.key) ok
              else
                ok.withCookies(
                  Cookie(
                    SESSION_KEY,
                    utils.Crypto.signToken(session.key),
                    maxAge = if (request.cookies.get("_session_rememberMe").exists(_.value == "remember-me")) session.refreshExpiresIn map (_.toInt) else None,
                    httpOnly = true))

          } recover {
            case scala.util.control.NonFatal(_) =>

              Ok(Json.obj("error" -> true))
                .discardingCookies(DiscardingCookie(SESSION_KEY))
          }

        case _ =>

          Future.successful {
            Ok(Json.obj("error" -> true))
              .discardingCookies(DiscardingCookie(SESSION_KEY))
          }
      }
  }

  /**
   * Redirects to the login page if no session.
   */
  def index = Action.async {

    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].session(sessionKey, userAgent) map {
            session =>

              if (session.changePasswordAtNextLogin)
                Redirect(routes.Passwords.changePage(required = true))

              else Ok(views.html.index())

          } recover {
            case scala.util.control.NonFatal(_) =>

              Redirect(routes.LoginPage.index)
                .discardingCookies(DiscardingCookie(SESSION_KEY))
          }

        case _ =>

          Future.successful(Redirect(routes.LoginPage.index))
      }
  }
}
