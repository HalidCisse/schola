package controllers

import play.api.mvc._

import play.api.data.Form
import play.api.data.Forms._

import play.api.Play.current

import com.typesafe.plugin._

import schola.oadmin._, domain._, cl.SessionSupport, conversions.json._
import play.api.libs.iteratee.{ Iteratee, Enumerator }

/**
 * A controller to handle user registration.
 *
 */
object Profile extends Controller with Helpers {

  val CurrentPassword = "password"
  val Password1 = "password1"
  val Password2 = "password2"
  val PrimaryEmail = "primaryEmail"
  val GivenName = "givenName"
  val FamilyName = "familyName"
  val Gender = "gender"
  val HomeAddress = "homeAddress"
  val WorkAddress = "workAddress"
  val Contacts = "contacts"
  val Mobiles = "mobiles"
  val Mobile1 = "mobile1"
  val Mobile2 = "mobile2"
  val City = "city"
  val Country = "country"
  val PostalCode = "postalCode"
  val StreetAddress = "streetAddress"
  val Home = "home"
  val Work = "work"
  val Email = "email"
  val PhoneNumber = "phoneNumber"
  val Fax = "fax"

  case class UserInfo(
    primaryEmail: String,
    password: Option[String],
    givenName: String,
    familyName: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Contacts)

  implicit def UserToInfo(user: domain.User) = UserInfo(user.primaryEmail, None, user.givenName, user.familyName, user.gender, user.homeAddress, user.workAddress, user.contacts)

  val form = Form[UserInfo](
    mapping(
      PrimaryEmail -> email,
      CurrentPassword -> optional(tuple(
        Password1 -> nonEmptyText(minLength = schola.oadmin.PasswordMinLength),
        Password2 -> nonEmptyText).verifying("Passwords do not match", passwords => passwords._1 == passwords._2)),
      GivenName -> nonEmptyText,
      FamilyName -> nonEmptyText,
      Gender -> nonEmptyText.transform(domain.Gender.withName, (_: Gender).toString),
      HomeAddress -> optional(mapping(
        City -> text,
        Country -> text,
        PostalCode -> text,
        StreetAddress -> text)(AddressInfo)(AddressInfo.unapply)),
      WorkAddress -> optional(mapping(
        City -> text,
        Country -> text,
        PostalCode -> text,
        StreetAddress -> text)(AddressInfo)(AddressInfo.unapply)),
      Contacts -> mapping(
        Mobiles -> mapping(
          Mobile1 -> optional(text),
          Mobile2 -> optional(text))(MobileNumbers)(MobileNumbers.unapply),
        Home -> optional(mapping(
          Email -> optional(email),
          PhoneNumber -> optional(text),
          Fax -> optional(text))(ContactInfo)(ContactInfo.unapply)),
        Work -> optional(mapping(
          Email -> optional(email),
          PhoneNumber -> optional(text),
          Fax -> optional(text))(ContactInfo)(ContactInfo.unapply)))(domain.Contacts)(domain.Contacts.unapply)) {

        (primaryEmail, passwords, givenName, familyName, gender, homeAddress, workAddress, contacts) => UserInfo(primaryEmail, passwords map (_._1), givenName, familyName, gender, homeAddress, workAddress, contacts)

      } { (userInfo: UserInfo) => Some(userInfo.primaryEmail, Some("", ""), userInfo.givenName, userInfo.familyName, userInfo.gender, userInfo.homeAddress, userInfo.workAddress, userInfo.contacts) })

  def edit = Action.async {
    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].session(sessionKey, userAgent) map {
            session =>

              if (session.user.changePasswordAtNextLogin)
                Redirect(routes.Passwords.changePage(required = true))

              else Ok(views.html.editprofile(form.fill(session.user)))

          } recover {
            case _: Throwable =>

              Redirect(routes.LoginPage.index)
                .discardingCookies(DiscardingCookie(SESSION_KEY))
          }

        case _ =>

          scala.concurrent.Future.successful(Redirect(routes.LoginPage.index))
      }
  }

  def update = Action.async {

    implicit request =>

      form.bindFromRequest.fold(
        errors => {

          scala.concurrent.Future.successful {
            BadRequest(views.html.editprofile(errors))
          }

        }, info => {

          getToken match {

            case Some(sessionKey) =>

              val UserInfo(
                primaryEmail,
                password,
                givenName,
                familyName,
                gender,
                homeAddress,
                workAddress,
                contacts) = info

              use[SessionSupport].updateAccount(
                sessionKey,
                userAgent,
                primaryEmail,
                password,
                givenName,
                familyName,
                gender,
                homeAddress,
                workAddress,
                contacts) map {
                  success =>

                    if (success)

                      Redirect(routes.Application.index)
                        .flashing("success" -> "Account updated.")

                    else

                      BadRequest(views.html.editprofile(form.fill(info)))
                        .flashing("error" -> "Update failed. Please try again.")
                }

            case _ =>

              scala.concurrent.Future.successful {
                Redirect(routes.LoginPage.index)
              }
          }
        })
  }

  def uploadAvatar(filename: String) = Action.async(parse.temporaryFile) {
    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          { Enumerator.fromFile(request.body.file) |>>> Iteratee.consume[Array[Byte]]() } flatMap {
            bytes =>

              use[SessionSupport].setAvatar(sessionKey, userAgent, filename, request.contentType, bytes) map {
                success => json[domain.Response](Response(success = success))
              } recover {
                case ex: Throwable =>
                  json[domain.Response](Response(success = false))
              }
          }

        case _ =>

          scala.concurrent.Future.successful(json[domain.Response](Response(success = false)))
      }
  }

  def downloadAvatar = Action.async {
    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].getAvatar(sessionKey, userAgent) map {
            json[domain.AvatarInfo]
          } recover {
            case _: Throwable => NotFound
          }

        case _ =>

          scala.concurrent.Future.successful(NotFound)
      }
  }

  def purgeAvatar = Action.async {
    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].purgeAvatar(sessionKey, userAgent) map {
            success => json[domain.Response](Response(success = success))
          } recover {
            case _: Throwable => json[domain.Response](Response(success = false))
          }

        case _ =>

          scala.concurrent.Future.successful(json[domain.Response](Response(success = false)))
      }
  }
}
