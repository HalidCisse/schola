package ma.epsilon.schola
package school

/*Halid have been here

import _root_.ma.epsilon.schola.domain._, school.domain._

trait StudentServicesComponent {

  trait Students {

/*    def addStudent(
      org: Option[Uuid],
      cin: String,
      courseId: Option[Uuid],
      batch: Option[Uuid],
      admDate: Option[java.time.LocalDate],
      nationality: String,
      dateOB: java.time.LocalDate,
      username: String,
      givenName: String,
      familyName: String,
      gender: Gender,
      homeAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      createdBy: Option[Uuid]): StudentInfo

    def setGuardian(
      id: Uuid,
      cin: String,
      username: String,
      givenName: String,
      familyName: String,
      jobTitle: String,
      gender: Gender,
      homeAddress: Option[AddressInfo],
      workAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      relation: Option[GuardianRelation], 
      createdBy: Option[Uuid]): GuardianInfo

    def addInscription(admissionId: Uuid, compaignId: Uuid, createdBy: Option[Uuid]): Boolean

    def endStudentAdm(/**/
      id: Uuid,
      reason: ClosureStatus,
      remarques: Option[String]): Boolean

    def updateStudent(org: Uuid, studentId: Uuid, spec: StudentSpec): Boolean
    def fetchStudents(implicit page: Page): List[StudentInfo]
    def fetchPurgedStudents: List[StudentInfo]
    def fetchStudentsByCIN(cin: String): List[StudentInfo]
    def fetchStudentByCIN(cin: String): Option[StudentInfo]
    def fetchAdmissions(org: Uuid): List[AdmissionInfo]
    def fetchInscriptions(compaignId: Uuid): List[InscriptionInfo]
    def fetchOrgStudentInscriptions(studentId: Uuid): List[InscriptionInfo]
    def fetchGuardian(studentId: Uuid): Option[GuardianInfo]

    def fetchOrgStudentEvents(
      admissionId: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDateTime]): List[Timetable]

    def purgeStudent(studentId: Uuid)*/
  }
}

trait StudentServicesRepoComponent {

  protected val studentServiceRepo: StudentsServicesRepo

  trait StudentsServicesRepo {

/*    def addStudent(
      org: Option[Uuid],
      cin: String,
      courseId: Option[Uuid],
      batch: Option[Uuid],
      admDate: Option[java.time.LocalDate],
      nationality: String,
      dateOB: java.time.LocalDate,
      username: String,
      givenName: String,
      familyName: String,
      gender: Gender,
      homeAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      createdBy: Option[Uuid]): StudentInfo

    def setGuardian(
      id: Uuid,
      cin: String,
      username: String,
      givenName: String,
      familyName: String,
      jobTitle: String,
      gender: Gender,
      homeAddress: Option[AddressInfo],
      workAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      relation: Option[GuardianRelation], 
      createdBy: Option[Uuid]): GuardianInfo

    def addInscription(admissionId: Uuid, compaignId: Uuid, createdBy: Option[Uuid]): Boolean

    def endStudentAdm(/**/
      admissionId: Uuid,
      reason: ClosureStatus,
      remarques: Option[String]): Boolean

    def updateStudent(org: Uuid, studentId: Uuid, spec: StudentSpec): Boolean
    def fetchStudents(implicit page: Page): List[StudentInfo]
    def fetchPurgedStudents: List[StudentInfo]
    def fetchStudentsByCIN(cin: String): List[StudentInfo]
    def fetchStudentByCIN(cin: String): Option[StudentInfo]
    def fetchAdmissions(org: Uuid): List[AdmissionInfo]
    def fetchInscriptions(org: Uuid): List[InscriptionInfo]
    def fetchOrgStudentInscriptions(studentId: Uuid): List[InscriptionInfo]
    def fetchGuardian(studentId: Uuid): Option[GuardianInfo]

    def fetchOrgStudentEvents(
      admissionId: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDateTime]): List[Timetable]

    def purgeStudent(studentId: Uuid)*/
  }
}
