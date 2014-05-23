package ma.epsilon.schola
package school

package impl

import _root_.ma.epsilon.schola.domain._, school.domain._

trait SchoolServicesComponentImpl {
  this: CommonSchoolServicesComponentImpl with CoursesSchoolServicesComponentImpl with EmployeeServicesComponentImpl with StudentServicesComponentImpl with ControlsServicesComponentImpl // with TimetableServicesComponent
  // with AttendanceServicesComponent 
  =>

  class SchoolServicesImpl extends CommonServices
    with CoursesServices
    with EmployeesServices
    with StudentsServices
    with ControlsServices
  // with TimetablesImpl 
  // with AttendancesImpl
}