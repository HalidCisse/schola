package ma.epsilon.schola
package conversions.json

import play.api.data.validation.ValidationError

object `package` {

  import domain._

  import play.api.libs.json._

  import java.time.{LocalDateTime, LocalDate, LocalTime, Duration, DayOfWeek, Instant, MonthDay}

  implicit val monthDayFormat = new Format[MonthDay] {

    val df = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

    def reads(js: JsValue): JsResult[MonthDay] = js match {
      case JsString(s)   => JsSuccess(MonthDay.parse(s, df))
      case _             => JsError("invalid.monthday")
    }

    def writes(monthDay: MonthDay): JsValue = JsString(monthDay.format(df))
  }  

  implicit val dayOfWeekFormat = new Format[DayOfWeek] {

    def reads(js: JsValue): JsResult[DayOfWeek] = js match {
      case JsNumber(dayOfWeek)   => JsSuccess(DayOfWeek.of(dayOfWeek.intValue))
      case _                     => JsError("invalid.dayofweek")
    }

    def writes(dayOfWeek: DayOfWeek): JsValue = JsNumber(dayOfWeek.getValue)
  }

  implicit object durationFormat extends Format[Duration] {

    def writes(d: Duration): JsValue = JsNumber(d.getNano)

    def reads(json: JsValue): JsResult[Duration] = json match {
      case JsNumber(nanos) => JsSuccess(Duration.ofNanos(nanos.longValue))
      case _               => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }
  }  

  implicit object instantFormat extends Format[Instant] {

    def writes(d: Instant): JsValue = JsNumber(d.getEpochSecond)

    def reads(json: JsValue): JsResult[Instant] = json match {
      case JsNumber(s) => JsSuccess(Instant.ofEpochSecond(s.longValue))
      case _           => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }
  }  

  implicit object localDateTimeFormat extends Format[LocalDateTime] {

    def writes(d: LocalDateTime): JsValue = JsString(d.toString)

    def reads(json: JsValue): JsResult[LocalDateTime] = json match {
      case JsString(s) => parseDate(s) match {
        case Some(d) => JsSuccess(d)
        case None    => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[LocalDateTime] =
      scala.util.control.Exception.allCatch[LocalDateTime] opt (LocalDateTime.parse(input))
  }

  implicit object localDateFormat extends Format[LocalDate] {

    def writes(d: LocalDate): JsValue = JsString(d.toString)

    def reads(json: JsValue): JsResult[LocalDate] = json match {
      case JsString(s) => parseDate(s) match {
        case Some(d) => JsSuccess(d)
        case None    => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[LocalDate] =
      scala.util.control.Exception.allCatch[LocalDate] opt (LocalDate.parse(input))
  }

  /* -------------------------------------------------------------------------------------------------------- */

  implicit object localTimeFormat extends Format[LocalTime] {

    def writes(d: LocalTime): JsValue = JsString(d.toString)

    def reads(json: JsValue): JsResult[LocalTime] = json match {
      case JsString(s) => parseDate(s) match {
        case Some(d) => JsSuccess(d)
        case None    => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jodadate.format"))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.date"))))
    }

    private def parseDate(input: String): Option[LocalTime] =
      scala.util.control.Exception.allCatch[LocalTime] opt (LocalTime.parse(input))
  }  

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
          case _: Exception => JsError(ValidationError("invalid.uuid"))
        }
    }

    def writes(uuid: java.util.UUID): JsValue = JsString(uuid.toString)
  }

  implicit val scopeFormat = Json.format[Scope]
  implicit val accessRightsFormat = Json.format[AccessRight]
  implicit val appsFormat = Json.format[domain.App]
  implicit val userAccessRightsFormat = Json.format[UserAccessRight]

  implicit val profileFormat = Json.format[Profile]
  implicit val userFormat = Json.format[User]
  implicit val sessionFormat = Json.format[Session]
  implicit val responseFormat = Json.format[Response]

  implicit val labelFormat = Json.format[Label]
  implicit val userLabelFormat = Json.format[UserLabel]
}