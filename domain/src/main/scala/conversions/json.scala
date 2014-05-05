package ma.epsilon.schola
package conversions.json

import play.api.data.validation.ValidationError

object `package` {

  import domain._

  import play.api.libs.json._

  implicit val contactInfoFormat = Json.format[ContactInfo]
  implicit val addressInfoFormat = Json.format[AddressInfo]
  implicit val mobileNumbersFormat = Json.format[MobileNumbers]
  implicit val contactsFormat = Json.format[Contacts]
  implicit val usersStatsFormat = Json.format[UsersStats]
  implicit val avatarInfoFormat = Json.format[AvatarInfo]

  implicit val genderFormat = new Format[Gender] {

    def reads(js: JsValue): JsResult[Gender] = js match {
      case JsString("Female") => JsSuccess(Gender.Female)
      case _                  => JsSuccess(Gender.Male)
    }

    def writes(gender: Gender): JsValue = JsString(gender.toString)
  }

  implicit val uuidFormat = new Format[java.util.UUID] {

    def reads(js: JsValue): JsResult[java.util.UUID] = js match {
      case JsString(str) =>
        try JsSuccess(java.util.UUID.fromString(str)) catch {
          case _: Exception => JsError(ValidationError("Invalid UUID"))
        }
    }

    def writes(uuid: java.util.UUID): JsValue = JsString(uuid.toString)
  }

  implicit val scopeFormat = Json.format[Scope]
  implicit val accessRightsFormat = Json.format[AccessRight]
  implicit val appsFormat = Json.format[domain.App]
  implicit val userAccessRightsFormat = Json.format[UserAccessRight]

  implicit val profileFormat = Json.format[Profile]
  implicit val userFormat = Json.format[User]
  implicit val sessionFormat = Json.format[Session]
  implicit val responseFormat = Json.format[Response]

  implicit val labelFormat = Json.format[Label]
  implicit val userLabelFormat = Json.format[UserLabel]
}