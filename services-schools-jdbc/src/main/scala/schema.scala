package ma.epsilon.schola
package school

package schema

object `package` {

  import jdbc.Q._

  import ma.epsilon.schola.domain._
  import ma.epsilon.schola.school.domain._

  import ma.epsilon.schola.conversions.jdbc._

  import scala.slick.model.ForeignKeyAction

  import java.time.{ LocalDate, LocalDateTime, LocalTime, Duration, MonthDay }

  // #############################################################################################

  class Users(tag: Tag) extends Table[User](tag, "users") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val cin = column[String]("cin", O.NotNull)

    val primaryEmail = column[String]("primary_email", O.NotNull)

    val password = column[String]("password", O.NotNull, O.DBType("text"))

    val givenName = column[String]("given_name", O.NotNull)

    val familyName = column[String]("family_name", O.NotNull)

    val jobTitle = column[String]("job_title", O.NotNull)

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    val lastLoginTime = column[Option[LocalDateTime]]("last_login_time")

    val lastModifiedAt = column[Option[LocalDateTime]]("last_modified_at")

    val lastModifiedBy = column[Option[Uuid]]("last_modified_by")

    val stars = column[Int]("stars", O.Default(0))

    val gender = column[Gender]("gender", O.NotNull, O.Default(Gender.Male))

    val homeAddress = column[Option[AddressInfo]]("home_address", O.DBType("json"))

    val workAddress = column[Option[AddressInfo]]("work_address", O.DBType("json"))

    val contacts = column[Option[Contacts]]("contacts", O.DBType("json"))

    val activationKey = column[Option[String]]("user_activation_key", O.DBType("text"))

    val _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    val suspended = column[Boolean]("suspended", O.NotNull, O.Default(false))

    val changePasswordAtNextLogin = column[Boolean]("change_password_at_next_login", O.NotNull, O.Default(false))

    def * = (cin, primaryEmail, password?, givenName, familyName, jobTitle, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, activationKey, _deleted, suspended, changePasswordAtNextLogin, id?) <> ({ t: (String, String, Option[String], String, String, String, LocalDateTime, Option[Uuid], Option[LocalDateTime], Option[LocalDateTime], Option[Uuid], Int, Gender, Option[AddressInfo], Option[AddressInfo], Option[Contacts], Option[String], Boolean, Boolean, Boolean, Option[Uuid]) =>
      t match {
        case (
          cin,
          primaryEmail,
          password,
          givenName,
          familyName,
          jobTitle,
          createdAt,
          createdBy,
          lastLoginTime,
          lastModifiedAt,
          lastModifiedBy,
          stars,
          gender,
          homeAddress,
          workAddress,
          contacts,
          activationKey,
          _deleted,
          suspended,
          changePasswordAtNextLogin,
          id) =>
          User(
            cin,
            primaryEmail,
            password,
            givenName,
            familyName,
            jobTitle,
            createdAt,
            createdBy,
            lastLoginTime,
            lastModifiedAt,
            lastModifiedBy,
            stars,
            gender,
            homeAddress,
            workAddress,
            contacts,
            activationKey,
            _deleted,
            suspended,
            changePasswordAtNextLogin,
            id)
      }
    }, (user: User) => Some(user.cin, user.primaryEmail, user.password, user.givenName, user.familyName, user.jobTitle, user.createdAt, user.createdBy, user.lastLoginTime, user.lastModifiedAt, user.lastModifiedBy, user.stars, user.gender, user.homeAddress, user.workAddress, user.contacts, user.activationKey, user._deleted, user.suspended, user.changePasswordAtNextLogin, user.id))

    lazy val _createdBy = foreignKey("USER_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    lazy val _lastModifiedBy = foreignKey("USER_MODIFIER_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val indexPrimaryEmail = index("USER_USERNAME_INDEX", primaryEmail, unique = true)

    val indexCIN = index("USER_CIN_INDEX", cin, unique = true)

    val pk = primaryKey("USER_PK", id)
  }

  val Users = TableQuery[Users]

  //

  class AccessRights(tag: Tag) extends Table[AccessRight](tag, "access_rights") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val alias = column[String]("alias", O.NotNull)

    val displayName = column[String]("display_name", O.NotNull)

    val redirectUri = column[String]("redirect_uri", O.NotNull)

    val appId = column[Uuid]("app_id", O.NotNull)

    val scopes = column[List[Scope]]("scopes", O.DBType("json"), O.NotNull, O.Default(List()))

    val grantOptions = column[List[Uuid]]("grant_options", O.NotNull, O.Default(List()))

    def * = (alias, displayName, redirectUri, appId, scopes, grantOptions, id?) <> (AccessRight.tupled, AccessRight.unapply)

    val pk = primaryKey("ACCESS_RIGHT_PK", id)
  }

  val AccessRights = new TableQuery(new AccessRights(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into { case (newAccessRight, id) => newAccessRight copy (id = Some(id)) }

      inserts.insertInvoker
    }

    def insert(alias: String, displayName: String, redirectUri: String, appId: Uuid, scopes: List[Scope], grantOptions: List[Uuid] = Nil)(implicit session: jdbc.Q.Session): AccessRight =
      insertInvoker insert AccessRight(alias, displayName, redirectUri, appId, scopes, grantOptions)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // ##############################################################################################

  class Universities(tag: Tag) extends Table[University](tag, "universities") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val name = column[String]("name", O.NotNull)

    val website = column[Option[String]]("website")

    val contacts = column[ContactInfo]("contacts", O.NotNull, O.DBType("json"), O.Default(ContactInfo()))

    val address = column[AddressInfo]("address", O.NotNull, O.DBType("json"), O.Default(AddressInfo()))

    def * = (name, website, contacts, address, id?) <> (University.tupled, University.unapply)

    val indexName = index("UNIVERSITY_NAME_INDEX", name, unique = true)

    val pk = primaryKey("UNIVERSITY_PK", id)
  }

  val Universities = new TableQuery(new Universities(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newUniversity, id) => newUniversity copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, website: Option[String], contacts: ContactInfo = ContactInfo(), address: AddressInfo = AddressInfo())(implicit session: jdbc.Q.Session): University =
      insertInvoker insert University(name, website, contacts, address)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // ################################################################################################

  class Orgs(tag: Tag) extends Table[Org](tag, "orgs") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val name = column[String]("name", O.NotNull)

    val acronyms = column[Option[String]]("acronyms")

    val website = column[Option[String]]("website")

    val contacts = column[ContactInfo]("contacts", O.DBType("json"))

    val address = column[AddressInfo]("address", O.DBType("json"))

    val universityId = column[Option[Uuid]]("university_id")

    val _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (name, acronyms, website, contacts, address, universityId, _deleted, createdAt, createdBy, id?) <> (Org.tupled, Org.unapply)

    val _createdBy = foreignKey("ORG_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val indexName = index("ORG_NAME_INDEX", name, unique = true)

    val pk = primaryKey("ORG_PK", id)

    val university = foreignKey("ORG_UNIVERSITY_FK", universityId, Universities)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val Orgs = new TableQuery(new Orgs(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newOrg, id) => newOrg copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    private val forDeletion = {
      def getDeleted(id: Column[Uuid]) =
        this
          .filter(_.id === id)
          .map(_._deleted)

      Compiled(getDeleted _)
    }

    def insert(name: String, acronyms: Option[String], website: Option[String], universityId: Option[Uuid], contacts: ContactInfo = ContactInfo(), address: AddressInfo = AddressInfo(), createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): Org =
      insertInvoker insert Org(name, acronyms, website, contacts, address, universityId, createdBy = createdBy)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      forDeletion(id).update(true) == 1

    def purge(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  class OrgSettings(tag: Tag) extends Table[OrgSetting](tag, "org_settings") {

    val org = column[Uuid]("org", O.NotNull)

    val sessionDuration = column[Duration]("session_duration", O.Default(Duration.ofHours(2)))

    val weekDays = column[WeekDays]("weekdays", O.Default(WeekDays()))

    val startOfInscription = column[Option[LocalDate]]("start_of_inscription")

    val endOfInscription = column[Option[LocalDate]]("end_of_inscription")

    val attendanceEnabled = column[Boolean]("enable_attendance", O.NotNull, O.Default(true))

    def * = (org, sessionDuration, weekDays, startOfInscription, endOfInscription, attendanceEnabled) <> (OrgSetting.tupled, OrgSetting.unapply)

    val pk = primaryKey("ORG_SETTING_PK", org)

    val _org = foreignKey("ORG_SETTING_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
  }

  val OrgSettings = new TableQuery(new OrgSettings(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this

      inserts.insertInvoker
    }

    def insert(org: Uuid, sessionDuration: Duration, weekdays: WeekDays, startOfInscription: Option[LocalDate], endOfInscription: Option[LocalDate], attendanceEnabled: Boolean)(implicit session: jdbc.Q.Session): OrgSetting =
      insertInvoker insert OrgSetting(org, sessionDuration, weekdays, startOfInscription, endOfInscription, attendanceEnabled)
  }

 // -------------------------------------------------------------------------------------------- 

  class Compaigns(tag: Tag) extends Table[Compaign](tag, "org_compaigns") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val org = column[Uuid]("org", O.NotNull)

    val during = column[com.github.tminglei.slickpg.Range[java.sql.Timestamp]]("during", O.NotNull)

    val moduleType = column[ModuleType]("module_type", O.NotNull, O.Default(ModuleType withName config.getString("schools.default_module_system")))

    def * = (org, during, moduleType, id?) <> (
      { t: (Uuid, com.github.tminglei.slickpg.Range[java.sql.Timestamp], ModuleType, Option[Uuid]) =>
        t match {
          case (org, during, moduleType, id) =>
            Compaign(
              org,
              Range(
                java.time.Instant.ofEpochMilli(during.start.getTime),
                java.time.Instant.ofEpochMilli(during.end.getTime)), moduleType, id = id)
        }
      },
      (compaign: Compaign) =>
        Some((
          compaign.org,
          com.github.tminglei.slickpg.Range[java.sql.Timestamp](
            java.sql.Timestamp.from(compaign.during.start),
            java.sql.Timestamp.from(compaign.during.end),
            com.github.tminglei.slickpg.`[_,_]`),
            compaign.moduleType,
            compaign.id)))    

    val pk = primaryKey("COMPAIGN_PK", id)

    val _org = foreignKey("COMPAIGN_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
  }

  val Compaigns = new TableQuery(new Compaigns(_)) {
    
    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newCompaign, id) => newCompaign copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(org: Uuid, during: Range[java.time.Instant], moduleType: ModuleType)(implicit session: jdbc.Q.Session): Compaign =
      insertInvoker insert Compaign(org, during, moduleType)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete      
  }

  // --------------------------------------------------------------------------------------------

  class OrgCompositions(tag: Tag) extends Table[OrgComposition](tag, "org_compositions") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val compaignId = column[Uuid]("compaign_id", O.NotNull)

    val name = column[String]("name", O.NotNull)

    val during = column[com.github.tminglei.slickpg.Range[java.sql.Timestamp]]("during", O.NotNull)

    val coefficient = column[Option[Double]]("coefficient")

    // TODO: add sql `alter table "org_semesters" add constraint "ORG_COMPOSITION_DURING" EXCLUDE USING gist ((org :: text) WITH =, during WITH &&)`

    def * = (compaignId, name, during, coefficient, id?) <> (
      { t: (Uuid, String, com.github.tminglei.slickpg.Range[java.sql.Timestamp], Option[Double], Option[Uuid]) =>
        t match {
          case (compaignId, name, during, coefficient, id) =>
            OrgComposition(
              compaignId,
              name,
              Range(
                java.time.Instant.ofEpochMilli(during.start.getTime),
                java.time.Instant.ofEpochMilli(during.end.getTime)), coefficient, id = id)
        }
      },
      (composition: OrgComposition) =>
        Some((
          composition.compaignId,
          composition.name,
          com.github.tminglei.slickpg.Range[java.sql.Timestamp](
            java.sql.Timestamp.from(composition.during.start),
            java.sql.Timestamp.from(composition.during.end),
            com.github.tminglei.slickpg.`[_,_]`),
          composition.coefficient,
          composition.id)))

    val indexName = index("COMPOSITION_NAME_INDEX", (name, compaignId), unique = true)
    
    val indexCompaign = index("COMPOSITION_ID_COMPAIGN_INDEX", (id, compaignId), unique = true)

    val pk = primaryKey("COMPOSITION_PK", id)

    val compaign = foreignKey("COMPOSITION_COMPAIGN_FK", compaignId, Compaigns)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val OrgCompositions = new TableQuery(new OrgCompositions(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newOrgComposition, id) => newOrgComposition copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(compaignId: Uuid, name: String, during: Range[java.time.Instant], coefficient: Option[Double])(implicit session: jdbc.Q.Session): OrgComposition =
      insertInvoker insert OrgComposition(compaignId, name, during, coefficient)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // ####################################################################################

  class Depts(tag: Tag) extends Table[Dept](tag, "depts") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val name = column[String]("name", O.NotNull)

    val org = column[Uuid]("org", O.NotNull)

    val departmentChefId = column[Option[Uuid]]("department_chef_id")

    def * = (name, org, departmentChefId, id?) <> (Dept.tupled, Dept.unapply)

    val indexName = index("DEPT_NAME_INDEX", (org, name), unique = true)

    val pk = primaryKey("DEPT_PK", id)

    val _org = foreignKey("DEPT_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val departmentChef = foreignKey("DEPT_EMPLOYEE_FK", departmentChefId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)
  }

  val Depts = new TableQuery(new Depts(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newDept, id) => newDept copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, org: Uuid, departmentChefId: Option[Uuid])(implicit session: jdbc.Q.Session): Dept =
      insertInvoker insert Dept(name, org, departmentChefId)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // #########################################################################################

  class Courses(tag: Tag) extends Table[Course](tag, "courses") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val name = column[String]("name", O.NotNull)

    val code = column[Option[String]]("code")

    def * = (name, code , id?) <> (Course.tupled, Course.unapply)

    val indexName = index("COURSE_NAME_INDEX", name, unique = true)
    
    val indexCode = index("COURSE_CODE_INDEX", code, unique = true)

    val pk = primaryKey("COURSE_PK", id)
  }

  val Courses = new TableQuery(new Courses(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id)

      inserts.insertInvoker
    }

    def insert(name: String, code: Option[String])(implicit session: jdbc.Q.Session): Uuid =
      insertInvoker insert Course(name, code)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  class OrgCourses(tag: Tag) extends Table[OrgCourse](tag, "org_courses") {

    val org = column[Uuid]("org", O.NotNull)
    
    val levels = column[Int]("levels", O.NotNull)

    val deptId = column[Option[Uuid]]("dept_id", O.NotNull)

    val desc = column[Option[String]]("desc", O.DBType("text"))

    val courseId = column[Uuid]("course_id", O.NotNull)

    def * = (org, levels, deptId, desc, courseId) <> (OrgCourse.tupled, OrgCourse.unapply)

    val pk = primaryKey("ORG_COURSE_PK", (org, courseId))

    val course = foreignKey("ORG_COURSE_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _org = foreignKey("ORG_COURSE_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val dept = foreignKey("ORG_COURSE_DEPT_FK", deptId, Depts)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val OrgCourses = TableQuery[OrgCourses]

  // ###################################################################################################  

  class OrgSubjects(tag: Tag) extends Table[OrgSubject](tag, "org_subjects") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val org = column[Uuid]("org", O.NotNull)

    val name = column[String]("name", O.NotNull)

    val desc = column[Option[String]]("desc", O.DBType("text"))

    def * = (org, name, desc, id?) <> (OrgSubject.tupled, OrgSubject.unapply)

    val pk = primaryKey("ORG_SUBJECT_PK", id)

    val indexName = index("ORG_SUBJECT_NAME_INDEX", name)

    val indexOrg = index("ORG_SUBJECT_ORG_NAME_INDEX", (org, name), unique = true)

    val _org = foreignKey("ORG_SUBJECT_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
  }

  val OrgSubjects = new TableQuery(new OrgSubjects(_)) {
    
    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newOrgSubject, id) => newOrgSubject copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(org: Uuid, name: String, desc: Option[String])(implicit session: jdbc.Q.Session): OrgSubject =
      insertInvoker insert OrgSubject(org, name, desc)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete    
  }

  // ###################################################################################################  

/*  class Batches(tag: Tag) extends Table[Batch](tag, "batch") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val name = column[String]("name", O.NotNull)

    val empId = column[Option[Uuid]]("current_emp_id", O.NotNull)

    val courseId = column[Uuid]("course_id", O.NotNull)

    val subjectId = column[Uuid]("subject_id", O.NotNull)

    val compositionId = column[Uuid]("composition_id", O.NotNull)

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (name, empId, courseId, subjectId, compositionId, createdAt, createdBy, id?) <> (Batch.tupled, Batch.unapply)

    val pk = primaryKey("BATCH_PK", id)

    val _createdBy = foreignKey("BATCH_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    val course = foreignKey("BATCH_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val employee = foreignKey("BATCH_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val subject = foreignKey("BATCH_SUBJECT_FK", subjectId, OrgSubjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val composition = foreignKey("BATCH_COMPOSITION_FK", compositionId, OrgCompositions)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Batches = new TableQuery(new Batches(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newBatch, id) => newBatch copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, empId: Option[Uuid], courseId: Uuid, subjectId: Uuid, compositionId: Uuid, createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): Batch =
      insertInvoker insert Batch(name, empId, courseId, subjectId, compositionId, createdBy = createdBy)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }*/

  // ###################################################################################################  

  class Employees(tag: Tag) extends Table[Employee](tag, "employees") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val empNo = column[String]("emp_no", O.NotNull)

    val userId = column[Uuid]("user_id", O.NotNull)

    def * = (empNo, userId, id?) <> (Employee.tupled, Employee.unapply)

    val user = foreignKey("EMPLOYEE_USER_FK", userId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val indexEmpNo = index("EMPLOYEE_EMP_NO_INDEX", empNo, unique = true)

    val pk = primaryKey("EMPLOYEE_PK", id)
  }

  val Employees = new TableQuery(new Employees(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newEmployee, id) => newEmployee copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(empNo: String /* GENERATE THIS */ , userId: Uuid)(implicit session: jdbc.Q.Session): Employee =
      insertInvoker insert Employee(empNo, userId)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  class OrgEmployments(tag: Tag) extends Table[OrgEmployment](tag, "org_employments") {

    val org = column[Uuid]("org", O.NotNull)

    val empId = column[Uuid]("emp_id", O.NotNull)

    val deptId = column[Option[Uuid]]("dept_id")

    val joinDate = column[LocalDate]("join_date", O.NotNull)

    val endDate = column[Option[LocalDateTime]]("end_date")

    val endStatus = column[Option[ClosureStatus]]("end_status")

    val endRemarques = column[Option[String]]("end_remarques")

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (org, empId, deptId, joinDate, endDate, endStatus, endRemarques, createdBy) <> (OrgEmployment.tupled, OrgEmployment.unapply)

    val pk = primaryKey("ORG_EMPLOYEE_PK", (org, empId))

    val _org = foreignKey("ORG_EMPLOYEE_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val _createdBy = foreignKey("ORG_EMPLOYEE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val employee = foreignKey("ORG_EMPLOYEE_EMPLOYEE_FK", empId, Employees)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val dept = foreignKey("ORG_EMPLOYEE_DEPT_FK", deptId, Depts)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val OrgEmployments = new TableQuery(new OrgEmployments(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this

      inserts.insertInvoker
    }

    def insert(org: Uuid, empId: Uuid, deptId: Option[Uuid] = None, joinDate: LocalDate = dateNow, createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): OrgEmployment =
      insertInvoker insert OrgEmployment(org, empId, deptId, joinDate, createdBy = createdBy)

    def deleteForEmployee(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.empId === id)
        .delete
  }

  // ################################################################################################### 

  class Guardians(tag: Tag) extends Table[Guardian](tag, "guardians") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val relation = column[GuardianRelation]("relation")

    val userId = column[Uuid]("user_id", O.NotNull)

    def * = (relation, userId, id?) <> (Guardian.tupled, Guardian.unapply)

    val user = foreignKey("GUARDIAN_USER_FK", userId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val pk = primaryKey("GUARDIAN_PK", id)
  }

  val Guardians = new TableQuery(new Guardians(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newGuardian, id) => newGuardian copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(relation: GuardianRelation, userId: Uuid)(implicit session: jdbc.Q.Session): Guardian =
      insertInvoker insert Guardian(relation, userId)
  }

  // ###################################################################################################  

  class Students(tag: Tag) extends Table[Student](tag, "students") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val regNo = column[String]("reg_no", O.NotNull)

    val dateOB = column[LocalDate]("date_ob", O.NotNull)

    val nationality = column[String]("nationality", O.NotNull)

    val userId = column[Uuid]("user_id", O.NotNull)

    val guardianId = column[Option[Uuid]]("guardian_id")

    def * = (regNo, dateOB, nationality, userId, guardianId, id?) <> (Student.tupled, Student.unapply)

    val indexRegNo = index("STUDENT_REG_NO_INDEX", regNo, unique = true)

    val pk = primaryKey("STUDENT_PK", id)

    val user = foreignKey("STUDENT_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val guardian = foreignKey("STUDENT_GUARDIAN_FK", guardianId, Guardians)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val Students = new TableQuery(new Students(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newStudent, id) => newStudent copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(regNo: String /* GENERATE THIS */ , userId: Uuid, nationality: String, dateOB: LocalDate, guardianId: Option[Uuid] = None)(implicit session: jdbc.Q.Session): Student =
      insertInvoker insert Student(regNo, dateOB, nationality, userId, guardianId)

    def deleteForUser(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.userId === id)
        .delete

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // ###################################################################################################  

  class Admissions(tag: Tag) extends Table[Admission](tag, "admissions") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val org = column[Uuid]("org", O.NotNull)

    val studentId = column[Uuid]("student_id", O.NotNull)

    val courseId = column[Uuid]("course_id", O.NotNull)

    val status = column[InscriptionStatus]("inscription_status", O.NotNull, O.Default(InscriptionStatus.PendingApproval))

    val endStatus = column[Option[ClosureStatus]]("end_status")

    val endRemarques = column[Option[String]]("end_remarques")

    val admDate = column[LocalDate]("adm_date", O.NotNull)

    val endDate = column[Option[LocalDateTime]]("end_date")

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (org, studentId, courseId, status, endStatus, endRemarques, admDate, endDate, createdAt, createdBy, id?) <> (Admission.tupled, Admission.unapply)

    val pk = primaryKey("ADMISSION_PK", id)

    val _createdBy = foreignKey("ADMISSION_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val _org = foreignKey("ADMISSION_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val student = foreignKey("ADMISSION_STUDENT_FK", studentId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val course = foreignKey("ADMISSION_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Admissions = new TableQuery(new Admissions(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newAdmission, id) => newAdmission copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(org: Uuid, studentId: Uuid, courseId: Uuid, createdBy: Option[Uuid], admDate: LocalDate = dateNow)(implicit session: jdbc.Q.Session): Admission =
      insertInvoker insert Admission(org, studentId, courseId, admDate = admDate, createdBy = createdBy)

    def deleteForStudent(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.studentId === id)
        .delete

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // ###################################################################################################  

  class Inscriptions(tag: Tag) extends Table[Inscription](tag, "inscriptions") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val admissionId = column[Uuid]("admission_id", O.NotNull)

    val compaignId = column[Uuid]("compaign_id", O.NotNull)
    
    val level = column[Int]("level", O.NotNull)

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (admissionId, compaignId, level, createdAt, createdBy, id?) <> (Inscription.tupled, Inscription.unapply)

    val pk = primaryKey("INSCRIPTION_PK", id)

    val _createdBy = foreignKey("INSCRIPTION_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val admission = foreignKey("INSCRIPTION_ADMISSION_FK", admissionId, Admissions)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val compaign = foreignKey("INSCRIPTION_COMPAIGN_FK", compaignId, Compaigns)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Inscriptions = new TableQuery(new Inscriptions(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newInscription, id) => newInscription copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(admissionId: Uuid, compaignId: Uuid, level: Int, createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): Inscription =
      insertInvoker insert Inscription(admissionId, compaignId, level, createdBy = createdBy)
  }

  // ###################################################################################################  

  class ControlCategories(tag: Tag) extends Table[ControlCategory](tag, "controls_categories") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val name = column[String]("name", O.NotNull)

    val during = column[com.github.tminglei.slickpg.Range[java.sql.Timestamp]]("during", O.NotNull)

    val compositionId = column[Uuid]("composition_id", O.NotNull)

    val coefficient = column[Option[Double]]("coefficient")

    // TODO: add sql `alter table "controls_categories" add constraint "CONTROL_CATEGORY_DURING" EXCLUDE USING gist ((org :: text) WITH =, during WITH &&)`

    def * = (name, during, compositionId, coefficient, id?) <> (
      { t: (String, com.github.tminglei.slickpg.Range[java.sql.Timestamp], Uuid, Option[Double], Option[Uuid]) =>
        t match {
          case (name, during, compositionId, coefficient, id) =>
            ControlCategory(
              name,
              Range(
                java.time.Instant.ofEpochMilli(during.start.getTime),
                java.time.Instant.ofEpochMilli(during.end.getTime)), compositionId, coefficient, id = id)
        }
      },
      (category: ControlCategory) =>
        Some((
          category.name,
          com.github.tminglei.slickpg.Range[java.sql.Timestamp](
            java.sql.Timestamp.from(category.during.start),
            java.sql.Timestamp.from(category.during.end),
            com.github.tminglei.slickpg.`[_,_]`),
            category.compositionId,
            category.coefficient,
            category.id)))    

    val indexName = index("CONTROL_CATEGORY_NAME_INDEX", (compositionId, name), unique = true)

    val pk = primaryKey("CONTROL_CATEGORY_PK", id)

    val composition = foreignKey("CONTROL_CATEGORY_FK", compositionId, OrgCompositions)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val ControlCategories = new TableQuery(new ControlCategories(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newControlCategory, id) => newControlCategory copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, during: Range[java.time.Instant], compositionId: Uuid, coefficient: Option[Double])(implicit session: jdbc.Q.Session): ControlCategory =
      insertInvoker insert ControlCategory(name, during, compositionId, coefficient)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete
  }

  // --------------------------------------------------------------------------------------------

  class Modules(tag: Tag) extends Table[Module](tag, "modules") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val org = column[Uuid]("org", O.NotNull)

    val name = column[String]("name", O.NotNull)

    def * = (org, name, id?) <> (Module.tupled, Module.unapply)

    val pk = primaryKey("MODULE_PK", id)
    
    val indexOrg = index("MODULE_ORG_NAME_INDEX", (org, name), unique = true)

    val _org = foreignKey("MODULE_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)
  }

  val Modules = new TableQuery(new Modules(_)) {
    
    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id)

      inserts.insertInvoker
    }

    def insert(org: Uuid, name: String)(implicit session: jdbc.Q.Session): Uuid =
      insertInvoker insert Module(org, name)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete    
  }

  // --------------------------------------------------------------------------------------------

  class OrgCoursesModules(tag: Tag) extends Table[OrgCourseModule](tag, "org_courses_modules") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val compaignId = column[Uuid]("compaign_id", O.NotNull)

    val compositionId = column[Uuid]("composition_id", O.NotNull)

    val courseId = column[Uuid]("course_id", O.NotNull)

    val moduleId = column[Uuid]("module_id", O.NotNull)

    val level = column[Int]("level", O.NotNull)

    val coefficient = column[Option[Double]]("coefficient")

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")    

    def * = (compaignId, compositionId, courseId, moduleId, level, coefficient, createdAt, createdBy, id?) <> (OrgCourseModule.tupled, OrgCourseModule.unapply)

    val pk = primaryKey("ORG_COURSE_MODULE_PK", id)
    
    val indexPK = index("ORG_COURSE_MODULE_PK_INDEX", (compaignId, compositionId, courseId, moduleId, level), unique = true)

    val cCompaign = foreignKey("ORG_COURSE_MODULE_COMPOSITION_COMPAIGN_FK", (compositionId, compaignId), OrgCompositions)(composition => (composition.id, composition.compaignId), ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val course = foreignKey("ORG_COURSE_MODULE_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val module = foreignKey("ORG_COURSE_MODULE_MODULE_FK", moduleId, Modules)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _createdBy = foreignKey("ORG_COURSE_MODULE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val OrgCoursesModules = new TableQuery(new OrgCoursesModules(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id)

      inserts.insertInvoker
    }

    def insert(compaignId: Uuid, compositionId: Uuid, courseId: Uuid, moduleId: Uuid, level: Int, coefficient: Option[Double], createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): Uuid =
      insertInvoker insert OrgCourseModule(compaignId, compositionId, courseId, moduleId, level, coefficient, createdBy = createdBy)
  }

  // --------------------------------------------------------------------------------------------

  class Batches(tag: Tag) extends Table[Batch](tag, "batch") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val empId = column[Option[Uuid]]("current_emp_id", O.NotNull)

    val compaignId = column[Uuid]("compaign_id", O.NotNull)

    val compositionId = column[Uuid]("composition_id", O.NotNull)

    val courseId = column[Uuid]("course_id", O.NotNull)

    val moduleId = column[Uuid]("module_id", O.NotNull)

    val subjectId = column[Uuid]("subject_id", O.NotNull)

    val level = column[Int]("level", O.NotNull)

    val coefficient = column[Option[Double]]("coefficient")

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")    

    def * = (empId, compaignId, compositionId, courseId, moduleId, subjectId, level, coefficient, createdAt, createdBy, id?) <> (Batch.tupled, Batch.unapply)

    val pk = primaryKey("ORG_MODULE_SUBJECT_PK", id)
    
    val indexPK = index("ORG_MODULE_SUBJECT_PK_INDEX", (compaignId, compositionId, courseId, moduleId, subjectId, level), unique = true)

    val employee = foreignKey("ORG_MODULE_SUBJECT_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val orgCoursesModules = foreignKey("ORG_MODULE_SUBJECT_OrgCoursesModules_FK", (compaignId, compositionId, courseId, moduleId, level), OrgCoursesModules)(o => (o.compaignId, o.compositionId, o.courseId, o.moduleId, o.level), ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val subject = foreignKey("ORG_MODULE_SUBJECT_SUBJECT_FK", subjectId, OrgSubjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _createdBy = foreignKey("ORG_MODULE_SUBJECT_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val Batches = new TableQuery(new Batches(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newBatch, id) => newBatch copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(empId: Option[Uuid], compaignId: Uuid, compositionId: Uuid, courseId: Uuid, moduleId: Uuid, subjectId: Uuid, level: Int, coefficient: Option[Double], createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): Batch =
      insertInvoker insert Batch(empId, compaignId, compositionId, courseId, moduleId, subjectId, level, coefficient, createdBy = createdBy)        

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete      
  }

  // ----------------------------------------------------------------------------------------------------------------------------

  class TeachingHistories(tag: Tag) extends Table[TeachingHistory](tag, "teaching_histories") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val empId = column[Uuid]("emp_id", O.NotNull)

    val batchId = column[Uuid]("batch_id", O.NotNull)

    val startDate = column[LocalDate]("start_date", O.NotNull)

    val endDate = column[LocalDateTime]("end_date")

    val createdAt = column[LocalDateTime]("end_date")

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (empId, batchId, startDate, endDate, createdAt, createdBy, id?) <> (TeachingHistory.tupled, TeachingHistory.unapply)

    val pk = primaryKey("TEACHING_HISTORY_PK", id)

    val _createdBy = foreignKey("TEACHING_HISTORY_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val employee = foreignKey("TEACHING_HISTORY_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val batch = foreignKey("TEACHING_HISTORY_BATCH_FK", batchId, /*Batches*/Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val TeachingHistories = new TableQuery(new TeachingHistories(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newTeachingHistory, id) => newTeachingHistory copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(empId: Uuid, batchId: Uuid, startDate: LocalDate, endDate: LocalDateTime, createdAt: LocalDateTime, createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): TeachingHistory =
      insertInvoker insert TeachingHistory(empId, batchId, startDate, endDate, createdAt, createdBy = createdBy)
  }

  // ###################################################################################################  

  class Controls(tag: Tag) extends Table[Control](tag, "controls") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val eventId = column[Uuid]("event_id", O.NotNull)

    val name = column[String]("name", O.NotNull)

    val batchId = column[Uuid]("batch_id", O.NotNull)

    val supervisors = column[List[Uuid]]("supervisors", O.NotNull, O.Default(List()))

    val `type` = column[Uuid]("type", O.NotNull)

    val coefficient = column[Option[Double]]("coefficient")

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (eventId, name, batchId, supervisors, `type`, coefficient, createdAt, createdBy, id?) <> (Control.tupled, Control.unapply)

    val pk = primaryKey("CONTROL_PK", id)

    val _createdBy = foreignKey("TIMETABLE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    lazy val event = foreignKey("CONTROL_EVENT_FK", eventId, Timetables)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val batch = foreignKey("CONTROL_BATCH_FK", batchId, /*Batches*/Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _type = foreignKey("CONTROL_TYPE_FK", `type`, ControlCategories)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Controls = new TableQuery(new Controls(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newControl, id) => newControl copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, eventId: Uuid, batchId: Uuid, supervisors: List[Uuid], `type`: Uuid, coefficient: Option[Double], createdBy: Option[Uuid])(implicit session: jdbc.Q.Session): Control =
      insertInvoker insert Control(eventId, name, batchId, supervisors, `type`, coefficient, createdBy = createdBy)

    def delete(id: Uuid)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === id)
        .delete      
  }

  // ###################################################################################################  

  class Marks(tag: Tag) extends Table[Mark](tag, "marks") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val studentId = column[Uuid]("student_id", O.NotNull)

    val empId = column[Uuid]("emp_id", O.NotNull)

    val controlId = column[Uuid]("exam_id", O.NotNull)

    val marks = column[Double]("marks", O.NotNull)

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    def * = (studentId, empId, controlId, marks, createdAt, id?) <> (Mark.tupled, Mark.unapply)

    val pk = primaryKey("MARK_PK", id)

    val student = foreignKey("MARK_STUDENT_FK", studentId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val employee = foreignKey("MARK_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val exam = foreignKey("MARK_CONTROL_FK", controlId, Controls)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Marks = new TableQuery(new Marks(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newMark, id) => newMark copy (id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(studentId: Uuid, empId: Uuid, controlId: Uuid, marks: Double, createdAt: LocalDateTime = now)(implicit session: jdbc.Q.Session): Mark =
      insertInvoker insert Mark(studentId, empId, controlId, marks, createdAt)
  }  

  // ###################################################################################################  

  class Timetables(tag: Tag) extends Table[Timetable](tag, "events") {

    val id = column[Uuid]("id", O.DBType("uuid"), O.AutoInc)

    val batchId = column[Uuid]("batch_id", O.NotNull)

    val `type` = column[TimetableEventType]("type", O.NotNull, O.Default(TimetableEventType.Lecture))

    val `class` = column[String]("class", O.NotNull)

    val during = column[com.github.tminglei.slickpg.Range[java.sql.Timestamp]]("during", O.NotNull)    

    val recurrence = column[Recurrence]("recurrence", O.NotNull, O.Default(Recurrence.None))

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (batchId, `type`, `class`, during, recurrence, createdAt, createdBy, id?) <> (
      { t: (Uuid, TimetableEventType, String, com.github.tminglei.slickpg.Range[java.sql.Timestamp], Recurrence, LocalDateTime, Option[Uuid], Option[Uuid]) =>
        t match {
          case (batchId, ttype, cclass, during, recurrence, createdAt, createdBy, id) =>
            Timetable(
              batchId,
              ttype,
              cclass,              
              Range(
                java.time.Instant.ofEpochMilli(during.start.getTime),
                java.time.Instant.ofEpochMilli(during.end.getTime)), 
              recurrence, 
              createdAt, 
              createdBy, id = id)
        }
      },
      (timetable: Timetable) =>
        Some((
          timetable.batchId,
          timetable.`type`,
          timetable.`class`,              
          com.github.tminglei.slickpg.Range[java.sql.Timestamp](
            java.sql.Timestamp.from(timetable.during.start),
            java.sql.Timestamp.from(timetable.during.end),
            com.github.tminglei.slickpg.`[_,_]`),
          timetable.recurrence,
          timetable.createdAt,
          timetable.createdBy,
          timetable.id)))

    val pk = primaryKey("TIMETABLE_EVENT_PK", id)

    val _createdBy = foreignKey("TIMETABLE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    val batch = foreignKey("TIMETABLE_EVENT_BATCH_FK", batchId, /*Batches*/Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Timetables = new TableQuery(new Timetables(_)) {}

  // ################################################################################################################################

  class Attendances(tag: Tag) extends Table[Attendance](tag, "attendances") {

    val userId = column[Uuid]("user_id", O.NotNull)

    val timetableId = column[Uuid]("event_id", O.NotNull)

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[Uuid]]("created_by")

    def * = (userId, timetableId, createdAt, createdBy) <> (Attendance.tupled, Attendance.unapply)

    val _createdBy = foreignKey("ATTENDANCE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    val pk = primaryKey("ATTENDANCE_PK", (userId, timetableId))

    val user = foreignKey("ATTENDANCE_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val timetable = foreignKey("ATTENDANCE_TIMETABLE_EVENT_FK", timetableId, Timetables)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Attendances = new TableQuery(new Attendances(_)) {}  
}