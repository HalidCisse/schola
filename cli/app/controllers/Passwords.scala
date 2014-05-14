package controllers

import play.api.mvc.{ Controller, Action }
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data.Form
import play.api.data.Forms._

import ma.epsilon.schola._, cli.SessionSupport

import com.typesafe.plugin._

import scala.util.control.NonFatal

/**
 * A controller to provide password change functionality
 */
object Passwords extends Controller with Helpers {

  val Username = "username"
  val Challenge = "recaptcha_challenge_field"
  val Response = "recaptcha_response_field"
  val Required = "required"
  val NewPassword = "newPassword"
  val CurrentPassword = "password"
  val Key = "key"
  val Password1 = "password1"
  val Password2 = "password2"

  def changePage(required: Boolean) = Action {
    implicit request => Ok(views.html.changepasswd(changeForm fill ChangeInfo("", "", required)))
  }

  def lostPage = Action {
    implicit request => Ok(views.html.lostpasswd(use[SessionSupport].PublicKey))
  }

  def resetPage(login: String, key: String) = Action.async {
    implicit request =>

      use[SessionSupport].checkActivationReq(login, key) map { valid =>

        if (valid)
          Ok(views.html.resetpasswd(login, key))

        else
          Redirect(routes.Application.index)
            .flashing("error" -> "Invalid request.")
      }
  }

  case class ChangeInfo(password: String, newPassword: String, required: Boolean)

  val changeForm = Form[ChangeInfo](
    mapping(
      CurrentPassword -> nonEmptyText,
      NewPassword ->
        tuple(
          Password1 -> nonEmptyText(minLength = ma.epsilon.schola.PasswordMinLength),
          Password2 -> nonEmptyText).verifying("Passwords do not match", passwords => passwords._1 == passwords._2),
      Required -> default(boolean, false))((currentPassword, newPassword, required) => ChangeInfo(currentPassword, newPassword._1, required))((changeInfo: ChangeInfo) => Some("", ("", ""), changeInfo.required)))

  def change = Action.async {

    implicit request =>

      changeForm.bindFromRequest.fold(
        errors => {

          scala.concurrent.Future.successful {
            BadRequest(views.html.changepasswd(errors))
          }

        }, info => {

          getToken match {

            case Some(sessionKey) =>

              val ChangeInfo(password, newPassword, required) = info

              use[SessionSupport].changePasswd(sessionKey, userAgent, password, newPassword) map {
                changed =>

                  if (changed)

                    Redirect(routes.Application.index)
                      .flashing("success" -> "Password successfully changed.")

                  else

                    Redirect(routes.Passwords.changePage(required = required))
                      .flashing("error" -> "Wrong password. Please try again.")
              }

            case _ =>

              scala.concurrent.Future.successful {
                Redirect(routes.Application.index)
              }
          }
        })
  }

  case class LostInfo(username: String, challenge: String, response: String)

  val lostForm = Form[LostInfo](
    mapping(
      Username -> nonEmptyText,
      Challenge -> text,
      Response -> text)(LostInfo)(LostInfo.unapply))

  def lost = Action.async {

    implicit request =>

      lostForm.bindFromRequest.fold(
        _ => {

          scala.concurrent.Future.successful {
            Redirect(routes.Passwords.lostPage)
              .flashing("error" -> "Username is required.")
          }

        }, info => {

          val LostInfo(username, challenge, response) = info

          use[SessionSupport].reCaptchaVerify(challenge, response, request.remoteAddress) flatMap {
            valid =>

              if (valid)

                use[SessionSupport].createPasswdResetReq(username) map {
                  success =>

                    if (success)

                      Redirect(routes.LoginPage.index)
                        .flashing("success" -> "Your password reset request has been sent. Please check your email.")

                    else

                      Redirect(routes.Passwords.lostPage)
                        .flashing("error" -> "Unknown username.")

                } recover {
                  case NonFatal(_) =>

                    Redirect(routes.Passwords.lostPage)
                      .flashing("error" -> "An error occured. Please try again.")
                }

              else scala.concurrent.Future.successful {

                Redirect(routes.Passwords.lostPage)
                  .flashing("error" -> "Invalid captcha. Please try again.")
              }

          } recover {
            case NonFatal(_) =>

              Redirect(routes.Passwords.lostPage)
                .flashing("error" -> "Invalid captcha. Please try again.")
          }
        })
  }

  case class ResetInfo(username: String, key: String, newPassword: String)

  val resetForm = Form[ResetInfo](
    mapping(
      Username -> nonEmptyText,
      Key -> text,
      NewPassword ->
        tuple(
          Password1 -> nonEmptyText(minLength = ma.epsilon.schola.PasswordMinLength),
          Password2 -> nonEmptyText).verifying("Passwords do not match", passwords => passwords._1 == passwords._2))((username, key, newPassword) => ResetInfo(username, key, newPassword._1))(_ => Some("", "", ("", ""))))

  def reset = Action.async {

    implicit request =>

      resetForm.bindFromRequest.fold(
        errors => scala.concurrent.Future.successful {

          errors.value collect {
            case ResetInfo(username, key, _) =>

              Redirect(routes.Passwords.resetPage(username, key))
                .flashing("error" -> "Invalid password.")

          } getOrElse {
            Redirect(routes.Application.index)
          }

        }, info => {

          val ResetInfo(username, key, newPassword) = info

          use[SessionSupport].resetPasswd(username, key, newPassword) map {
            success =>

              if (success)

                Redirect(routes.LoginPage.index)
                  .flashing("success" -> "Password successfully reset.")

              else

                Redirect(routes.Passwords.resetPage(username, key))
                  .flashing("error" -> """Please try again or visit <a href="/LostPasswd">Forgot Password</a> for another password reset email.""")

          } recover {
            case NonFatal(_) =>

              Redirect(routes.Passwords.resetPage(username, key))
                .flashing("error" -> """Please try again or visit <a href="/LostPasswd">Forgot Password</a> for another password reset email.""")
          }
        })
  }
}
