package ma.epsilon.schola
package conversions

package jdbc

object `package` {

  import ma.epsilon.schola.jdbc.Q._

  import domain._
  import school.domain._

  import play.api.libs.json.{Json, Format}
  import json._

  def setTypeMapper[T: scala.reflect.ClassTag: Format] =
    MappedColumnType.base[Set[T], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[Set[T]])

  def listTypeMapper[T: scala.reflect.ClassTag: Format] =
    MappedColumnType.base[List[T], String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[List[T]])

  implicit val scopesTypeMapper = listTypeMapper[Scope]
  implicit val accessRightsTypeMapper = setTypeMapper[AccessRight]

  def jsonTypeMapper[T: scala.reflect.ClassTag: Format] =
    MappedColumnType.base[T, String](
      ts => Json.stringify(Json.toJson(ts)),
      s => Json.parse(s).as[T])  

  implicit val contactsTypeMapper = jsonTypeMapper[Contacts]
  implicit val contactInfoTypeMapper = jsonTypeMapper[ContactInfo]
  implicit val addressInfoTypeMapper = jsonTypeMapper[AddressInfo]
  implicit val dayOfWeekTypeMapper = jsonTypeMapper[java.time.DayOfWeek]
  implicit val monthDayTypeMapper = jsonTypeMapper[java.time.MonthDay]
}