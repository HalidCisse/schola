package controllers

import play.api.mvc.{ DiscardingCookie, Action, Controller, Cookie }
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.typesafe.plugin._

import schola.oadmin._, cli.SessionSupport

import play.api.data.Form
import play.api.data.Forms._

import scala.concurrent.Future

/**
 * The Login page controller
 */
object LoginPage extends Controller with Helpers {

  /**
   * Renders the login page
   * @return
   */
  def index = Action {

    implicit request =>

      getToken match {

        case Some(_) =>

          Redirect(routes.Application.index)

        case _ =>

          Ok(views.html.login())
      }
  }

  val loginForm = Form(
    tuple(
      "username" -> email,
      "password" -> nonEmptyText(minLength = schola.oadmin.PasswordMinLength),
      "rememberMe" -> optional(text)))

  /**
   * Logs in user
   * @return
   */

  def login = Action.async {

    implicit request =>

      loginForm.bindFromRequest.fold(
        _ => {

          Future.successful {
            Redirect(routes.LoginPage.index)
              .flashing("error" -> "Login failed; invalid username or password.")
          }

        }, form => {

          val (username, password, rememberMe) = form

          use[SessionSupport].login(username, password, userAgent) map { session =>

            if (session.user.suspended)

              Redirect(routes.LoginPage.index)
                .discardingCookies(DiscardingCookie(SESSION_KEY))
                .withCookies(
                  Cookie(
                    "_session_rememberMe",
                    "remember-me", maxAge = Some(if (rememberMe.isDefined) 31536000 /* 1 year */ else 0 /* expire it */ ),
                    httpOnly = true))
                .flashing("error" -> "Login failed; account disabled.")

            else

              Redirect(routes.Application.index)
                .withCookies(
                  Cookie(
                    SESSION_KEY,
                    utils.Crypto.signToken(session.key),
                    maxAge = if (rememberMe.isDefined) session.refreshExpiresIn map (_.toInt) else None,
                    httpOnly = true),
                  Cookie(
                    "_session_rememberMe",
                    "remember-me", maxAge = Some(if (rememberMe.isDefined) 31536000 /* 1 year */ else 0 /* expire it */ ),
                    httpOnly = true))

          } recover {
            case _: Throwable =>

              Redirect(routes.LoginPage.index)
                .flashing("error" -> "Login failed; invalid username or password.")
          }
        })
  }

  /**
   * Logs out the user by clearing the credentials from the session.
   * The browser is redirected either to the login page.
   *
   * @return
   */
  def logout = Action.async {

    implicit request =>

      val result =
        Redirect(routes.Application.index)
          .discardingCookies(DiscardingCookie(SESSION_KEY))

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].logout(sessionKey)
            .map(_ => result)
            .recover {
              case _: Throwable => result
            }

        case _ =>

          Future.successful {
            result
          }
      }
  }
}
