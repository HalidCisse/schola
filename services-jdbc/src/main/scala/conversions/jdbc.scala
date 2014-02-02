package schola
package oadmin
package conversions

package object jdbc {

  import schema.Q._

  import domain._

  import org.json4s._
  import org.json4s.native.Serialization
  import org.json4s.native.Serialization._

  implicit lazy val formats =
    Serialization.formats(
      ShortTypeHints(List())) + new json.EnumNameSerializer(Gender)

  implicit val enumGenderMapper = MappedColumnType.base[Gender, String](_.toString, Gender.withName)

  implicit val setTypeMapper =
    MappedColumnType.base[Set[String], String](
      ts => write(ts),
      read[Set[String]])

  implicit val addressInfoTypeMapper =
    MappedColumnType.base[AddressInfo, String](
      ts => write(ts),
      read[AddressInfo])

  implicit val contactsTypeMapper =
    MappedColumnType.base[Contacts, String](
      ts => write(ts),
      read[Contacts])
}