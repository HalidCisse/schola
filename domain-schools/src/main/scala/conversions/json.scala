package ma.epsilon.schola
package school
package conversions.json

object `package` {

  import domain._

  import play.api.libs.json._

  implicit val inscriptionStatusFormat = new Format[InscriptionStatus] {

    def reads(js: JsValue): JsResult[InscriptionStatus] = js match {
      case JsString("PendingApproval")   => JsSuccess(InscriptionStatus.PendingApproval)
      case JsString("Approved")          => JsSuccess(InscriptionStatus.Approved)
      case JsString("Rejected")          => JsSuccess(InscriptionStatus.Rejected)
      case _                             => JsError("invalid.inscriptionstatus")
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
      case JsString("Parent")    => JsSuccess(GuardianRelation.Parent)
      case JsString("Child")     => JsSuccess(GuardianRelation.Child)
      case JsString("Sibling")   => JsSuccess(GuardianRelation.Sibling)
      case JsString("Spouse")    => JsSuccess(GuardianRelation.Spouse)
      case JsString("Relative")  => JsSuccess(GuardianRelation.Relative)
      case _                     => JsSuccess(GuardianRelation.Other)
    }

    def writes(guardianRelation: GuardianRelation): JsValue = JsString(GuardianRelation.toString)
  }

  implicit val timetableEventTypeFormat = new Format[TimetableEventType] {

    def reads(js: JsValue): JsResult[TimetableEventType] = js match {
      case JsString("Lecture")                    => JsSuccess(TimetableEventType.Lecture)
      case JsString("Break")                      => JsSuccess(TimetableEventType.Break)
      case JsString("Exam")                       => JsSuccess(TimetableEventType.Exam)
      case JsString("Quiz")                       => JsSuccess(TimetableEventType.Quiz)
      case JsString("EmployeeGuardianMeeting")    => JsSuccess(TimetableEventType.EmployeeGuardianMeeting)
      case _                                      => JsSuccess(TimetableEventType.Unspecified)
    }

    def writes(timetableEventType: TimetableEventType): JsValue = JsString(timetableEventType.toString)
  }        
}