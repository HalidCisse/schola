package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait AttendanceServicesComponent {

  trait Attendances {

    def markStudentPresence(
      id: String,
      event: Uuid,
      createdBy: Option[String])

    def markEmployeePresence(
      id: String,
      event: Uuid,
      createdBy: Option[String])
  }
}
