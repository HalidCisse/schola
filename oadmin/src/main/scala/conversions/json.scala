package schola
package oadmin
package conversions

package object json {

  import domain._

  import org.json4s._
  import org.json4s.native.Serialization

  val userSerializer = FieldSerializer[User](FieldSerializer.ignore("_deleted") orElse FieldSerializer.ignore("password"))

  val tokenSerializer = FieldSerializer[OAuthToken](
    FieldSerializer.ignore("macKey") orElse FieldSerializer.ignore("refreshExpiresIn") orElse FieldSerializer.ignore("tokenType")
  )

  class UUIDSerializer extends Serializer[java.util.UUID] {
    val UUIDClass = classOf[java.util.UUID]

    def deserialize(implicit format: Formats):
    PartialFunction[(TypeInfo, JValue), java.util.UUID] = {
      case (t@TypeInfo(UUIDClass, _), json) =>
        json match {
          case JString(s) => java.util.UUID.fromString(s)
          case value => throw new MappingException(s"Can't convert $value to $UUIDClass")
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
      override val typeHints = ShortTypeHints(List(classOf[Email], classOf[PhoneNumber], classOf[Fax], classOf[HomeContactInfo], classOf[WorkContactInfo], classOf[MobileContactInfo]))
    } +
    new jdbc.EnumNameSerializer(Gender) +
    userSerializer +
    tokenSerializer +
    new UUIDSerializer

//  implicit val formats =
//    DefaultFormats +
//      new jdbc.EnumNameSerializer(Gender) +
//      userSerializer +
//      tokenSerializer +
//      new UUIDSerializer
//
//  type ToJson = {
//    def toJson: JValue
//  }

  def tojson[A <: AnyRef](obj: A) = Serialization.write(obj)

//  def tojson[T](obj: T with Json) = Serialization.write(obj)
//
//  def tojson[T](obj: Traversable[T with Json]) = Serialization.write(obj)
//
//  def tojson[T](obj: Option[T with Json]) = Serialization.write(obj)

/*  implicit def tojvalue[T <% ToJson](obj: T) = obj.toJson

  implicit class UserToJson(val user: domain.User) extends AnyVal {
    def toJson =
      ("id" -> user.id.get.toString) ~
        ("email" -> user.email) ~
          ("firstname" -> user.firstname) ~
            ("lastname" -> user.lastname) ~
              ("createdAt" -> user.createdAt) ~
                ("createdBy" -> (user.createdBy map (_.toString))) ~
                  ("lastModifiedAt" -> user.lastModifiedAt) ~
                    ("lastModifiedBy" -> (user.lastModifiedBy map (_.toString))) ~
                      ("gender" -> user.gender.toString) ~
                        ("homeAddress" -> (user.homeAddress map(_.toJson))) ~
                          ("workAddress" -> (user.workAddress map(_.toJson))) ~
                            ("contacts" -> tojvalue(user.contacts)) ~
                              ("avatar" -> (user.avatar map(_.toJson)))

  }

  implicit class RoleToJson(val role: domain.Role) extends AnyVal {
    def toJson =
      ("name" -> role.name) ~
        ("parent" -> role.parent) ~
          ("createdAt" -> role.createdAt) ~
            ("createdBy" -> (role.createdBy map (_.toString))) ~
              ("public" -> role.public)
  }

  implicit class PermissionToJson(val permission: domain.Permission) extends AnyVal {
    def toJson =
      ("name" -> permission.name) ~
        ("clientId" -> permission.clientId)
  }

  implicit class UserRoleToJson(val userRole: domain.UserRole) extends AnyVal {
    def toJson =
      ("userId" -> userRole.userId.toString) ~
        ("role" -> userRole.role) ~
          ("grantedAt" -> userRole.grantedAt) ~
            ("grantedBy" -> (userRole.grantedBy map(_.toString)))
  }

  implicit class RolePermissionToJson(val rolePermission: domain.RolePermission) extends AnyVal {
    def toJson =
      ("role" -> rolePermission.role) ~
        ("permission" -> rolePermission.permission) ~
          ("grantedAt" -> rolePermission.grantedAt) ~
            ("grantedBy" -> (rolePermission.grantedBy map(_.toString)))
  }

  implicit class OAuthTokenToJson(val token: domain.OAuthToken) extends AnyVal {
    def toJson =
      ("key" -> token.accessToken) ~
       ("issuedTime" -> token.createdAt) ~
         ("expiresIn" -> token.expiresIn) ~
           ("lastAccessTime" -> token.lastAccessTime) ~
            ("userAgent" -> token.uA) ~
              ("scopes" -> token.scopes)

  }

  implicit class SessionToJson(val session: domain.Session) extends AnyVal {
    def toJson =
      ("key" -> session.key) ~
        ("secret" -> session.secret) ~
          ("clientId" -> session.clientId) ~
            ("refresh" -> session.refresh) ~
              ("issuedTime" -> session.issuedTime) ~
                ("expiresIn" -> session.expiresIn) ~
                  ("refreshExpiresIn" -> session.refreshExpiresIn) ~
                   ("lastAccessTime" -> session.lastAccessTime) ~
                    ("userAgent" -> session.userAgent) ~
                      ("user" -> session.user.toJson) ~
                        ("roles" -> session.roles) ~
                          ("permissions" -> session.permissions) ~
                            ("scopes" -> session.scopes)

  }

  implicit class ContactValueToJson(val contactValue: domain.ContactValue) extends AnyVal {
    def toJson = contactValue match {
      case domain.Email(email)        => ("type" -> "Email") ~ ("email" -> email)
      case domain.Fax(fax)            => ("type" -> "Fax") ~ ("email" -> fax)
      case domain.PhoneNumber(number) => ("type" -> "PhoneNumber") ~ ("email" -> number)
    }
  }

  implicit class ContactInfoToJson(val contactInfo: domain.ContactInfo) extends AnyVal {
    def toJson = contactInfo match {
      case domain.HomeContactInfo(value)    =>  ("type" -> "HomeContactInfo") ~ ("home" -> value.toJson)
      case domain.WorkContactInfo(value)    =>  ("type" -> "WorkContactInfo") ~ ("work" -> value.toJson)
      case domain.MobileContactInfo(value)  =>  ("type" -> "MobileContactInfo") ~ ("mobile" -> value.toJson)
    }
  }

  implicit class AddressInfoToJson(val addressInfo: domain.AddressInfo) extends AnyVal {
    def toJson =
      ("city" -> addressInfo.city) ~
        ("country" -> addressInfo.country) ~
          ("zipCode" -> addressInfo.zipCode) ~
            ("addressLine" -> addressInfo.addressLine)
  }

  implicit class AvatarInfoToJson(val avatarInfo: domain.AvatarInfo) extends AnyVal {
      def toJson =
        ("contentType" -> avatarInfo.contentType) ~
          ("created" -> avatarInfo.created)
  }*/
}