package schola
package oadmin
package conversions

package object jdbc {

  import schema.Q._

  import domain._

  import play.api.libs.json.Json
  import conversions.json._

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