package ma.epsilon.schola
package school

package domain

object `package` {

  import _root_.ma.epsilon.schola.domain._

  import java.time.{ LocalDate, LocalDateTime, LocalTime, Duration, MonthDay, Instant }

  object ModuleType extends Enumeration {
    val MODULE, SUBJECT = Value
  }

  type ModuleType = ModuleType.Value

  case class Compaign(
    org: Uuid,
    during: Range[Instant],
    moduleType: ModuleType,
    id: Option[Uuid] = None)

  case class University(
    name: String,
    website: Option[String],
    contacts: ContactInfo,
    address: AddressInfo,
    id: Option[Uuid] = None)

  case class Org(
    name: String,
    acronyms: Option[String],
    website: Option[String],
    contacts: ContactInfo,
    address: AddressInfo,
    universityId: Option[Uuid],
    _deleted: Boolean = false,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  case class WeekDays(
    Monday: Boolean    = true,
    Tuesday: Boolean   = true,
    Wednesday: Boolean = true,
    Thursday: Boolean  = true,
    Friday: Boolean    = true,
    Saturday: Boolean  = false,
    Sunday: Boolean    = false)

  case class OrgSetting(
    org: Uuid,
    sessionDuration: Duration,
    weekDays: WeekDays,
    startOfInscription: Option[LocalDate],
    endOfInscription: Option[LocalDate],
    attendanceEnabled: Boolean)

  case class Range[T](start: T, end: T)

  case class OrgComposition(
    compaignId: Uuid,
    name: String,
    during: Range[Instant],
    coefficient: Option[Double],
    id: Option[Uuid] = None)

  case class Dept(
    name: String,
    org: Uuid,
    departmentChefId: Option[Uuid],
    id: Option[Uuid] = None)

  case class Course(
    name: String,
    code: Option[String],
    id: Option[Uuid] = None)

  case class OrgCourse(
    org: Uuid,    
    levels: Int,
    deptId: Option[Uuid],
    desc: Option[String],
    courseId: Uuid)

  case class OrgSubject(
    org: Uuid,
    name: String,
    desc: Option[String],
    id: Option[Uuid] = None)

/*  case class Batch(
    name: String,
    empId: Option[Uuid],
    courseId: Uuid,
    subjectId: Uuid,
    compositionId: Uuid,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)*/

  case class Employee(
    empNo: String,
    userId: Uuid,
    id: Option[Uuid] = None)

  case class OrgEmployment(
    org: Uuid,
    empId: Uuid,
    deptId: Option[Uuid],
    joinDate: LocalDate,
    endDate: Option[LocalDateTime] = None,
    endStatus: Option[ClosureStatus] = None,
    endRemarques: Option[String] = None,
    createdBy: Option[Uuid])

  case class TeachingHistory(
    empId: Uuid,
    batchId: Uuid,
    startDate: LocalDate,
    endDate: LocalDateTime,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  object GuardianRelation extends Enumeration {
    val Parent, Child, Sibling, Spouse, Relative, Other = Value
  }

  type GuardianRelation = GuardianRelation.Value

  case class Guardian(
    relation: GuardianRelation = GuardianRelation.Other,
    userId: Uuid,
    id: Option[Uuid] = None)

  case class Student(
    regNo: String,
    dateOB: LocalDate,
    nationality: String,
    userId: Uuid,
    guardianId: Option[Uuid],
    id: Option[Uuid] = None)

  object InscriptionStatus extends Enumeration {
    val PendingApproval, Approved, Rejected = Value
  }

  type InscriptionStatus = InscriptionStatus.Value

  object ClosureStatus extends Enumeration {
    val Completed, Adjorned, Unspecified = Value
  }

  type ClosureStatus = ClosureStatus.Value

  case class Admission(
    org: Uuid,
    studentId: Uuid,
    courseId: Uuid,
    status: InscriptionStatus = InscriptionStatus.PendingApproval,
    endStatus: Option[ClosureStatus] = None,
    endRemarques: Option[String] = None,
    admDate: LocalDate,
    endDate: Option[LocalDateTime] = None,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  case class Inscription(
    admissionId: Uuid,
    compaignId: Uuid,
    level: Int,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  case class ControlCategory(
    name: String,
    during: Range[Instant],
    compositionId: Uuid,
    coefficient: Option[Double],
    id: Option[Uuid] = None)

  case class Control(
    eventId: Uuid,
    name: String,
    batchId: Uuid,
    supervisors: List[Uuid],
    `type`: Uuid,
    coefficient: Option[Double],
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  case class Mark(
    studentId: Uuid,
    empId: Uuid,
    controlId: Uuid,
    marks: Double,
    createdAt: LocalDateTime = now,
    id: Option[Uuid] = None)

  object TimetableEventType extends Enumeration {
    val Lecture, Lab, Assignment, Seminar, Sports, Break, Exam, Quiz, EmployeeGuardianMeeting, Unspecified = Value
  }

  type TimetableEventType = TimetableEventType.Value

  object Recurrence extends Enumeration {
    val None, Daily, Weekly, Monthly, Yearly = Value
  }

  type Recurrence = Recurrence.Value

  case class Timetable(
    batchId: Uuid,
    `type`: TimetableEventType = TimetableEventType.Lecture,
    `class`: String,
    during: Range[Instant],
    recurrence: Recurrence = Recurrence.None,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  case class Attendance(
    userId: Uuid,
    eventId: Uuid,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid])

  // ##########################################################################################################
  
  /*case class OrgCourseSubject(
    empId: Option[Uuid],
    compaignId: Uuid,
    compositionId: Uuid,
    courseId: Uuid,
    subjectId: Uuid,
    level: Int,
    coefficient: Option[Double],
    id: Option[Uuid] = None)*/

  case class Module(
    org: Uuid,
    name: String,
    id: Option[Uuid] = None)

  case class OrgCourseModule(
    compaignId: Uuid,
    compositionId: Uuid,    
    courseId: Uuid,
    moduleId: Uuid,
    level: Int,
    coefficient: Option[Double],
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  case class Batch(
    empId: Option[Uuid],
    compaignId: Uuid,
    compositionId: Uuid,
    courseId: Uuid,
    moduleId: Uuid,
    subjectId: Uuid,
    level: Int,
    coefficient: Option[Double],
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    id: Option[Uuid] = None)

  // ##########################################################################################################

  case class InscriptionInfo(
    admissionId: Uuid,
    compaignId: Uuid,
    createdAt: LocalDateTime,
    createdBy: Option[Uuid])

  case class AdmissionInfo(
    id: Uuid,
    org: Uuid,
    course: Uuid,
    student: Uuid,
    status: InscriptionStatus,
    endStatus: Option[ClosureStatus],
    endRemarques: Option[String],
    admDate: LocalDate,
    endDate: Option[LocalDateTime],
    createdAt: LocalDateTime,
    createdBy: Option[Uuid])

  case class GuardianInfo(
    id: Uuid,
    cin: String,
    userId: Uuid,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    jobTitle: String,
    relation: GuardianRelation,
    createdAt: LocalDateTime,
    createdBy: Option[Uuid],
    lastLoginTime: Option[LocalDateTime],
    lastModifiedAt: Option[LocalDateTime],
    lastModifiedBy: Option[Uuid],
    stars: Int,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts],
    suspended: Boolean)

  case class StudentInfo(
    id: Uuid,
    regNo: String,
    dateOB: LocalDate,
    nationality: String,
    userId: Uuid,
    cin: String,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    jobTitle: String,
    createdAt: LocalDateTime,
    createdBy: Option[Uuid],
    lastLoginTime: Option[LocalDateTime],
    lastModifiedAt: Option[LocalDateTime],
    lastModifiedBy: Option[Uuid],
    stars: Int,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    contacts: Option[Contacts],
    suspended: Boolean)

  case class EmployeeInfo(
    id: Uuid,
    empNo: String,
    userId: Uuid,
    cin: String,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    jobTitle: String,
    createdAt: LocalDateTime,
    createdBy: Option[Uuid],
    lastLoginTime: Option[LocalDateTime],
    lastModifiedAt: Option[LocalDateTime],
    lastModifiedBy: Option[Uuid],
    stars: Int,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts],
    suspended: Boolean,
    org: Option[Uuid],
    deptId: Option[Uuid])

  case class Employment(
    id: Uuid,
    org: Uuid,
    name: String,
    empId: Option[Uuid],
    courseId: Uuid,
    subjectId: Uuid,
    historyId: Option[Uuid],
    startDate: LocalDate,
    endDate: Option[LocalDateTime],
    createdAt: LocalDateTime,
    createdBy: Option[Uuid])
}