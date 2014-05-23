package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait SchoolServicesComponent {
  this: CommonSchoolServicesComponent with CoursesSchoolServicesComponent with EmployeeServicesComponent with StudentServicesComponent with ControlsServicesComponent with TimetableServicesComponent with AttendanceServicesComponent =>

  val schoolService: SchoolServices

  trait SchoolServices extends Common
    with Courses
    with Employees
    with Students
    with Controls
    with Timetables
    with Attendances
}

/*trait SchoolServicesRepoComponent {
  this: CommonSchoolServicesRepoComponent 
        with EmployeeServicesRepoComponent 
        with StudentServicesRepoComponent 
        =>

  trait SchoolServicesRepo extends CommonServicesRepo
    with EmployeesServicesRepo
    with StudentsServicesRepo
}*/
