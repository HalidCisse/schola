package ma.epsilon.schola
package school
package conversions.json

import scala.reflect.ClassTag

object `package` {

  import domain._
  import _root_.ma.epsilon.schola.conversions.json._

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  def rangeFormat[T: ClassTag: Format] = (
    (__ \ "start").format[T] ~
    (__ \ "end").format[T])(Range[T], unlift(Range.unapply[T]))

  implicit val tsRangeFormat = rangeFormat[java.time.Instant]

  implicit val recurrenceFormat = new Format[Recurrence] {

    def reads(js: JsValue): JsResult[Recurrence] = js match {
      case JsString("Daily")   => JsSuccess(Recurrence.Daily)
      case JsString("Weekly")  => JsSuccess(Recurrence.Weekly)
      case JsString("Monthly") => JsSuccess(Recurrence.Monthly)
      case JsString("Yearly")  => JsSuccess(Recurrence.Yearly)
      case _                   => JsSuccess(Recurrence.None)
    }

    def writes(recurrence: Recurrence): JsValue = JsString(recurrence.toString)
  }

  implicit val moduleTypeFormat = new Format[ModuleType] {

    def reads(js: JsValue): JsResult[ModuleType] = js match {
      case JsString("SUBJECT")        => JsSuccess(ModuleType.SUBJECT)
      case _                          => JsSuccess(ModuleType.MODULE)
    }

    def writes(moduleType: ModuleType): JsValue = JsString(moduleType.toString)
  }  

  implicit val inscriptionStatusFormat = new Format[InscriptionStatus] {

    def reads(js: JsValue): JsResult[InscriptionStatus] = js match {
      case JsString("PendingApproval") => JsSuccess(InscriptionStatus.PendingApproval)
      case JsString("Approved")        => JsSuccess(InscriptionStatus.Approved)
      case JsString("Rejected")        => JsSuccess(InscriptionStatus.Rejected)
      case _                           => JsError("invalid.inscriptionstatus")
    }

    def writes(inscriptionStatus: InscriptionStatus): JsValue = JsString(inscriptionStatus.toString)
  }

  implicit val closureStatusFormat = new Format[ClosureStatus] {

    def reads(js: JsValue): JsResult[ClosureStatus] = js match {
      case JsString("Completed")   => JsSuccess(ClosureStatus.Completed)
      case JsString("Adjorned")    => JsSuccess(ClosureStatus.Adjorned)
      case JsString("Unspecified") => JsSuccess(ClosureStatus.Unspecified)
      case _                       => JsError("invalid.closurestatus")
    }

    def writes(closureStatus: ClosureStatus): JsValue = JsString(closureStatus.toString)
  }

  implicit val guardianRelationFormat = new Format[GuardianRelation] {

    def reads(js: JsValue): JsResult[GuardianRelation] = js match {
      case JsString("Parent")   => JsSuccess(GuardianRelation.Parent)
      case JsString("Child")    => JsSuccess(GuardianRelation.Child)
      case JsString("Sibling")  => JsSuccess(GuardianRelation.Sibling)
      case JsString("Spouse")   => JsSuccess(GuardianRelation.Spouse)
      case JsString("Relative") => JsSuccess(GuardianRelation.Relative)
      case _                    => JsSuccess(GuardianRelation.Other)
    }

    def writes(guardianRelation: GuardianRelation): JsValue = JsString(GuardianRelation.toString)
  }

  implicit val timetableEventTypeFormat = new Format[TimetableEventType] {

    def reads(js: JsValue): JsResult[TimetableEventType] = js match {
      case JsString("Lecture")                 => JsSuccess(TimetableEventType.Lecture)
      case JsString("Lab")                     => JsSuccess(TimetableEventType.Lab)
      case JsString("Seminar")                 => JsSuccess(TimetableEventType.Seminar)
      case JsString("Sports")                  => JsSuccess(TimetableEventType.Sports)
      case JsString("Break")                   => JsSuccess(TimetableEventType.Break)
      case JsString("Exam")                    => JsSuccess(TimetableEventType.Exam)
      case JsString("Quiz")                    => JsSuccess(TimetableEventType.Quiz)
      case JsString("Assignment")              => JsSuccess(TimetableEventType.Assignment)
      case JsString("EmployeeGuardianMeeting") => JsSuccess(TimetableEventType.EmployeeGuardianMeeting)
      case _                                   => JsSuccess(TimetableEventType.Unspecified)
    }

    def writes(timetableEventType: TimetableEventType): JsValue = JsString(timetableEventType.toString)
  }

  implicit val weekDaysFormat = Json.format[WeekDays]

  implicit val UniversityFormat = Json.format[University]
  implicit val OrgFormat = Json.format[Org]
  implicit val OrgSettingFormat = Json.format[OrgSetting]
  implicit val DeptFormat = Json.format[Dept]
  implicit val CourseFormat = Json.format[Course]
  implicit val SubjectFormat = Json.format[OrgSubject]
  implicit val CompositionFormat = Json.format[OrgComposition]
  implicit val CompaignFormat = Json.format[Compaign]
  implicit val ControlCategoryFormat = Json.format[ControlCategory]
  implicit val ControlFormat = Json.format[Control]
  implicit val TimetableFormat = Json.format[Timetable]
  implicit val AttendanceFormat = Json.format[Attendance]
  implicit val InscriptionInfoFormat = Json.format[InscriptionInfo]
  implicit val AdmissionInfoFormat = Json.format[AdmissionInfo]
  implicit val GuardianInfoFormat = Json.format[GuardianInfo]
  implicit val StudentInfoFormat = Json.format[StudentInfo]
  implicit val EmployeeInfoFormat = Json.format[EmployeeInfo]
  implicit val EmploymentFormat = Json.format[Employment]
}