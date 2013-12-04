package schola
package oadmin
package conversions

import scala.reflect.ClassTag

package object jdbc {

  import slick.jdbc.MappedJdbcType
  import slick.driver.JdbcDriver.simple._

  import domain._

  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization._

  class EnumNameSerializer[E <: Enumeration : ClassTag](enum: E)
    extends Serializer[E#Value] {

    import JsonDSL._

    val EnumerationClass = classOf[E#Value]

    def deserialize(implicit format: Formats):
    PartialFunction[(TypeInfo, JValue), E#Value] = {
      case (t@TypeInfo(EnumerationClass, _), json) if isValid(json) =>
        json match {
          case JString(value) => (enum withName value).asInstanceOf[E#Value]
          case value => throw new MappingException(s"Can't convert $value to $EnumerationClass")
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
      ShortTypeHints(List(classOf[Email], classOf[PhoneNumber], classOf[Fax], classOf[HomeContactInfo], classOf[WorkContactInfo], classOf[MobileContactInfo]))
      ) + new EnumNameSerializer(domain.Gender)

  abstract class DBEnum extends Enumeration {
    implicit val enumMapper = MappedJdbcType.base[Value, String](_.toString, this.withName)
  }

  implicit val setTypeMapper =
    MappedJdbcType.base[Set[String], String](
      ts => write(ts),
      read[Set[String]]
    )

  implicit val addressInfoTypeMapper =
    MappedJdbcType.base[AddressInfo, String](
      ts => write(ts),
      read[AddressInfo]
    )

  implicit val contactInfoTypeMapper =
    MappedJdbcType.base[Set[ContactInfo], String](
      ts => write(ts),
      read[Set[ContactInfo]]
    )
}