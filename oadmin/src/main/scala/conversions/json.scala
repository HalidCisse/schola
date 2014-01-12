package schola
package oadmin
package conversions

package object json {

  import domain._

  import org.json4s._
  import org.json4s.native.Serialization

  val userSerializer = FieldSerializer[User](FieldSerializer.ignore("_deleted") orElse FieldSerializer.ignore("password"))

  val tokenSerializer = FieldSerializer[OAuthToken](
    FieldSerializer.ignore("macKey") orElse FieldSerializer.ignore("refreshExpiresIn") orElse FieldSerializer.ignore("tokenType"))

  class UUIDSerializer extends Serializer[java.util.UUID] {
    val UUIDClass = classOf[java.util.UUID]

    def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), java.util.UUID] = {
      case (t @ TypeInfo(UUIDClass, _), json) =>
        json match {
          case JString(s) => java.util.UUID.fromString(s)
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
      new jdbc.EnumNameSerializer(Gender) +
      userSerializer +
      tokenSerializer +
      new UUIDSerializer

  def tojson[A <: AnyRef](obj: A) = Serialization.write(obj)
}