package ma.epsilon.schola
package school

package impl

import _root_.ma.epsilon.schola.domain._, school.domain._

trait ControlsServicesComponentImpl extends ControlsServicesComponent {
  this: ControlsServicesRepoComponent =>

  trait ControlsServices extends Controls {

    def getControlCategories(org: Uuid) = controlServicesRepo.getControlCategories(org)

    def saveControlCategory(name: String, during: Range[java.time.Instant], compositionId: Uuid, coefficient: Option[Double]) = controlServicesRepo.saveControlCategory(name, during, compositionId, coefficient)
    def updateControlCategory(id: Uuid, spec: ScheduledSpec) = controlServicesRepo.updateControlCategory(id, spec)
    def delControlCategory(id: Uuid) = controlServicesRepo.delControlCategory(id)

    def addControl(
      eventId: Uuid,
      name: String,
      batchId: Uuid,
      supervisors: List[Uuid],
      `type`: Uuid,
      coefficient: Option[Double],
      createdBy: Option[Uuid]) = controlServicesRepo.addControl(eventId, name, batchId, supervisors, `type`, coefficient, createdBy)

    def delControl(id: Uuid) = controlServicesRepo.delControl(id)

    def markStudent(
      id: String,
      examId: Uuid,
      empId: Uuid,
      marks: Double) = controlServicesRepo.markStudent(id, examId, empId, marks)
  }
}

trait ControlsServicesRepoComponentImpl extends ControlsServicesRepoComponent {
  this: jdbc.WithDatabase =>

  import school.schema._
  import jdbc.Q._

  protected val controlServicesRepo = new ControlsServicesRepoImpl

  class ControlsServicesRepoImpl extends ControlsServicesRepo {

    private[this] object oq {

      val categories = {

        def forDuring(id: Column[Uuid]) =
          ControlCategories filter (_.id === id) map (_.during)                

        def getControlCategories(org: Column[Uuid]) =
          ControlCategories
            .innerJoin(OrgCompositions).on(_.compositionId === _.id)
            .innerJoin(
              Compaigns
                .filter(_.org === org)
            ).on(_._2.compaignId === _.id)
            .map(_._1._1)

        def forNamed(id: Column[Uuid]) =
          ControlCategories
            .filter(_.id === id)
            .map(_.name)

        def forCoefficient(id: Column[Uuid]) =
          ControlCategories
            .filter(_.id === id)
            .map(_.coefficient)            

        new {
          val all = Compiled(getControlCategories _)
          val named = Compiled(forNamed _)
          val coefficient = Compiled(forCoefficient _)
          val during = Compiled{forDuring _}
        }
      }

      val controls = {

        def forFutureEvent(id: Column[Uuid]) =
          Timetables
            .filter(event => event.id === id && java.sql.Timestamp.from(now) <@^: event.during)                

        def byId(id: Column[Uuid]) =
          Controls filter(_.id === id)

        new {
          val id = Compiled{byId _}
          val futureEvent = Compiled{forFutureEvent _}
        }
      }
    }

    def getControlCategories(org: Uuid) =
      db.withSession { implicit s =>
        oq.categories
          .all(org)
          .list
      }

    def saveControlCategory(name: String, during: Range[java.time.Instant], compositionId: Uuid, coefficient: Option[Double]) =
      db.withTransaction { implicit s =>
        ControlCategories insert (name, during, compositionId, coefficient)
      }

    def updateControlCategory(id: Uuid, spec: ScheduledSpec) =
      db.withTransaction { implicit s =>

        val _1 = spec.name map {
          name =>

            oq.categories
              .named(id)
              .update(name) == 1

        } getOrElse true

        val _2 = _1 && (spec.during map {
          during =>

            oq.categories
              .during(id)
              .update(
                com.github.tminglei.slickpg.Range[java.sql.Timestamp](
                  java.sql.Timestamp.from(during.start),
                  java.sql.Timestamp.from(during.end),
                  com.github.tminglei.slickpg.`[_,_]`)
              ) == 1

        } getOrElse true)

        _2 && (spec.coefficient map {
          coefficient =>

            oq.categories
              .coefficient(id)
              .update(Some(coefficient)) == 1

        } getOrElse true)        
      }

    def delControlCategory(id: Uuid) =
      db.withTransaction { implicit s =>
        ControlCategories delete (id)
      }

    def addControl(
      eventId: Uuid,
      name: String,
      batchId: Uuid,
      supervisors: List[Uuid],
      `type`: Uuid,
      coefficient: Option[Double],
      createdBy: Option[Uuid]) = 
    db.withTransaction {
      implicit s =>
        Controls insert(name, eventId, batchId, supervisors, `type`, coefficient, createdBy)
    }

    def delControl(id: Uuid) = {

      def doDelete()(implicit s: Session) = Controls delete(id)

      db.withSession{implicit s => oq.controls.id(id).firstOption} match {
        case Some(Control(eventId, _, _, _, _, _, _, _, _)) => 

          db.withTransaction {
            implicit s =>

              oq.controls
                .futureEvent(eventId)
                .delete

              doDelete()
          }
        
        case _ =>
      
          db.withTransaction {
            implicit s => doDelete()
          }
      }        
    }

    def markStudent(
      id: String,
      examId: Uuid,
      empId: Uuid,
      marks: Double) = ???
  }
}
