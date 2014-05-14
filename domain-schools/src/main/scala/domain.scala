package ma.epsilon.schola
package school

package domain

object `package` {

  import _root_.ma.epsilon.schola.domain._

  case class University(
    name: String,
    contacts: ContactInfo,
    address: AddressInfo,
    id: Option[Long] = None)

  case class Org(
    name: String,
    contacts: ContactInfo,
    address: AddressInfo,
    universityId: Option[Long],
    _deleted: Boolean = false,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    id: Option[Long] = None)

  case class Dept(
    name: String,
    org: Long,
    departmentChefId: Option[java.util.UUID],
    id: Option[Long] = None)

  case class Course(
    name: String,
    code: String,
    id: Option[Long] = None)

  case class OrgCourse(
    org: Long,
    courseId: Long)

  case class Subject(
    name: String,
    id: Option[Long] = None)

  case class OrgSubject(
    org: Long,
    subjectId: Long)

  case class Batch(
    org: Long,
    name: String,
    courseId: Long,
    empId: java.util.UUID,
    subjectId: Long,    
    startDate: java.time.LocalDate,
    endDate: java.time.LocalDate,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],     
    id: Option[Long] = None)  

  case class Employee(
    empNo: String,
    joinDate: java.time.LocalDate,
    jobTitle: String,
    userId: java.util.UUID,
    deptId: Option[Long],
    id: Option[java.util.UUID] = None)

  case class OrgEmployee(
    org: Long,
    empId: java.util.UUID,
    startDate: java.time.LocalDate,
    endDate: Option[java.time.LocalDateTime] = None,
    endStatus: Option[ClosureStatus] = None,
    endRemarques: Option[String] = None,
    createdBy: Option[java.util.UUID])

  case class EmployeeSubject(
    empId: java.util.UUID,
    batchId: Long,
    startDate: java.time.LocalDate,
    endDate: Option[java.time.LocalDateTime] = None,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID])

  object GuardianRelation extends Enumeration {
    val Parent, Child, Sibling, Spouse, Relative, Other = Value
  }

  type GuardianRelation = GuardianRelation.Value

  case class Guardian(
    occupation: Option[String],
    relation: GuardianRelation = GuardianRelation.Other,
    userId: java.util.UUID,
    id: Option[java.util.UUID] = None)

  case class Student(
    regNo: String,    
    dateOB: java.time.LocalDate,
    nationality: String,
    userId: java.util.UUID,
    guardianId: Option[java.util.UUID],
    id: Option[java.util.UUID] = None)

  object InscriptionStatus extends Enumeration {
    val PendingApproval, Approved, Rejected = Value
  }

  type InscriptionStatus = InscriptionStatus.Value

  object ClosureStatus extends Enumeration {
    val Completed, Adjorned, Unspecified = Value
  }

  type ClosureStatus = ClosureStatus.Value

  case class Admission(
    org: Long,
    studentId: java.util.UUID,
    courseId: Long,
    status: InscriptionStatus = InscriptionStatus.PendingApproval,
    endStatus: Option[ClosureStatus] = None,
    endRemarques: Option[String] = None,
    admDate: java.time.LocalDate,
    endDate: Option[java.time.LocalDateTime] = None,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    id: Option[java.util.UUID] = None)

  case class Inscription(
    admissionId: java.util.UUID,  
    batchId: Long,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    id: Option[java.util.UUID] = None)

  case class ExamCategory(
    name: String,
    startDate: java.time.MonthDay,
    endDate: java.time.MonthDay,
    id: Option[Long] = None)

  case class Exam(
    eventId: Long,
    org: Long,
    name: String,
    subjectId: Long,
    batchId: Long,
    staffs: List[Long],
    `type`: Long,
    date: java.time.LocalDate,
    startTime: java.time.LocalTime,
    duration: java.time.Duration,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    id: Option[Long] = None)

  case class Mark(
    studentId: java.util.UUID,
    empId: java.util.UUID,
    examId: Long,
    marks: Double,
    status: Boolean,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    id: Option[Long] = None)

  object TimetableEventType extends Enumeration {
    val Lecture, Break, Exam, Quiz, EmployeeGuardianMeeting, Unspecified = Value    
  }

  type TimetableEventType = TimetableEventType.Value

  case class Timetable(
    org: Long,
    courseId: Long,
    subjectId: Long,
    batchId: Long,
    dayOfWeek: java.time.DayOfWeek,
    `type`: TimetableEventType = TimetableEventType.Lecture,  
    startTime: java.time.LocalTime,
    endTime: java.time.LocalTime,
    id: Option[java.util.UUID] = None)

  case class TimetableEvent(
    org: Long,
    courseId: Long,
    subjectId: Long,
    batchId: Long,
    `type`: TimetableEventType = TimetableEventType.Lecture,  
    date: java.time.LocalDate,
    startTime: java.time.LocalTime,
    endTime: java.time.LocalTime,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    id: Option[Long] = None)

  case class Attendance(
    userId: java.util.UUID,
    eventId: Long,
    present: Boolean,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID])

  // ##########################################################################################################

  case class OrgInfo(
    id: Long,
    name: String,
    address: AddressInfo,
    contacts: ContactInfo,
    numOfAdmissions: Int,
    courses: List[Long])

  case class AdmissionInfo(
    id: Long,
    org: Long,
    course: Long,
    student: java.util.UUID,
    status: InscriptionStatus = InscriptionStatus.PendingApproval,
    endStatus: Option[ClosureStatus] = None,
    endRemarques: Option[String] = None,
    admDate: java.time.LocalDate,
    endDate: Option[java.time.LocalDateTime] = None,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    inscriptions: List[TeachingRecord])

  case class GuardianInfo(
    id: java.util.UUID,
    cin: String,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts],
    occupation: Option[String],
    relation: GuardianRelation = GuardianRelation.Other)  

  case class StudentInfo(
    id: java.util.UUID,
    cin: String,
    regNo: String,
    courseId: Long,
    dateOB: java.time.LocalDate,
    userId: java.util.UUID,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    createdAt: java.time.LocalDateTime,
    createdBy: Option[java.util.UUID],
    lastModifiedAt: Option[java.time.LocalDateTime],
    lastModifiedBy: Option[java.util.UUID],
    stars: Int = 0,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    contacts: Option[MobileNumbers],
    admissions: Seq[AdmissionInfo],
    guardianInfo: Option[GuardianInfo])  

  case class ScheduledEventSpec(
    id: Option[String],
    org: Long,
    subject: Long,
    `type`: TimetableEventType,
    dayOfWeek: java.time.DayOfWeek,
    startTime: java.time.LocalTime,
    endTime: java.time.LocalTime,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID])  

  case class EmployeeInfo(
    id: java.util.UUID,
    cin: String,
    empNo: String,
    joinDate: java.time.LocalDate,
    jobTitle: String,
    userId: java.util.UUID,
    org: Option[Long],
    deptId: Option[Long],
    primaryEmail: String,
    givenName: String,
    familyName: String,
    createdAt: java.time.LocalDateTime,
    createdBy: Option[java.util.UUID],
    lastModifiedAt: Option[java.time.LocalDateTime],
    lastModifiedBy: Option[java.util.UUID],
    stars: Int = 0,
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts],
    employmentHistory: List[Employment])

  case class Employment(
    id: java.util.UUID,
    org: Long,
    startDate: java.time.LocalDate,
    endDate: Option[java.time.LocalDateTime],
    endStatus: Option[ClosureStatus],
    endRemarques: Option[String],
    createdAt: java.time.LocalDateTime,
    createdBy: Option[java.util.UUID],
    teachingHistory: List[TeachingRecord])

  case class TeachingRecord(
    id: Long,
    course: String,
    subject: String,
    startDate: java.time.LocalDate,
    endDate: Option[java.time.LocalDateTime],
    createdAt: java.time.LocalDateTime,
    createdBy: Option[java.util.UUID])  

  case class StudentMarkInfo(student: StudentInfo, mark: Option[MarkInfo])
  
  case class ExamInfo(
    event: EventInfo,
    org: Long,
    name: String,
    subjectId: Long,
    batchId: Long,
    staffs: List[EmployeeInfo],
    `type`: Long,
    date: java.time.LocalDate,
    startTime: java.time.LocalTime,
    duration: java.time.Duration,
    createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now,
    createdBy: Option[java.util.UUID],
    marks: List[MarkInfo],
    id: Long)
  
  case class MarkInfo(
    marks: Double,
    status: Boolean,
    createdAt: java.time.LocalDateTime)  

  case class EventInfo()
}