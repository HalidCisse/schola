package schola
package oadmin

package impl

trait LabelServicesComponentImpl extends LabelServicesComponent {
  this: LabelServicesRepoComponent =>

  class LabelServicesImpl extends LabelServices {

    def getLabels = labelServiceRepo.getLabels

    def updateLabel(label: String, newName: String) = labelServiceRepo.updateLabel(label, newName)

    def findOrNew(label: String, color: Option[String]) = labelServiceRepo.findOrNew(label, color)

    def remove(labels: Set[String]) {
      labelServiceRepo.remove(labels)
    }

    def labelUser(userId: String, labels: Set[String]) {
      labelServiceRepo.labelUser(userId, labels)
    }

    def unLabelUser(userId: String, labels: Set[String]) {
      labelServiceRepo.unLabelUser(userId, labels)
    }

    def getUserLabels(userId: String) = labelServiceRepo.getUserLabels(userId)
  }
}

trait LabelServicesRepoComponentImpl extends LabelServicesRepoComponent {
  this: LabelServicesComponent =>

  import schema._
  import domain._
  import Q._

  private[this] val log = Logger("oadmin.LabelServicesRepoComponentImpl")

  protected val db: Database

  protected val labelServiceRepo = new LabelServicesRepoImpl

  class LabelServicesRepoImpl extends LabelServicesRepo {

    private[this] object oq {

      val labels = Compiled(Labels map (l => (l.name, l.color)))

      val labelled = {
        def getLabel(label: Column[String]) =
          Labels where (_.name is label)

        Compiled(getLabel _)
      }

      val forUpdate = {
        def getLabelName(label: Column[String]) =
          Labels where (_.name is label) map (_.name)

        Compiled(getLabelName _)
      }

      val forUser = {
        def getUserLabels(userId: Column[java.util.UUID]) =
          UsersLabels where (_.userId is userId)

        Compiled(getUserLabels _)
      }
    }

    def getLabels = {
      import Database.dynamicSession

      val labels = oq.labels

      val result = db.withDynSession {
        labels.list
      }

      result map (Label.tupled)
    }

    def updateLabel(label: String, newName: String) = {
      val labelInDB = oq.forUpdate(label)

      db.withTransaction { implicit session =>
        labelInDB.update(newName) == 1
      }
    }

    def findOrNew(label: String, color: Option[String] = None) = {

      val labelInDB = oq.labelled(label)

      def result = db.withSession { implicit session =>
        labelInDB.firstOption
      }

      result.fold {

        db.withTransaction { implicit session =>
          Option(Labels.insert(label, color.getOrElse("#fff")))
        }

      } {
        case Label(name, colorInDB) =>

          for (c <- color if c ne colorInDB)
            db.withTransaction { implicit session =>
              labelInDB.update(Label(label, c))
            }

          result
      }
    }

    def remove(labels: Set[String]) =
      db.withTransaction { implicit session =>
        val labelsInDB = Labels where (_.name inSet labels)

        labelsInDB.delete
      }

    def labelUser(userId: String, labels: Set[String]) = {
      val id = uuid(userId)

      labels foreach {
        label =>

          val result = findOrNew(label)

          result match {
            case Some(Label(name, _)) => db.withTransaction { implicit s => UsersLabels += UserLabel(id, name) }
            case _                    => {}
          }
      }
    }

    def unLabelUser(userId: String, labels: Set[String]) =
      db.withTransaction { implicit session =>
        val userLabel = UsersLabels where (uL => (uL.userId is uuid(userId)) && (uL.label inSet labels))

        userLabel.delete
      }

    def getUserLabels(userId: String) = {
      import Database.dynamicSession

      val userLabels = oq.forUser(uuid(userId))

      db.withDynSession {
        userLabels.list
      }
    }
  }
}