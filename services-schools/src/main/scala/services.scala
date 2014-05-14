package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait SchoolServicesComponent {

  val schoolService: SchoolServices

  trait SchoolServices extends Common 
    with Employees 
    with Students 
    with Exams 
    with Timetables 
    with Attendances

  trait Common {

    def cinExists(cin: String): Option[Profile]
    
    def saveUniversity(name: String, contacts: ContactInfo, address: AddressInfo): University
    def updateUniversity(id: Long, spec: UniversitySpec): Boolean
    def delUniversity(id: Long)

    def saveOrg(name: String, university: Option[Long], contacts: ContactInfo, address: AddressInfo): Org
    def updateOrg(id: Long, spec: OrgSpec): Boolean
    def delOrg(id: Long)
    def purgeOrg(id: Long)

    def saveDept(name: String, org: Long): Dept
    def updateDept(id: Long, spec: DeptSpec): Boolean
    def delDept(id: Long)    

    def saveCourse(name: String, code: String, org: Option[Long]): Course
    def getCourses: List[Course]
    def getOrgCourses(org: Long): List[Course]
    def updateCourse(id: Long, spec: CourseSpec): Boolean
    def delCourse(id: Long)  

    def saveSubject(name: String, org:  Option[Long]): Subject
    def getSubjects: List[Subject]
    def getOrgSubjects(org: Long): List[Subject]    
    def updateSubject(id: Long, spec: SubjectSpec): Boolean
    def delSubject(id: Long) 

    def saveBatch(org: Long, name: String, courseId: Long, startDate: java.time.LocalDate, endDate: java.time.LocalDate): Batch
    def updateBatch(id: Long, spec: BatchSpec): Boolean
    def delBatch(id: Long)
  }

  trait Employees {

    def saveEmployee(
      org: Option[Long],
      cin: String, 
      username: String, 
      password: String, 
      givenName: String, 
      familyName: String,  
      jobTitle: String,         
      deptId: Option[Long],
      gender: Gender, 
      homeAddress: Option[AddressInfo], 
      workAddress: Option[AddressInfo], 
      contacts: Option[Contacts], 
      suspended: Boolean,       
      joinDate: java.time.LocalDate,
      createdBy: Option[String],
      changePasswordAtNextLogin: Boolean, accessRights: List[String]): EmployeeInfo

    def updateEmployee(id: String, spec: EmployeeSpec): Boolean
    def fetchEmployees: List[EmployeeInfo]
    def fetchPurgedEmployees: List[EmployeeInfo]
    def fetchEmployee(id: String): Option[EmployeeInfo]
    def fetchEmployeeByCIN(cin: String): Option[EmployeeInfo]
    def searchEmployees(q: String): Option[EmployeeInfo]
    // def fetchEmployeeSubjects(id: String, org: Option[Long]): List[EmployeeSubjectInfo]
    def fetchOrgEmployees(org: Long): List[EmployeeInfo]
    def fetchOrgEmployeeEvents(org: Long, batch: Long, id: String): List[EventInfo]
    // def fetchOrgEmployeeBatches(org: Long, id: String): List[BatchInfo]
    def delEmployee(id: String)                         
    def purgeEmployee(id: String)
  }

  trait Students {

    def addStudent(
      cin: String, 
      dateOB: java.time.LocalDate,      
      username: String, 
      password: String, 
      givenName: String, 
      familyName: String,  
      gender: Gender, 
      homeAddress: Option[AddressInfo], 
      contacts: Option[Contacts], 
      createdBy: Option[String],
      changePasswordAtNextLogin: Boolean): StudentInfo

    def admStudent(
      id: String,
      org: Long,
      courseId: Long,
      batch: Long,
      admDate: java.time.LocalDate,
      createdBy: Option[String]
    )

    def closeAdm(
      id: String,
      reason: ClosureStatus,
      remarques: Option[String]
    )

    def updateStudent(id: String, spec: StudentSpec): Boolean
    def fetchStudents: List[StudentInfo]
    def fetchPurgedStudents: List[StudentInfo]
    def fetchStudent(id: java.util.UUID): Option[StudentInfo]
    def fetchStudentByCIN(cin: String): Option[StudentInfo]
    def searchStudents(q: String): Option[StudentInfo]
    def fetchOrgStudents(org: Long): List[AdmissionInfo]
    def fetchOrgStudentEvents(org: Long, id: String): List[EventInfo]
    def delStudent(id: String)                         
    def purgeStudent(id: String)                                                   
  }

  trait Exams {

    def saveExamCategory(name: String, startDate: java.time.LocalDate, endDate: java.time.LocalDate): ExamCategory
    def updateExamCategory(id: Long, spec: ExamCategorySpec): Boolean
    def delExamCategory(id: Long)

    def addExam(
      org: Long,
      name: String,
      subjectId: Long,
      batchId: Long,
      staffs: List[Long],
      `type`: Long,
      date: java.time.LocalDate,
      startTime: java.time.LocalTime,
      duration: java.time.Duration,
      createdBy: Option[String]): Exam

    def delExam(id: Long)

    def markStudent(
      id: String, 
      examId: Long, 
      empId: String,
      marks: Double,
      createdBy: Option[String]): MarkInfo     
  }

  trait Timetables {

    /*
     *  Creates a timetable entry, adds a timetable event entry if dayOfWeek is today
    */  
    def addOrUpdateScheduledEvents(
      batch: Long, events: List[ScheduledEventSpec]): TimetableEvent // new event

    /*
     *  Gets a timetable event entry; if it's not expired, creates or refreshes an associated timetable events for `date`
    */  
    def getScheduledEvent(
      date: java.time.LocalDate,
      org: Long,
      batch: Long,
      subject: Long,
      `type`: TimetableEventType,
      dayOfWeek: java.time.DayOfWeek,
      startTime: java.time.LocalTime,
      endTime: java.time.LocalTime,
      createdBy: Option[String]): Option[TimetableEvent]

    def getScheduledEvents(
      date: java.time.LocalDate,
      org: Long,
      batch: Long): List[TimetableEvent]    

    def getStudentScheduledEvents(
      id: String,
      date: java.time.LocalDate,
      org: Long,
      batch: Long): List[TimetableEvent]        

    def getEmployeeScheduledEvents(
      id: String,
      date: java.time.LocalDate,
      org: Long,
      batch: Long): List[TimetableEvent]    
  }

  trait Attendances {

    def markStudentPresence(
      id: String, 
      event: Long, 
      present: Boolean, 
      createdBy: Option[String])

    def markEmployeePresence(
      id: String, 
      event: Long, 
      present: Boolean, 
      createdBy: Option[String])    
  }
}

trait SchoolServicesRepoComponent {

  val schoolServiceRepo: SchoolServicesRepo

  trait SchoolServicesRepo {
    
  }
}

