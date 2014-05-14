package ma.epsilon.schola
package school

package schema

object `package` {

  import jdbc.Q._

  import ma.epsilon.schola.domain._
  import ma.epsilon.schola.school.domain._

  import ma.epsilon.schola.conversions.jdbc._

  import scala.slick.model.ForeignKeyAction

  // #############################################################################################

  class Users(tag: Tag) extends Table[(java.util.UUID)](tag, "users") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    def * = id

    val pk = primaryKey("USER_PK", id)
  }

  val Users = TableQuery[Users]  

  // ##############################################################################################

  class Universities(tag: Tag) extends Table[University](tag, "universities") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val contacts = column[ContactInfo]("contacts", O.NotNull, O.DBType("json"), O.Default(ContactInfo()))

    val address = column[AddressInfo]("address", O.NotNull, O.DBType("json"), O.Default(AddressInfo()))

    def * = (name, contacts, address, id?) <> (University.tupled, University.unapply)

    val indexName = index("UNIVERSITY_NAME_INDEX", name, unique = true)

    val pk = primaryKey("UNIVERSITY_PK", id)    
  }

  val Universities = new TableQuery[Universities](new Universities(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newUniversity, id) =>
            University(
              newUniversity.name,
              newUniversity.contacts,
              newUniversity.address, 
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, contacts: ContactInfo = ContactInfo(), address: AddressInfo = AddressInfo())(implicit session: jdbc.Q.Session): University =
      insertInvoker insert University(name, contacts, address)
  }  

  // ################################################################################################


  class Orgs(tag: Tag) extends Table[Org](tag, "orgs") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val contacts = column[ContactInfo]("contacts", O.DBType("json"))

    val address = column[AddressInfo]("address", O.DBType("json"))

    val universityId = column[Option[Long]]("university_id")

    val _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def * = (name, contacts, address, universityId, _deleted, createdAt, createdBy, id?) <> (Org.tupled, Org.unapply)

    val _createdBy = foreignKey("ORG_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)    

    val indexName = index("ORG_NAME_INDEX", name, unique = true)

    val pk = primaryKey("ORG_PK", id)

    val university = foreignKey("ORG_UNIVERSITY_FK", universityId, Universities)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val Orgs = new TableQuery(new Orgs(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newOrg, id) =>
            Org(
              newOrg.name,
              newOrg.contacts,
              newOrg.address, 
              newOrg.universityId,
              newOrg._deleted,
              newOrg.createdAt,
              newOrg.createdBy,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    private val forDeletion = {
      def getDeleted(id: Column[Long]) =
        this
          .filter(_.id === id)
          .map(_._deleted)

      Compiled(getDeleted _)
    }

    def insert(name: String, universityId: Option[Long], contacts: ContactInfo = ContactInfo(), address: AddressInfo = AddressInfo(), createdBy: Option[String])(implicit session: jdbc.Q.Session): Org =
      insertInvoker insert Org(name, contacts, address, universityId, createdBy = createdBy map uuid)

    def delete(id: Long)(implicit session: jdbc.Q.Session) =
      forDeletion(id).update(true) == 1
  }

  // ####################################################################################

  class Depts(tag: Tag) extends Table[Dept](tag, "depts") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val org = column[Long]("org", O.NotNull)

    val departmentChefId = column[Option[java.util.UUID]]("department_chef_id")

    def * = (name, org, departmentChefId, id?) <> (Dept.tupled, Dept.unapply)

    val indexName = index("DEPT_NAME_INDEX", name, unique = true)

    val pk = primaryKey("DEPT_PK", id)

    val _org = foreignKey("DEPT_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)    
    
    val departmentChef = foreignKey("DEPT_EMPLOYEE_FK", departmentChefId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)
  }

  val Depts = new TableQuery(new Depts(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newDept, id) =>
            Dept(
              newDept.name,
              newDept.org,
              newDept.departmentChefId,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, org: Long, departmentChefId: Option[String])(implicit session: jdbc.Q.Session): Dept =
      insertInvoker insert Dept(name, org, departmentChefId map uuid)
  }

  // #########################################################################################


  class Courses(tag: Tag) extends Table[Course](tag, "courses") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val code = column[String]("org", O.NotNull)

    def * = (name, code, id?) <> (Course.tupled, Course.unapply)

    val indexName = index("COURSE_NAME_INDEX", name, unique = true)

    val pk = primaryKey("COURSE_PK", id)
  }

  val Courses = new TableQuery(new Courses(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newCourse, id) =>
            Course(
              newCourse.name,
              newCourse.code,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, code: String)(implicit session: jdbc.Q.Session): Course =
      insertInvoker insert Course(name, code)
  }

  class OrgCourses(tag: Tag) extends Table[OrgCourse](tag, "org_courses") {

    val org = column[Long]("org", O.NotNull)

    val courseId = column[Long]("course_id", O.NotNull)

    def * = (org, courseId) <> (OrgCourse.tupled, OrgCourse.unapply)

    val pk = primaryKey("ORG_COURSE_PK", (org, courseId))

    val course = foreignKey("ORG_COURSE_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _org = foreignKey("ORG_COURSE_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
  }

  val OrgCourses = TableQuery[OrgCourses]

  // ###################################################################################################  
  

  class Subjects(tag: Tag) extends Table[Subject](tag, "subjects") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    def * = (name, id?) <> (Subject.tupled, Subject.unapply)

    val indexName = index("SUBJECT_NAME_INDEX", name, unique = true)

    val pk = primaryKey("SUBJECT_PK", id)
  }

  val Subjects = new TableQuery(new Subjects(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newSubject, id) =>
            Subject(
              newSubject.name,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String)(implicit session: jdbc.Q.Session): Subject =
      insertInvoker insert Subject(name)
  }    

  class OrgSubjects(tag: Tag) extends Table[OrgSubject](tag, "org_subjects") {

    val org = column[Long]("org", O.NotNull)

    val subjectId = column[Long]("subject_id", O.NotNull)

    def * = (org, subjectId) <> (OrgSubject.tupled, OrgSubject.unapply)

    val pk = primaryKey("ORG_SUBJECT_PK", (org, subjectId))

    val subject = foreignKey("ORG_SUBJECT_SUBJECT_FK", subjectId, Subjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _org = foreignKey("ORG_SUBJECT_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
  }

  val OrgSubjects = TableQuery[OrgSubjects]

  // ###################################################################################################  
  
  class Batches(tag: Tag) extends Table[Batch](tag, "batch") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val org = column[Long]("org", O.NotNull)

    val name = column[String]("name", O.NotNull)

    val courseId = column[Long]("course_id", O.NotNull)

    val empId = column[java.util.UUID]("emp_id", O.DBType("uuid"), O.NotNull)

    val subjectId = column[Long]("subject_id", O.NotNull)    

    val startDate = column[java.time.LocalDate]("start_date", O.NotNull)

    val endDate = column[java.time.LocalDate]("end_date", O.NotNull)

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))    

    def * = (org, name, courseId, empId, subjectId, startDate, endDate, createdAt, createdBy, id?) <> (Batch.tupled, Batch.unapply)

    val indexName = index("BATCH_NAME_INDEX", name, unique = true)

    val pk = primaryKey("BATCH_PK", id)

    val _createdBy = foreignKey("BATCH_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    val course = foreignKey("BATCH_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _org = foreignKey("BATCH_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade) 

    val employee = foreignKey("EMPLOYEE_SUBJECT_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val subject = foreignKey("EMPLOYEE_SUBJECT_SUBJECT_FK", subjectId, Subjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)    
  }

  val Batches = new TableQuery(new Batches(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newBatch, id) =>
            Batch(
              newBatch.org,
              newBatch.name,
              newBatch.courseId,
              newBatch.empId,
              newBatch.subjectId,
              newBatch.startDate,
              newBatch.endDate,
              newBatch.createdAt,
              newBatch.createdBy,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(org: Long, name: String, courseId: Long, empId: String, subjectId: Long, startDate: java.time.LocalDate, endDate: java.time.LocalDate, createdBy: Option[String])(implicit session: jdbc.Q.Session): Batch =
      insertInvoker insert Batch(org, name, courseId, uuid(empId), subjectId, startDate, endDate, createdBy = createdBy map uuid)
  }  


  // ###################################################################################################  
  

  class Employees(tag: Tag) extends Table[Employee](tag, "employees") {
    
    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val empNo = column[String]("emp_no", O.NotNull)

    val joinDate = column[java.time.LocalDate]("join_date", O.NotNull)

    val jobTitle = column[String]("job_title", O.NotNull)
    
    val userId = column[java.util.UUID]("user_id", O.DBType("uuid"), O.NotNull)

    val deptId = column[Option[Long]]("dept_id")

    def * = (empNo, joinDate, jobTitle, userId, deptId, id?) <> (Employee.tupled, Employee.unapply)

    val user = foreignKey("EMPLOYEE_USER_FK", userId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val dept = foreignKey("EMPLOYEE_DEPT_FK", deptId, Depts)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val indexEmpNo = index("EMPLOYEE_EMP_NO_INDEX", empNo, unique = true)

    val pk = primaryKey("EMPLOYEE_PK", id)
  }

  val Employees = new TableQuery(new Employees(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newEmployee, id) =>
            Employee(
              newEmployee.empNo,
              newEmployee.joinDate,
              newEmployee.jobTitle,
              newEmployee.userId,
              newEmployee.deptId,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(empNo: String /* GENERATE THIS */, userId: String, jobTitle: String, deptId: Option[Long] = None, joinDate: java.time.LocalDate = java.time.LocalDate.now)(implicit session: jdbc.Q.Session): Employee =
      insertInvoker insert Employee(empNo, joinDate, jobTitle, uuid(userId), deptId)
  }    

  class OrgEmployees(tag: Tag) extends Table[OrgEmployee](tag, "orgs_employees") {

    val org = column[Long]("org", O.NotNull)

    val empId = column[java.util.UUID]("emp_id", O.DBType("uuid"), O.NotNull)

    val startDate = column[java.time.LocalDate]("start_date", O.NotNull)
    
    val endDate = column[Option[java.time.LocalDateTime]]("end_date")

    val endStatus = column[Option[ClosureStatus]]("end_status")

    val endRemarques = column[Option[String]]("end_remarques")

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def * = (org, empId, startDate, endDate, endStatus, endRemarques, createdBy) <> (OrgEmployee.tupled, OrgEmployee.unapply)

    val pk = primaryKey("ORG_EMPLOYEE_PK", (org, empId))

    val _org = foreignKey("ORG_EMPLOYEE_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val _createdBy = foreignKey("ORG_EMPLOYEE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val employee = foreignKey("ORG_EMPLOYEE_EMPLOYEE_FK", empId, Employees)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val OrgEmployees = TableQuery[OrgEmployees]

  class EmployeesSubjects(tag: Tag) extends Table[EmployeeSubject](tag, "employees_subjects") {

    val empId = column[java.util.UUID]("emp_id", O.DBType("uuid"), O.NotNull)
    
    val batchId = column[Long]("batch_id", O.NotNull)

    val startDate = column[java.time.LocalDate]("start_date", O.NotNull)
    
    val endDate = column[Option[java.time.LocalDateTime]]("end_date")

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def * = (empId, batchId, startDate, endDate, createdAt, createdBy) <> (EmployeeSubject.tupled, EmployeeSubject.unapply)

    val pk = primaryKey("EMPLOYEE_SUBJECT_PK", (empId, batchId))

    val _createdBy = foreignKey("EMPLOYEE_SUBJECT_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val employee = foreignKey("EMPLOYEE_SUBJECT_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val batch = foreignKey("EMPLOYEE_SUBJECT_BATCH_FK", batchId, Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val EmployeesSubjects = TableQuery[EmployeesSubjects]  

  // ################################################################################################### 

  class Guardians(tag: Tag) extends Table[Guardian](tag, "guardians") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val occupation = column[Option[String]]("occupation")

    val relation = column[GuardianRelation]("relation")

    val userId = column[java.util.UUID]("user_id", O.DBType("uuid"), O.NotNull)

    def * = (occupation, relation, userId, id?) <> (Guardian.tupled, Guardian.unapply)

    val user = foreignKey("GUARDIAN_USER_FK", userId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val pk = primaryKey("GUARDIAN_PK", id)
  }

  val Guardians = new TableQuery(new Guardians(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newGuardian, id) =>
            Guardian(
              newGuardian.occupation,
              newGuardian.relation,
              newGuardian.userId,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(occupation: Option[String], relation: GuardianRelation, userId: String)(implicit session: jdbc.Q.Session): Guardian =
      insertInvoker insert Guardian(occupation, relation, uuid(userId))
  }  

  // ###################################################################################################  

  
  class Students(tag: Tag) extends Table[Student](tag, "students") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val regNo = column[String]("reg_no", O.NotNull)

    val dateOB = column[java.time.LocalDate]("date_ob", O.NotNull)

    val nationality = column[String]("nationality", O.NotNull)

    val userId = column[java.util.UUID]("user_id", O.DBType("uuid"), O.NotNull)

    val guardianId = column[Option[java.util.UUID]]("guardian_id", O.DBType("uuid"))

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
          case (newStudent, id) =>
            Student(
              newStudent.regNo,
              newStudent.dateOB,
              newStudent.nationality,
              newStudent.userId,
              newStudent.guardianId,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(regNo: String /* GENERATE THIS */, userId: String, nationality: String, dateOB: java.time.LocalDate, guardianId: Option[String] = None)(implicit session: jdbc.Q.Session): Student =
      insertInvoker insert Student(regNo, dateOB, nationality, uuid(userId), guardianId map uuid)
  }  

  // ###################################################################################################  
 
  
  class Admissions(tag: Tag) extends Table[Admission](tag, "admissions") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val org = column[Long]("org", O.NotNull)

    val studentId = column[java.util.UUID]("student_id", O.DBType("uuid"), O.NotNull)

    val courseId = column[Long]("course_id", O.NotNull)
    
    val status = column[InscriptionStatus]("inscription_status", O.NotNull, O.Default(InscriptionStatus.PendingApproval))

    val endStatus = column[Option[ClosureStatus]]("end_status")

    val endRemarques = column[Option[String]]("end_remarques")

    val admDate = column[java.time.LocalDate]("adm_date", O.NotNull)
    
    val endDate = column[Option[java.time.LocalDateTime]]("end_date")

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def * = (org, studentId, courseId, status, endStatus, endRemarques, admDate, endDate, createdAt, createdBy, id?) <> (Admission.tupled, Admission.unapply)

    val pk = primaryKey("ADMISSION_PK", id)

    val _createdBy = foreignKey("ADMISSION_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val _org = foreignKey("ADMISSION_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
    
    val student = foreignKey("ADMISSION_STUDENT_FK", studentId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)
    
    val course = foreignKey("ADMISSION_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Admissions = new TableQuery(new Admissions(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newAdmission, id) =>
            Admission(
              newAdmission.org,
              newAdmission.studentId,
              newAdmission.courseId,
              newAdmission.status,
              newAdmission.endStatus,
              newAdmission.endRemarques,
              newAdmission.admDate,
              newAdmission.endDate,
              newAdmission.createdAt,
              newAdmission.createdBy,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(org: Long, studentId: String, courseId: Long, createdBy: Option[String], admDate: java.time.LocalDate = java.time.LocalDate.now)(implicit session: jdbc.Q.Session): Admission =
      insertInvoker insert Admission(org, uuid(studentId), courseId, admDate = admDate, createdBy = createdBy map uuid)
  }

  // ###################################################################################################  

  
  class Inscriptions(tag: Tag) extends Table[Inscription](tag, "inscriptions") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val admissionId = column[java.util.UUID]("admission_id", O.DBType("uuid"), O.NotNull)
    
    val batchId = column[Long]("batch_id", O.NotNull)

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def * = (admissionId, batchId, createdAt, createdBy, id?) <> (Inscription.tupled, Inscription.unapply)

    val pk = primaryKey("INSCRIPTION_PK", id)

    val _createdBy = foreignKey("INSCRIPTION_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val admission = foreignKey("INSCRIPTION_ADMISSION_FK", admissionId, Admissions)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
    
    val batch = foreignKey("INSCRIPTION_BATCH_FK", batchId, Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Inscriptions = new TableQuery(new Inscriptions(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newInscription, id) =>
            Inscription(
              newInscription.admissionId,
              newInscription.batchId,
              newInscription.createdAt,
              newInscription.createdBy,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(admissionId: String, batchId: Long, createdBy: Option[String])(implicit session: jdbc.Q.Session): Inscription =
      insertInvoker insert Inscription(uuid(admissionId), batchId, createdBy = createdBy map uuid)
  }


  // ###################################################################################################  


  class ExamCategories(tag: Tag) extends Table[ExamCategory](tag, "exams_categories") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val startDate = column[java.time.MonthDay]("start_date", O.NotNull)

    val endDate = column[java.time.MonthDay]("end_date", O.NotNull)

    def * = (name, startDate, endDate, id?) <> (ExamCategory.tupled, ExamCategory.unapply)

    val indexName = index("EXAM_CATEGORY_NAME_INDEX", name, unique = true)

    val pk = primaryKey("EXAM_CATEGORY_PK", id)
  }

  val ExamCategories = new TableQuery(new ExamCategories(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newExamCategory, id) =>
            ExamCategory(
              newExamCategory.name,
              newExamCategory.startDate,
              newExamCategory.endDate, 
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, startDate: java.time.MonthDay, endDate: java.time.MonthDay)(implicit session: jdbc.Q.Session): ExamCategory =
      insertInvoker insert ExamCategory(name, startDate, endDate)
  }

  // ###################################################################################################  
  

  class Exams(tag: Tag) extends Table[Exam](tag, "exams") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)
    
    val eventId = column[Long]("event_id", O.NotNull)

    val org = column[Long]("org", O.NotNull)
    
    val name = column[String]("name", O.NotNull)

    val subjectId = column[Long]("subject_id", O.NotNull)

    val batchId = column[Long]("batch_id", O.NotNull)

    val staffs = column[List[Long]]("staffs", O.NotNull, O.Default(List()))
    
    val `type` = column[Long]("type", O.NotNull)

    val date = column[java.time.LocalDate]("date", O.NotNull)
    
    val startTime = column[java.time.LocalTime]("start_time", O.NotNull)

    val duration = column[java.time.Duration]("duration", O.NotNull)

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))    

    def * = (eventId, org, name, subjectId, batchId, staffs, `type`, date, startTime, duration, createdAt, createdBy, id?) <> (Exam.tupled, Exam.unapply)

    val indexName = index("EXAM_NAME_INDEX", name, unique = true)

    val pk = primaryKey("EXAM_PK", id)

    val _createdBy = foreignKey("TIMETABLE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    lazy val event = foreignKey("EXAM_EVENT_FK", eventId, TimetableEvents)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict) 
    
    val _org = foreignKey("EXAM_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade) 

    val subject = foreignKey("EXAM_SUBJECT_FK", subjectId, Subjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val batch = foreignKey("EXAM_BATCH_FK", batchId, Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val _type = foreignKey("EXAM_TYPE_FK", `type`, ExamCategories)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Exams = new TableQuery(new Exams(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newExam, id) =>
            Exam(
              newExam.org,
              newExam.eventId,
              newExam.name,
              newExam.subjectId, 
              newExam.batchId, 
              newExam.staffs, 
              newExam.`type`, 
              newExam.date, 
              newExam.startTime, 
              newExam.duration, 
              newExam.createdAt, 
              newExam.createdBy, 
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(name: String, eventId: Long, org: Long, subjectId: Long, batchId: Long, staffs: List[Long], `type`: Long, date: java.time.LocalDate, startTime: java.time.LocalTime, duration: java.time.Duration, createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now, createdBy: Option[String])(implicit session: jdbc.Q.Session): Exam =
      insertInvoker insert Exam(org, eventId, name, subjectId, batchId, staffs, `type`, date, startTime, duration, createdAt, createdBy map uuid)
  }  

  // ###################################################################################################  


  class Marks(tag: Tag) extends Table[Mark](tag, "marks") {
    
    val id = column[Long]("id", O.AutoInc, O.NotNull)

    val studentId = column[java.util.UUID]("student_id", O.DBType("uuid"), O.NotNull)

    val empId = column[java.util.UUID]("emp_id", O.DBType("uuid"), O.NotNull)

    val examId = column[Long]("exam_id", O.NotNull)

    val marks = column[Double]("marks", O.NotNull)
    
    val status = column[Boolean]("status", O.NotNull, O.Default(false)) // TODO: computed

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    def * = (studentId, empId, examId, marks, status, createdAt, id?) <> (Mark.tupled, Mark.unapply)

    val pk = primaryKey("MARK_PK", id)

    val student = foreignKey("MARK_STUDENT_FK", studentId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Cascade)

    val employee = foreignKey("MARK_EMPLOYEE_FK", empId, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.Restrict)

    val exam = foreignKey("MARK_EXAM_FK", examId, Exams)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Marks = new TableQuery(new Marks(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newMark, id) =>
            Mark(
              newMark.studentId,
              newMark.empId,
              newMark.examId, 
              newMark.marks, 
              newMark.status,
              newMark.createdAt,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(studentId: String, empId: String, examId: Long, marks: Double, status: Boolean, createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now)(implicit session: jdbc.Q.Session): Mark =
      insertInvoker insert Mark(uuid(studentId), uuid(empId), examId, marks, status, createdAt)
  }


  // ###################################################################################################  


  class Timetables(tag: Tag) extends Table[Timetable](tag, "timetables") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)
    
    val org = column[Long]("org", O.NotNull)

    val courseId = column[Long]("course_id", O.NotNull)

    val subjectId = column[Long]("subject_id", O.NotNull)

    val batchId = column[Long]("batch_id", O.NotNull)

    val dayOfWeek = column[java.time.DayOfWeek]("day_of_week", O.NotNull)

    val `type` = column[TimetableEventType]("type", O.NotNull, O.Default(TimetableEventType.Lecture))
    
    val startTime = column[java.time.LocalTime]("start_time", O.NotNull)

    val endTime = column[java.time.LocalTime]("end_time", O.NotNull)
        
    def * = (org, courseId, subjectId, batchId, dayOfWeek, `type`, startTime, endTime, id?) <> (Timetable.tupled, Timetable.unapply)

    val pk = primaryKey("TIMETABLE_PK", (org, courseId, subjectId, batchId, dayOfWeek, `type`, startTime, endTime))

    val indexId = index("TIMETABLE_ID_INDEX", id, unique = true) 

    val _org = foreignKey("TIMETABLE_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val course = foreignKey("TIMETABLE_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val subject = foreignKey("TIMETABLE_SUBJECT_FK", subjectId, Subjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val batch = foreignKey("TIMETABLE_BATCH_FK", batchId, Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Timetables = new TableQuery(new Timetables(_)) {

    private val insertInvoker = {
      val inserts =
        this returning this.map(_.id) into {
          case (newTimetable, id) =>
            Timetable(
              newTimetable.org,
              newTimetable.courseId,
              newTimetable.subjectId, 
              newTimetable.batchId, 
              newTimetable.dayOfWeek,
              newTimetable.`type`,
              newTimetable.startTime,
              newTimetable.endTime,
              id = Some(id))
        }

      inserts.insertInvoker
    }

    def insert(org: Long, courseId: Long, subjectId: Long, batchId: Long, dayOfWeek: java.time.DayOfWeek, `type`: TimetableEventType, startTime: java.time.LocalTime, endTime: java.time.LocalTime)(implicit session: jdbc.Q.Session): Timetable =
      insertInvoker insert Timetable(org, courseId, subjectId, batchId, dayOfWeek, `type`, startTime, endTime)
  }  

  // ################################################################################################################################
  
  class TimetableEvents(tag: Tag) extends Table[TimetableEvent](tag, "timetables_events") {

    val id = column[Long]("id", O.AutoInc, O.NotNull)
    
    val org = column[Long]("org", O.NotNull)

    val courseId = column[Long]("course_id", O.NotNull)

    val subjectId = column[Long]("subject_id", O.NotNull)

    val batchId = column[Long]("batch_id", O.NotNull)

    val `type` = column[TimetableEventType]("type", O.NotNull, O.Default(TimetableEventType.Lecture))
    
    val date = column[java.time.LocalDate]("date", O.NotNull)

    val startTime = column[java.time.LocalTime]("start_time", O.NotNull)

    val endTime = column[java.time.LocalTime]("end_time", O.NotNull) 

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))       
    
    def * = (org, courseId, subjectId, batchId, `type`, date, startTime, endTime, createdAt, createdBy, id?) <> (TimetableEvent.tupled, TimetableEvent.unapply)

    val pk = primaryKey("TIMETABLE_EVENT_PK", id) 

    val _createdBy = foreignKey("TIMETABLE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    val _org = foreignKey("TIMETABLE_EVENT_ORG_FK", org, Orgs)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val course = foreignKey("TIMETABLE_EVENT_COURSE_FK", courseId, Courses)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val subject = foreignKey("TIMETABLE_EVENT_SUBJECT_FK", subjectId, Subjects)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    val batch = foreignKey("TIMETABLE_EVENT_BATCH_FK", batchId, Batches)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val TimetableEvents = new TableQuery(new TimetableEvents(_)) {

  }

  // ################################################################################################################################

  class Attendances(tag: Tag) extends Table[Attendance](tag, "attendances") {
    
    val userId = column[java.util.UUID]("user_id", O.DBType("uuid"), O.NotNull)
    
    val timetableEventId = column[Long]("timetable_event_id", O.NotNull)

    val present = column[Boolean]("present", O.NotNull)

    val createdAt = column[java.time.LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def * = (userId, timetableEventId, present, createdAt, createdBy) <> (Attendance.tupled, Attendance.unapply)
    
    val _createdBy = foreignKey("ATTENDANCE_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Restrict, ForeignKeyAction.SetNull)

    val pk = primaryKey("ATTENDANCE_PK", (userId, timetableEventId, present)) 

    val user = foreignKey("ATTENDANCE_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
    
    val timetableEvent = foreignKey("ATTENDANCE_TIMETABLE_EVENT_FK", timetableEventId, TimetableEvents)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)
  }

  val Attendances = new TableQuery(new Attendances(_)) {
    
  }
}