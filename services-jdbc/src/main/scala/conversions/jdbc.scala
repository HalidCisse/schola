package schola
package oadmin
package conversions

package object jdbc {

  import schema.Q._

  import domain._

  import play.api.libs.json.Json
  import conversions.json._

  /*class EnumNameSerializer[E <: Enumeration: scala.reflect.ClassTag](enum: E)
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

  implicit lazy val formats =
    Serialization.formats(
      ShortTypeHints(List())) + new EnumNameSerializer(Gender)*/

  implicit val enumGenderMapper = MappedColumnType.base[Gender, String](_.toString, Gender.withName)

  implicit val setTypeMapper =
    MappedColumnType.base[Set[String], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Set[String]])

  implicit val addressInfoTypeMapper =
    MappedColumnType.base[AddressInfo, String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[AddressInfo])

  implicit val contactsTypeMapper =
    MappedColumnType.base[Contacts, String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Contacts])
}