package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait EmployeeServicesComponent {

  trait Employees {

    /*    def saveEmployee(
      org: Option[Uuid],
      cin: String,
      username: String,
      givenName: String,
      familyName: String,
      jobTitle: String,
      deptId: Option[Uuid],
      gender: Gender,
      homeAddress: Option[AddressInfo],
      workAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      suspended: Boolean,
      joinDate: Option[java.time.LocalDate],
      createdBy: Option[Uuid],
      accessRights: List[Uuid]): EmployeeInfo

    def updateEmployee(org: Uuid, id: Uuid, spec: EmployeeSpec): Boolean
    def fetchEmployees: List[EmployeeInfo]
    def fetchTrashedEmployees: List[EmployeeInfo]
    def fetchEmployee(id: Uuid): Option[EmployeeInfo]
    def searchEmployeesByCIN(cin: String): List[EmployeeInfo]
    def fetchEmployeeByCIN(cin: String): Option[EmployeeInfo]
    def fetchEmployements(id: Uuid): List[Employment]

    def endEmployment(org: Uuid, id: Uuid, reason: ClosureStatus, remarques: Option[String]): Option[Employment]

    def fetchOrgEmploymentEvents(
      id: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDate]): List[Timetable]

    def purgeEmployee(id: Uuid)*/
  }
}

trait EmployeeServicesRepoComponent {

  protected val employeeServiceRepo: EmployeesServicesRepo

  trait EmployeesServicesRepo {

    /*    def saveEmployee(
      org: Option[Uuid],
      cin: String,
      username: String,
      givenName: String,
      familyName: String,
      jobTitle: String,
      deptId: Option[Uuid],
      gender: Gender,
      homeAddress: Option[AddressInfo],
      workAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      suspended: Boolean,
      joinDate: Option[java.time.LocalDate],
      createdBy: Option[Uuid],
      accessRights: List[Uuid]): EmployeeInfo

    def updateEmployee(org: Uuid, id: Uuid, spec: EmployeeSpec): Boolean
    def fetchEmployees: List[EmployeeInfo]
    def fetchTrashedEmployees: List[EmployeeInfo]
    def fetchEmployee(id: Uuid): Option[EmployeeInfo]
    def searchEmployeesByCIN(cin: String): List[EmployeeInfo]
    def fetchEmployeeByCIN(cin: String): Option[EmployeeInfo]
    def fetchEmployements(id: Uuid): List[Employment]

    def endEmployment(org: Uuid, id: Uuid, reason: ClosureStatus, remarques: Option[String]): Option[Employment]

    def fetchOrgEmploymentEvents(
      id: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDate]): List[Timetable]

    def purgeEmployee(id: Uuid)*/
  }
}