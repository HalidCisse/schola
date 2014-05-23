package ma.epsilon.schola
package school

object Schools {

  val appId = Uuid(config.getString("schools.app_id"))

  object accessRights {

    val STUDENT = "schools.student"
    val SUPERVISOR = "schools.supervisor"
    val TEACHER = "schools.teacher"

    val DIRECTOR_ALIAS = "school.director"
    val ADMISSION_OFFICER_ALIAS = "school.admission_officer"

    def DIRECTOR(org: Uuid) = s"${DIRECTOR_ALIAS}_${org}"
    def ADMISSION_OFFICER(org: Uuid) = s"${ADMISSION_OFFICER_ALIAS}_${org}"

    def DISPLAYNAME(alias: String) = alias
  }

  object scopes {
    import _root_.ma.epsilon.schola.domain.Scope

    val STUDENT = List.empty[Scope]
    val SUPERVISOR = List.empty[Scope]
    val TEACHER = List.empty[Scope]

    val DIRECTOR = List.empty[Scope]

    val ADMISSION_OFFICER = List.empty[Scope]
  }
}