package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait ControlsServicesComponent {

  trait Controls {

    def getControlCategories(org: Uuid): List[ControlCategory]

    def saveControlCategory(name: String, during: Range[java.time.Instant], compositionId: Uuid, coefficient: Option[Double]): ControlCategory
    def updateControlCategory(id: Uuid, spec: ScheduledSpec): Boolean
    def delControlCategory(id: Uuid)

    def addControl(
      eventId: Uuid,
      name: String,
      batchId: Uuid,
      supervisors: List[Uuid],
      `type`: Uuid,
      coefficient: Option[Double],
      createdBy: Option[Uuid]): Control

    def delControl(id: Uuid)

    def markStudent(
      id: String,
      examId: Uuid,
      empId: Uuid,
      marks: Double): Boolean
  }
}

trait ControlsServicesRepoComponent {

  protected val controlServicesRepo: ControlsServicesRepo

  trait ControlsServicesRepo {

    def getControlCategories(org: Uuid): List[ControlCategory]

    def saveControlCategory(name: String, during: Range[java.time.Instant], compositionId: Uuid, coefficient: Option[Double]): ControlCategory
    def updateControlCategory(id: Uuid, spec: ScheduledSpec): Boolean
    def delControlCategory(id: Uuid)

    def addControl(
      eventId: Uuid,
      name: String,
      batchId: Uuid,
      supervisors: List[Uuid],
      `type`: Uuid,
      coefficient: Option[Double],
      createdBy: Option[Uuid]): Control

    def delControl(id: Uuid)

    def markStudent(
      id: String,
      examId: Uuid,
      empId: Uuid,
      marks: Double): Boolean
  }
}
