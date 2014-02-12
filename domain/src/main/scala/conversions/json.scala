package schola
package oadmin
package conversions
package json

import play.api.data.validation.ValidationError

/*package object json {

  import domain._

  import org.json4s._
  import org.json4s.native.Serialization

  class EnumNameSerializer[E <: Enumeration: scala.reflect.ClassTag](enum: E)
      extends Serializer[E#Value] {

    import JsonDSL._

    val EnumerationClass = classOf[E#Value]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), E#Value] = {
      case (t @ TypeInfo(EnumerationClass, _), json) if isValid(json) =>
        json match {
          case JString(value) => (enum withName value).asInstanceOf[E#Value]
          case value          => throw new MappingException(s"Can't convert $value to $EnumerationClass")
        }
    }

    private[this] def isValid(json: JValue) = json match {
      case JString(value) if enum.values.exists(_.toString == value) => true
      case _ => false
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case i: E#Value => string2jvalue(i.toString)
    }
  }

  val userSerializer = FieldSerializer[User]( /*FieldSerializer.ignore("_deleted") orElse*/ FieldSerializer.ignore("password"))

  val tokenSerializer = FieldSerializer[OAuthToken](
    FieldSerializer.ignore("macKey") orElse FieldSerializer.ignore("refreshExpiresIn") orElse FieldSerializer.ignore("tokenType"))

  class UUIDSerializer extends Serializer[java.util.UUID] {
    val UUIDClass = classOf[java.util.UUID]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), java.util.UUID] = {
      case (t @ TypeInfo(UUIDClass, _), json) =>
        json match {
          case JString(s) => uuid(s)
          case value      => throw new MappingException(s"Can't convert $value to $UUIDClass")
        }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case i: java.util.UUID => JsonDSL.string2jvalue(i.toString)
    }
  }

  implicit val formats =
    new org.json4s.Formats {
      override val typeHintFieldName = "type"
      val dateFormat = DefaultFormats.lossless.dateFormat
      override val typeHints = ShortTypeHints(List( /* classOf[Email], classOf[PhoneNumber], classOf[Fax], classOf[HomeContactInfo], classOf[WorkContactInfo] */ ))
    } +
      new EnumNameSerializer(Gender) +
      userSerializer +
      tokenSerializer +
      new UUIDSerializer

  def tojson[A <: AnyRef](obj: A) = Serialization.write(obj)
}*/

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

  implicit val userFormat = Json.format[User]
  implicit val sessionFormat = Json.format[Session]
  implicit val roleFormat = Json.format[Role]
  implicit val permissionFormat = Json.format[Permission]
  implicit val responseFormat = Json.format[Response]

  implicit val rolePermissionFormat = Json.format[RolePermission]
  implicit val userRoleFormat = Json.format[UserRole]
  implicit val labelFormat = Json.format[Label]
  implicit val userLabelFormat = Json.format[UserLabel]
}