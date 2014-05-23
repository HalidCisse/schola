package ma.epsilon.schola
package school

package impl

import _root_.ma.epsilon.schola.domain._, school.domain._

trait CoursesSchoolServicesComponentImpl extends CoursesSchoolServicesComponent {
  this: CoursesSchoolServicesRepoComponent =>

  trait CoursesServices extends Courses {

    def getCourses = coursesServicesRepo.getCourses
    
    def saveCourse(
      name: String, 
      levels: Option[Int],
      deptId: Option[Uuid], 
      code: Option[String], 
      desc: Option[String], 
      org: Option[Uuid]) = coursesServicesRepo.saveCourse(name, levels, deptId, code, desc, org)
    
    def getOrgCourses(org: Uuid) = coursesServicesRepo.getOrgCourses(org)
    def delOrgCourse(id: Uuid, org: Uuid) = coursesServicesRepo.delOrgCourse(id, org)
    def delCourse(id: Uuid) = coursesServicesRepo.delCourse(id)

    def getSubjects(org: Uuid) = coursesServicesRepo.getSubjects(org)
    def saveSubject(org: Uuid, name: String, desc: Option[String]) = coursesServicesRepo.saveSubject(org, name, desc)
    def getOrgSubjects(org: Uuid) = coursesServicesRepo.getOrgSubjects(org)
    def delSubject(id: Uuid) = coursesServicesRepo.delSubject(id)

    // -----------------------------------------------

    def getOrgModules(org: Uuid) = coursesServicesRepo.getOrgModules(org)

    def delCourseModule(
      id: Uuid) = coursesServicesRepo.delCourseModule(id)
    
    def delBatch(
      id: Uuid) = coursesServicesRepo.delBatch(id)

    def getCourseModules(compaign: Uuid) = coursesServicesRepo.getCourseModules(compaign)
    def getOrgBatches(compaign: Uuid) = coursesServicesRepo.getOrgBatches(compaign)

    def saveBatch(
      org: Uuid,
      compaignId: Uuid, 
      compositionId: Uuid, 
      courseId: Uuid, 
      module: String, 
      level: Int,
      coefficient: Option[Double],
      createdBy: Option[Uuid],
      subjects: List[(Uuid, Option[Uuid], Int, Option[Double])]) = 
    coursesServicesRepo.saveBatch(
      org,
      compaignId, 
      compositionId, 
      courseId, 
      module,
      level,
      coefficient,
      createdBy,
      subjects)
  }
}

trait CoursesSchoolServicesRepoComponentImpl extends CoursesSchoolServicesRepoComponent {
    this: jdbc.WithDatabase =>

  import school.schema._
  import jdbc.Q._

  protected val coursesServicesRepo = new CoursesServicesRepoImpl

  class CoursesServicesRepoImpl extends CoursesServicesRepo {

    private[this] object oq {

      val orgs = {

        def getOrg(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id)

        new {
          val id = Compiled(getOrg _)
        }
      }

      val courses = new {

        val all = Compiled { Courses map identity }

        val orgCourse = {
          def getOrgCourse(args: (Column[Uuid], Column[Uuid])) =
            OrgCourses filter (org => org.org === args._1 && org.courseId === args._2)

          Compiled(getOrgCourse _)
        }

        val orgCourseUpdate = {
          def getOrgCourse(args: (Column[Uuid], Column[Uuid])) =
            OrgCourses 
              .filter (org => org.org === args._1 && org.courseId === args._2)
              .map(o => (o.levels, o.deptId, o.desc))

          Compiled(getOrgCourse _)
        }

        val org = {
          def getOrgCourses(org: Column[Uuid]) =
            for {
              (course, orgCourse) <- Courses leftJoin OrgCourses on (_.id === _.courseId) if orgCourse.org === org
            } yield course

          Compiled{getOrgCourses _}
        }

        val id = {
          def getCourse(id: Column[Uuid]) =
            Courses filter (_.id === id) map (s => (s.name, s.code))
          
          Compiled{getCourse _}
        }

        val named = {
          def getCourse(name: Column[String]) =
            Courses filter (_.name.toLowerCase === name.toLowerCase)
          
          Compiled{getCourse _}
        }

        val code = {
          def getCourse(code: Column[Option[String]]) =
            Courses filter (_.code.toLowerCase === code)
          
          Compiled{getCourse _}
        }
      }

      val subjects = new {

        val org = {
          def getOrgSubjects(org: Column[Uuid]) =
            for {
              orgSubject <- OrgSubjects if orgSubject.org === org
            } yield orgSubject

          Compiled{getOrgSubjects _}
        }

        val id = {
          def getSubject(id: Column[Uuid]) =
            OrgSubjects filter (_.id === id)
          
          Compiled{getSubject _}
        }

        val named = {
          def getSubject(name: Column[String]) =
            OrgSubjects filter (_.name.toLowerCase === name.toLowerCase)
          
          Compiled{getSubject _}
        }

        val name = {
          def getSubject(id: Column[Uuid]) =
            OrgSubjects 
              .filter (_.id === id)
              .map(o => (o.name, o.desc))
          
          Compiled{getSubject _}
        }
      }

      val modules = new {

        val org = Compiled {
          (org: Column[Uuid]) => 
            Modules
              .filter(_.org === org)
        }

        val named = Compiled {
          (org: Column[Uuid], name: Column[String]) => 
            Modules
              .filter(module => module.org === org && module.name.toLowerCase === name.toLowerCase)
        }        
      }

      val courseModules = new {

        val compaign = Compiled {
          (compaignId: Column[Uuid]) => 
            OrgCoursesModules
              .filter(_.compaignId === compaignId)
        }

        val byId = Compiled{
          (id: Column[Uuid]) =>
            OrgCoursesModules
              .filter{
                courseModule => 
                  courseModule.id === id
              }
        }         

        val id = Compiled{
          (compaignId: Column[Uuid], compositionId: Column[Uuid], courseId: Column[Uuid], moduleId: Column[Uuid]) =>
            OrgCoursesModules
              .filter{
                courseModule => 
                  courseModule.compaignId === compaignId && 
                  courseModule.compositionId === compositionId && 
                    courseModule.courseId === courseId && 
                      courseModule.moduleId === moduleId
              }.map(_.id) 
        }

        val forUpdate = Compiled{
          (id: Column[Uuid]) =>
            OrgCoursesModules
              .filter{
                courseModule => 
                  courseModule.id === id
              }.map(o => (o.coefficient, o.level))
        }
      }

      val batches = new {

        val compaign = Compiled {
          (compaignId: Column[Uuid]) => 
            Batches
              .filter(_.compaignId === compaignId)
        }

        val byId = Compiled{
          (id: Column[Uuid]) =>
            Batches
              .filter{
                courseModule => 
                  courseModule.id === id
              }
        }        

        val id = Compiled{
          (compaignId: Column[Uuid], compositionId: Column[Uuid], courseId: Column[Uuid], moduleId: Column[Uuid], subjectId: Column[Uuid]) =>
            Batches
              .filter{
                moduleSubject => 
                  moduleSubject.compaignId === compaignId && 
                  moduleSubject.compositionId === compositionId && 
                    moduleSubject.courseId === courseId && 
                      moduleSubject.moduleId === moduleId &&
                        moduleSubject.subjectId === subjectId
              }.map(_.id) 
        }

        val forUpdate = Compiled{
          (id: Column[Uuid]) =>
            Batches
              .filter{
                moduleSubject => 
                  moduleSubject.id === id
              }.map(o => (o.coefficient, o.level))
        }
      }
    }

    def getCourses = db.withSession { implicit s => oq.courses.all.list }

    def saveCourse(
      name: String, 
      levels: Option[Int],
      deptId: Option[Uuid], 
      code: Option[String], 
      desc: Option[String], 
      org: Option[Uuid]) = {

      def named = 
        db.withSession {
          implicit s =>
            
            oq.courses
              .named(name)
              .firstOption
        }

      def codeInDB = 
        db.withSession {
          implicit s =>
            
            oq.courses
              .code(code map(_.toLowerCase))
              .firstOption
        }

      def doUpdate(id: Uuid) =
        db.withTransaction {
          implicit session =>
            
            oq.courses
              .id(id)
              .update(name, code) == 1
        }

      def doUpdateOrgCourse(oOrg: Uuid, oCourseId: Uuid) =
        db.withTransaction {
          implicit session =>
            
            oq.courses
              .orgCourseUpdate(oOrg, oCourseId)
              .update(levels.get /* TODO: add some error handling here. */, deptId, desc) == 1
        }

      def doInsert() = 
        db.withTransaction {
          implicit session => Courses insert (name, code)
        }

      org match {
        case Some(o) => // org exists in db

          def orgCourse(org: Uuid, id: Uuid) = db.withSession {
            implicit s => oq.courses.orgCourse(org, id).firstOption
          }

          val id = codeInDB match {
            case Some(Course(_, _, id))   => {val _id = id.get; doUpdate(_id); _id}
            case _ => named match {
              case Some(Course(_, _, id)) => {val _id = id.get; doUpdate(_id); _id}
              case _                      => doInsert()
            }
          }

          orgCourse(o, id) match {
            case None            => db.withTransaction { implicit s => OrgCourses insert OrgCourse(o, levels.get, deptId, desc, id) }
            case Some(OrgCourse(
                    oOrg, 
                    _, 
                    _, 
                    _, 
                    oCourseId))  => doUpdateOrgCourse(oOrg, oCourseId)
          }

          Course(name, code, id = Some(id))

        case _ =>

          val id = codeInDB match {
            case Some(Course(_, _, id))   => {val _id = id.get; doUpdate(_id); _id}
            case _ => named match {
              case Some(Course(_, _, id)) => {val _id = id.get; doUpdate(_id); _id}
              case _                      => doInsert()
            }
          }

          Course(name, code, id = Some(id))
      }
    }

    def getOrgCourses(org: Uuid) = db.withSession { implicit s => oq.courses.org(org).list }

    def delOrgCourse(id: Uuid, org: Uuid) =
      db.withTransaction { implicit s =>
        oq.courses.orgCourse(org, id).delete
      }

    def delCourse(id: Uuid) = db.withTransaction { implicit s => Courses delete id }

    def getSubjects(org: Uuid) =
      db.withSession { implicit s =>
        oq.subjects.org(org).list
      }    

    def saveSubject(org: Uuid, name: String, desc: Option[String]) = {
      
      def named = 
        db.withSession {
          implicit s =>
            oq.subjects
              .named(name)
              .firstOption
        }

      def doUpdate(id: Uuid) = 
        db.withTransaction {
          implicit session =>
            
            oq.subjects
              .name(id)
              .update(name, desc) == 1
        }

      def doInsert() = 
        db.withTransaction {
          implicit session =>
            OrgSubjects insert (org, name, desc)
        }

      named match {
        case Some(OrgSubject(_, _, _, id)) => doUpdate(id.get); OrgSubject(org, name, desc, id)
        case _                             => doInsert()
      }
    }

    def getOrgSubjects(org: Uuid) = db.withSession { implicit s => oq.subjects.org(org).list }

    def delSubject(id: Uuid) = db.withTransaction { implicit s => OrgSubjects delete id }

    // -----------------------------------------------

    def getOrgModules(org: Uuid) = 
      db.withSession{implicit s =>

        oq.modules
          .org(org)
          .list
      }

    def delCourseModule(
      id: Uuid) = 
      db.withTransaction {implicit s =>
        
        oq.courseModules
          .byId(id)
          .delete
      }
 
    def delBatch(
      id: Uuid) = 
      db.withTransaction {implicit s =>
        
        oq.batches
          .byId(id)
          .delete
      } 

    def getCourseModules(compaignId: Uuid) = 
      db.withSession{implicit s =>

        oq.courseModules
          .compaign(compaignId)
          .list
      }
    
    def getOrgBatches(compaignId: Uuid) =
      db.withSession{implicit s =>

        oq.batches
          .compaign(compaignId)
          .list
      }

    def saveBatch(
      org: Uuid,
      compaignId: Uuid, 
      compositionId: Uuid, 
      courseId: Uuid, 
      module: String, 
      level: Int,
      coefficient: Option[Double],
      createdBy: Option[Uuid],
      subjects: List[(Uuid, Option[Uuid], Int, Option[Double])]) {

      def doAddModuleSubjects(id: Uuid, moduleId: Uuid)(implicit s: Session) =
        subjects foreach {
          case (subjectId, empId, sLevel, sCoefficient) =>

            val subject = 
              oq.batches
                .forUpdate(id)

            subject.firstOption match {
              case Some(_) => subject update(sCoefficient, sLevel)
              case _       => Batches insert Batch(empId, compaignId, compositionId, courseId, moduleId, subjectId, sLevel, sCoefficient, createdBy = createdBy)
            }
        }

      val moduleId = db.withSession{implicit s => oq.modules.named(org, module).firstOption } match {
        case Some(Module(_, _, Some(id))) => id      
        case _                            => db.withTransaction{implicit s => Modules insert(org, module)}
      }

      db.withSession{implicit s => oq.courseModules.id(compaignId, compositionId, courseId, moduleId).firstOption } match {
        case Some(id) => 

          db.withTransaction {implicit s =>

            assert {          
              oq.courseModules
                .forUpdate(id)
                .update(coefficient, level) == 1
            }
            
            doAddModuleSubjects(id, moduleId)
          }

        case _ =>

        db.withTransaction{implicit s =>
          val id = OrgCoursesModules insert (compaignId, compositionId, courseId, moduleId, level, coefficient, createdBy = createdBy)
          doAddModuleSubjects(id, moduleId)
        }
      }
    }
  }
}