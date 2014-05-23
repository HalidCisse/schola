package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait CoursesSchoolServicesComponent {

  trait Courses {

    def getCourses: List[Course]
    
    def saveCourse(
      name: String,
      levels: Option[Int],
      deptId: Option[Uuid],
      code: Option[String], 
      desc: Option[String], 
      org: Option[Uuid]): Course
    
    def getOrgCourses(org: Uuid): List[Course]
    def delOrgCourse(id: Uuid, org: Uuid)
    def delCourse(id: Uuid)

    def getSubjects(org: Uuid): List[OrgSubject]
    def saveSubject(org: Uuid, name: String, desc: Option[String]): OrgSubject
    def getOrgSubjects(org: Uuid): List[OrgSubject]
    def delSubject(id: Uuid)

    // -----------------------------------------------

    def getOrgModules(org: Uuid): List[Module]

    def delCourseModule(
      id: Uuid)
    
    def delBatch(
      id: Uuid)

    def getCourseModules(compaign: Uuid): List[OrgCourseModule]
    
    def getOrgBatches(compaign: Uuid): List[Batch]

    def saveBatch(
      org: Uuid,
      compaignId: Uuid, 
      compositionId: Uuid, 
      courseId: Uuid, 
      module: String, 
      level: Int,
      coefficient: Option[Double],
      createdBy: Option[Uuid],
      subjects: List[(Uuid, Option[Uuid], Int, Option[Double])])
  }
}

trait CoursesSchoolServicesRepoComponent {

  protected val coursesServicesRepo: CoursesServicesRepo

  trait CoursesServicesRepo {

    def getCourses: List[Course]
    
    def saveCourse(
      name: String, 
      levels: Option[Int],
      deptId: Option[Uuid], 
      code: Option[String], 
      desc: Option[String], 
      org: Option[Uuid]): Course
    
    def getOrgCourses(org: Uuid): List[Course]
    def delOrgCourse(id: Uuid, org: Uuid)
    def delCourse(id: Uuid)

    def getSubjects(org: Uuid): List[OrgSubject]
    def saveSubject(org: Uuid, name: String, desc: Option[String]): OrgSubject
    def getOrgSubjects(org: Uuid): List[OrgSubject]
    def delSubject(id: Uuid)

    // -----------------------------------------------

    def getOrgModules(org: Uuid): List[Module]

    def delCourseModule(
      id: Uuid)
    
    def delBatch(
      id: Uuid)

    def getCourseModules(compaign: Uuid): List[OrgCourseModule]
    
    def getOrgBatches(compaign: Uuid): List[Batch]

    def saveBatch(
      org: Uuid,
      compaignId: Uuid, 
      compositionId: Uuid, 
      courseId: Uuid, 
      module: String, 
      level: Int,
      coefficient: Option[Double],
      createdBy: Option[Uuid],
      subjects: List[(Uuid, Option[Uuid], Int, Option[Double])])  
  }
}
