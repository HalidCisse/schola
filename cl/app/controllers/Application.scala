package controllers

import play.api.mvc._
import schola.oadmin._, cl.SessionSupport

import com.typesafe.plugin._

import scala.concurrent.Future

import play.api.Play.current

trait ExecutionSystem{

  implicit def system = play.libs.Akka.system

  implicit def dispatcher = system.dispatcher  
}

trait Helpers extends ExecutionSystem{

  def userAgent(implicit request: RequestHeader) = request.headers.get("User-Agent").getOrElse("")

  def getToken(implicit request: RequestHeader) =
    request.cookies.get(SESSION_KEY)
      .flatMap(c => utils.Crypto.extractSignedToken(c.value))
}

/**
 * Main application controller.
 */
object Application extends Controller with Helpers {

  def session = Action.async {
    implicit request =>

      import play.api.libs.json.Json

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].getSession(sessionKey, userAgent) map {
            session =>

              def ok =
                Ok(conversions.json.tojson(session))
                  .as("application/json")

              if (sessionKey eq session.key) ok
              else
                ok.withCookies(
                  Cookie(
                    SESSION_KEY,
                    utils.Crypto.signToken(session.key),
                    maxAge = if (request.cookies.get("_session_rememberMe").exists(_.value == "remember-me")) session.refreshExpiresIn map (_.toInt) else None,
                    httpOnly = true))

          } recover {
            case ex: ScholaException =>

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

          use[SessionSupport].getSession(sessionKey, userAgent) map {
            session =>

              if (session.user.changePasswordAtNextLogin)
                Redirect(routes.Passwords.changePage(required = true))

              else Ok(views.html.index())

          } recover {
            case ex: ScholaException =>

              Redirect(routes.LoginPage.index)
                .discardingCookies(DiscardingCookie(SESSION_KEY))
          }

        case _ =>

          Future.successful(Redirect(routes.LoginPage.index))
      }
  }
}
