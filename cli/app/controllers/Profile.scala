package controllers

import play.api.mvc._
import play.api.Routes

import play.api.data.Form
import play.api.data.Forms._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.typesafe.plugin._

import ma.epsilon.schola._, domain._, cli.SessionSupport, conversions.json._
import play.api.libs.iteratee.{ Iteratee, Enumerator }

/**
 * A controller to handle user registration.
 *
 */
object Profile extends Controller with Helpers {

  val CurrentPassword = "password"
  val NewPassword = "new_password"
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
    newPassword: Option[String],
    givenName: String,
    familyName: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts])

  implicit def UserToInfo(user: domain.Profile) = UserInfo(user.primaryEmail, None, None, user.givenName, user.familyName, user.gender, user.homeAddress, user.workAddress, user.contacts)

  val form = Form[UserInfo](
    mapping(
      PrimaryEmail -> email,
      CurrentPassword -> optional(text),
      NewPassword -> optional(tuple(
        Password1 -> nonEmptyText(minLength = ma.epsilon.schola.PasswordMinLength),
        Password2 -> nonEmptyText).verifying("Passwords do not match", passwords => passwords._1 == passwords._2)),
      GivenName -> nonEmptyText,
      FamilyName -> nonEmptyText,
      Gender -> nonEmptyText.transform(domain.Gender.withName, (_: Gender).toString),
      HomeAddress -> mapping(
        City -> text,
        Country -> text,
        PostalCode -> text,
        StreetAddress -> text)((city, country, postalCode, streetAddress) => Option(AddressInfo(Option(city), Option(country), Option(postalCode), Option(streetAddress))))(addressInfo => Option(addressInfo.flatMap(_.city).getOrElse(""), addressInfo.flatMap(_.country).getOrElse(""), addressInfo.flatMap(_.postalCode).getOrElse(""), addressInfo.flatMap(_.streetAddress).getOrElse(""))),
      WorkAddress -> mapping(
        City -> text,
        Country -> text,
        PostalCode -> text,
        StreetAddress -> text)((city, country, postalCode, streetAddress) => Option(AddressInfo(Option(city), Option(country), Option(postalCode), Option(streetAddress))))(addressInfo => Option(addressInfo.flatMap(_.city).getOrElse(""), addressInfo.flatMap(_.country).getOrElse(""), addressInfo.flatMap(_.postalCode).getOrElse(""), addressInfo.flatMap(_.streetAddress).getOrElse(""))),
      Contacts -> mapping(
        Mobiles -> mapping(
          Mobile1 -> text,
          Mobile2 -> text)((mobile1, mobile2) => Option(MobileNumbers(Option(mobile1), Option(mobile2))))(mobileNumbers => Option(mobileNumbers.flatMap(_.mobile1).getOrElse(""), mobileNumbers.flatMap(_.mobile2).getOrElse(""))),
        Home -> mapping(
          Email -> optional(email),
          PhoneNumber -> text,
          Fax -> text)((email, phoneNumber, fax) => Option(ContactInfo(Option(email.getOrElse("")), Option(phoneNumber), Option(fax))))(contactInfoIn => Option(contactInfoIn.flatMap(_.email), contactInfoIn.flatMap(_.phoneNumber).getOrElse(""), contactInfoIn.flatMap(_.fax).getOrElse(""))),
        Work -> mapping(
          Email -> optional(email),
          PhoneNumber -> text,
          Fax -> text)((email, phoneNumber, fax) => Option(ContactInfo(Option(email.getOrElse("")), Option(phoneNumber), Option(fax))))(contactInfoIn => Option(contactInfoIn.flatMap(_.email), contactInfoIn.flatMap(_.phoneNumber).getOrElse(""), contactInfoIn.flatMap(_.fax).getOrElse(""))))(domain.Contacts)(domain.Contacts.unapply)) {

        (primaryEmail, password, newPasswords, givenName, familyName, gender, homeAddress, workAddress, contacts) => UserInfo(primaryEmail, password, newPasswords map (_._1), givenName, familyName, gender, homeAddress, workAddress, Option(contacts))

      } { (userInfo: UserInfo) => Some(userInfo.primaryEmail, Some(""), Some("", ""), userInfo.givenName, userInfo.familyName, userInfo.gender, userInfo.homeAddress, userInfo.workAddress, userInfo.contacts.getOrElse(domain.Contacts())) })

  def edit = Action.async {
    implicit request =>

      getToken match {

        case Some(sessionKey) =>

          use[SessionSupport].session(sessionKey, userAgent) map {
            session =>

              if (session.changePasswordAtNextLogin)
                Redirect(routes.Passwords.changePage(required = true))

              else Ok(views.html.editprofile(form.fill(session.user)))

          } recover {
            case scala.util.control.NonFatal(_) =>

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
                newPassword,
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
                newPassword,
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

              use[SessionSupport].uploadAvatar(sessionKey, userAgent, filename, request.contentType, bytes) map {
                success => json[domain.Response](Response(success = success))
              } recover {
                case scala.util.control.NonFatal(_) =>
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

          use[SessionSupport].downloadAvatar(sessionKey, userAgent) map {
            json[domain.AvatarInfo]
          } recover {
            case scala.util.control.NonFatal(_) => NotFound
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
            case scala.util.control.NonFatal(_) => json[domain.Response](Response(success = false))
          }

        case _ =>

          scala.concurrent.Future.successful(json[domain.Response](Response(success = false)))
      }
  }

  def javascriptRoutes = Action {
    implicit request =>
      Ok(
        Routes.javascriptRouter("jsRoutes")(
          routes.javascript.Profile.uploadAvatar,
          routes.javascript.Profile.downloadAvatar,
          routes.javascript.Profile.purgeAvatar)).as(JAVASCRIPT)
  }
}
