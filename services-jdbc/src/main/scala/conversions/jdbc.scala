package ma.epsilon.schola
package conversions

package object jdbc {

  import schema.Q._

  import domain._

  import play.api.libs.json.Json
  import conversions.json._

  implicit val enumGenderMapper = MappedColumnType.base[Gender, String](_.toString, Gender.withName)

  implicit val stringSetTypeMapper =
    MappedColumnType.base[Seq[String], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Seq[String]])

  implicit val UUIDSeqTypeMapper =
    MappedColumnType.base[Seq[java.util.UUID], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Seq[java.util.UUID]])

  implicit val accessRightSetTypeMapper =
    MappedColumnType.base[Set[AccessRight], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Set[AccessRight]])

  implicit val scopeSeqTypeMapper =
    MappedColumnType.base[Seq[Scope], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Seq[Scope]])

  implicit val addressInfoTypeMapper =
    MappedColumnType.base[AddressInfo, String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[AddressInfo])

  implicit val contactsTypeMapper =
    MappedColumnType.base[Contacts, String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Contacts])
}