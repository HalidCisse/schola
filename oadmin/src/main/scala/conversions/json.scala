package schola
package oadmin
package conversions

package object json {

  import domain._

  import org.json4s._
  import org.json4s.native.Serialization

  val userSerializer = FieldSerializer[User](FieldSerializer.ignore("_deleted") orElse FieldSerializer.ignore("password"))

  class UUIDSerializer extends Serializer[java.util.UUID] {

    import JsonDSL._

    val UUIDClass = classOf[java.util.UUID]

    def deserialize(implicit format: Formats):
    PartialFunction[(TypeInfo, JValue), java.util.UUID] = {
      case (t@TypeInfo(UUIDClass, _), json) =>
        json match {
          case JString(value) => java.util.UUID.fromString(value)
          case value => throw new MappingException(s"Can't convert $value to $UUIDClass")
        }
    }

    def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
      case i: java.util.UUID => string2jvalue(i.toString)
    }
  }

  implicit lazy val formats =
    Serialization.formats(
      ShortTypeHints(List(classOf[Email], classOf[PhoneNumber], classOf[Fax], classOf[HomeContactInfo], classOf[WorkContactInfo], classOf[MobileContactInfo]))
    ) + new jdbc.EnumNameSerializer(Gender) + userSerializer + new UUIDSerializer

  private[this] def wrap[T <% JValue](value: T) = value

  implicit def tojson[T](v: T) = Serialization.write(v.asInstanceOf[AnyRef])

//  implicit def user2json(user: User) = Serialization.write(user)
//  implicit def role2json(role: Role) = Serialization.write(role)
//  implicit def permission2json(permission: Permission) = Serialization.write(permission)
//  implicit def userrole2json(userRole: UserRole) = Serialization.write(userRole)
//  implicit def rolepermission2json(rolePermission: RolePermission) = Serialization.write(rolePermission)
}