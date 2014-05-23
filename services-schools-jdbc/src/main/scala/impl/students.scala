package ma.epsilon.schola
package school

package impl

import _root_.ma.epsilon.schola.domain._, school.domain._

trait StudentServicesComponentImpl extends StudentServicesComponent {
  this: StudentServicesRepoComponent =>

  trait StudentsServices extends Students {

/*    def addStudent(
      org: Option[Uuid],
      cin: String,
      courseId: Option[Uuid],
      batch: Option[Uuid],
      admDate: Option[java.time.LocalDate],
      nationality: String,
      dateOB: java.time.LocalDate,
      username: String,
      givenName: String,
      familyName: String,
      gender: Gender,
      homeAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      createdBy: Option[Uuid]) = studentServiceRepo.addStudent(org, cin, courseId, batch, admDate, nationality, dateOB, username, givenName, familyName, gender, homeAddress, contacts, createdBy)

    def setGuardian(
      id: Uuid,
      cin: String,
      username: String,
      givenName: String,
      familyName: String,
      jobTitle: String,
      gender: Gender,
      homeAddress: Option[AddressInfo],
      workAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      relation: Option[GuardianRelation], 
      createdBy: Option[Uuid]) = studentServiceRepo.setGuardian(id, cin, username, givenName, familyName, jobTitle, gender, homeAddress, workAddress, contacts, relation, createdBy)

    def addInscription(admissionId: Uuid, compaignId: Uuid, createdBy: Option[Uuid]) = studentServiceRepo.addInscription(admissionId, compaignId, createdBy)

    def endStudentAdm(
      admissionId: Uuid,
      reason: ClosureStatus,
      remarques: Option[String]) = studentServiceRepo.endStudentAdm(admissionId, reason, remarques)

    def updateStudent(org: Uuid, studentId: Uuid, spec: StudentSpec) = studentServiceRepo.updateStudent(org, studentId, spec)
    def fetchStudents = studentServiceRepo.fetchStudents
    def fetchPurgedStudents = studentServiceRepo.fetchPurgedStudents
    def fetchStudentsByCIN(cin: String) = studentServiceRepo.fetchStudentsByCIN(cin)
    def fetchStudentByCIN(cin: String) = studentServiceRepo.fetchStudentByCIN(cin)
    def fetchAdmissions(org: Uuid) = studentServiceRepo.fetchAdmissions(org)
    def fetchInscriptions(org: Uuid) = studentServiceRepo.fetchInscriptions(org)
    def fetchOrgStudentInscriptions(studentId: Uuid) = studentServiceRepo.fetchOrgStudentInscriptions(studentId)
    def fetchGuardian(studentId: Uuid) = studentServiceRepo.fetchGuardian(studentId)

    def fetchOrgStudentEvents(
      compaignId: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDate]) = studentServiceRepo.fetchOrgStudentEvents(compaignId, startDate, endDate)

    def purgeStudent(studentId: Uuid) = studentServiceRepo.purgeStudent(studentId)*/
  }
}

trait StudentServicesRepoComponentImpl extends StudentServicesRepoComponent {
    /*this: jdbc.WithDatabase with UserServicesComponent =>

  import school.schema._
  import jdbc.Q._*/

  protected val studentServiceRepo = new StudentsServicesRepoImpl

  class StudentsServicesRepoImpl extends StudentsServicesRepo {

/*    private[this] object oq {

      val userId = {
        def getUserId(id: Column[Uuid]) =
          for {
            (student, user) <- Students innerJoin Users on (_.userId === _.id) if student.id === id
          } yield user.id

        Compiled(getUserId _)
      }

      val updates = new {

        val modified = {
          def getAudits(id: Column[Uuid]) =
            for {
              user <- Users if user.id === id
            } yield (
              user.lastModifiedAt,
              user.lastModifiedBy)

          Compiled(getAudits _)
        }

        val dateOB = {
          def getDateOB(id: Column[Uuid]) =
            Students
              .filter(_.id === id)
              .map(_.dateOB)

          Compiled(getDateOB _)
        }

        val nationality = {
          def getNationality(id: Column[Uuid]) =
            Students
              .filter(_.id === id)
              .map(_.nationality)

          Compiled(getNationality _)
        }
      }

      val inscriptions = new {
        
        val student = Compiled{
          (compaignId: Column[Uuid], studentId: Column[Uuid]) =>
            for {
              (inscription, admission) <- Inscriptions leftJoin Admissions on (_.admissionId === _.id) if admission.studentId === studentId && inscription.compaignId === compaignId
            } yield (
              admission.id,
              admission.compaignId,
              inscription.createdAt,
              inscription.createdBy)
        }

        val compaign = Compiled{
          (compaignId: Column[Uuid]) =>
            for {
              (inscription, admission) <- Inscriptions leftJoin Admissions on (_.admissionId === _.id) if admission.compaignId === compaignId
            } yield (
              admission.id,
              admission.compaignId,
              inscription.createdAt,
              inscription.createdBy)
        }        
      }

      val admissions = {

        def forEnd(id: Column[Uuid]) =
          Admissions
            .filter(admission => admission.endStatus.isEmpty && admission.id === id)
            .map(admission => (admission.endStatus, admission.endRemarques, admission.endDate))

        def orgActiveAdmissions(org: Column[Uuid]) =
          Admissions 
            .filter(admission => admission.org === org && admission.endStatus.isEmpty)
            .leftJoin(Students).on(_.studentId === _.id)
            .innerJoin(Users filter(user => !user._deleted)).on(_._2.userId === _.id)
            .map{
              case ((admission, _), _) =>
                (
                admission.id,
                admission.org,
                admission.courseId,
                admission.studentId,
                admission.status,
                admission.endStatus,
                admission.endRemarques,
                admission.admDate,
                admission.endDate,
                admission.createdAt,
                admission.createdBy)              
            }
          
        new {
          val end = Compiled { forEnd _ }
          val orgActive = Compiled { orgActiveAdmissions _ }
        }
      }

      val students = new {

        val all = Compiled {
          Students
            .innerJoin(
              Users
                .filter(user => !user._deleted)).on(_.userId === _.id)
            .map {
              case (student, user) =>
                (
                  student.id,
                  student.regNo,
                  student.dateOB,
                  student.nationality,
                  user.id,
                  user.cin,
                  user.primaryEmail,
                  user.givenName,
                  user.familyName,
                  user.jobTitle,
                  user.createdAt,
                  user.createdBy,
                  user.lastLoginTime,
                  user.lastModifiedAt,
                  user.lastModifiedBy,
                  user.stars,
                  user.gender,
                  user.homeAddress,
                  user.contacts,
                  user.suspended)
            }
        }

        val trash = Compiled {
          Students
            .innerJoin(
              Users
                .filter(user => user._deleted)).on(_.userId === _.id)
            .map {
              case (student, user) =>
                (
                  student.id,
                  student.regNo,
                  student.dateOB,
                  student.nationality,
                  user.id,
                  user.cin,
                  user.primaryEmail,
                  user.givenName,
                  user.familyName,
                  user.jobTitle,
                  user.createdAt,
                  user.createdBy,
                  user.lastLoginTime,
                  user.lastModifiedAt,
                  user.lastModifiedBy,
                  user.stars,
                  user.gender,
                  user.homeAddress,
                  user.contacts,
                  user.suspended)
            }
        }

        val id = {
          def getStudent(id: Column[Uuid]) =
            Students
              .filter(student => student.id === id)
              .innerJoin(
                Users
                  .filter(user => !user._deleted)).on(_.userId === _.id)
              .map {
                case (student, user) =>
                  (
                    student.id,
                    student.regNo,
                    student.dateOB,
                    student.nationality,
                    user.id,
                    user.cin,
                    user.primaryEmail,
                    user.givenName,
                    user.familyName,
                    user.jobTitle,
                    user.createdAt,
                    user.createdBy,
                    user.lastLoginTime,
                    user.lastModifiedAt,
                    user.lastModifiedBy,
                    user.stars,
                    user.gender,
                    user.homeAddress,
                    user.contacts,
                    user.suspended)
              }

          Compiled(getStudent _)
        }

        val byCIN = {
          def getStudent(cin: Column[String]) =
            Students
              .innerJoin(
                Users
                  .filter(user => !user._deleted && user.cin === cin)).on(_.userId === _.id)
              .map {
                case (student, user) =>
                  (
                    student.id,
                    student.regNo,
                    student.dateOB,
                    student.nationality,
                    user.id,
                    user.cin,
                    user.primaryEmail,
                    user.givenName,
                    user.familyName,
                    user.jobTitle,
                    user.createdAt,
                    user.createdBy,
                    user.lastLoginTime,
                    user.lastModifiedAt,
                    user.lastModifiedBy,
                    user.stars,
                    user.gender,
                    user.homeAddress,
                    user.contacts,
                    user.suspended)
              }

          Compiled(getStudent _)
        }

        val likeCIN = {
          def getStudents(cin: Column[String]) =
            Students
              .innerJoin(
                Users
                  .filter(user => !user._deleted && (user.cin like cin))).on(_.userId === _.id)
              .map {
                case (student, user) =>
                  (
                    student.id,
                    student.regNo,
                    student.dateOB,
                    student.nationality,
                    user.id,
                    user.cin,
                    user.primaryEmail,
                    user.givenName,
                    user.familyName,
                    user.jobTitle,
                    user.createdAt,
                    user.createdBy,
                    user.lastLoginTime,
                    user.lastModifiedAt,
                    user.lastModifiedBy,
                    user.stars,
                    user.gender,
                    user.homeAddress,
                    user.contacts,
                    user.suspended)
              }

          Compiled(getStudents _)
        }

        val guardian = {
          def getGuardian(id: Column[Uuid]) =
            Guardians
              .filter(guardian => guardian.userId === id)
              .innerJoin(
                Users
                  .filter(user => !user._deleted)).on(_.userId === _.id)
              .map {
                case (guardian, user) =>
                  (
                    guardian.id,
                    user.cin,
                    user.id,
                    user.primaryEmail,
                    user.givenName,
                    user.familyName,
                    user.jobTitle,
                    user.createdAt,
                    user.createdBy,
                    user.lastLoginTime,
                    user.lastModifiedAt,
                    user.lastModifiedBy,
                    user.stars,
                    user.gender,
                    user.homeAddress,
                    user.workAddress,
                    user.contacts,
                    user.suspended,
                    guardian.relation)
              }

          Compiled(getGuardian _)
        }
      }
    }

    def addStudent(
      org: Option[Uuid],
      sCIN: String,
      courseId: Option[Uuid],
      batch: Option[Uuid],
      admDate: Option[java.time.LocalDate],
      nationality: String,
      dateOB: java.time.LocalDate,
      sPrimaryEmail: String,
      sGivenName: String,
      sFamilyName: String,
      sGender: Gender,
      sHomeAddress: Option[AddressInfo],
      sContacts: Option[Contacts],
      sCreatedBy: Option[Uuid]) = {

      // add student user if not exists
      // update existing user
      // add admission if course and org given
      // add to batch(inscribe) if given

      def accessRightsByAlias(aliases: String*)(implicit session: jdbc.Q.Session) = 
        AccessRights
          .filter(_.alias inSet Set(aliases:_*))
          .map(_.id)
          .list      

      def UpsertUserAndGet(id: Option[Uuid])(implicit session: jdbc.Q.Session) = id match {
        case Some(oId) =>

          userService.updateUser(oId, new DefaultUserSpec {
            override lazy val contacts =
              UpdateSpecImpl[ContactsSpec](
                set = sContacts collect {
                  case Contacts(mobiles, home, work, site) =>
                    Some(ContactsSpec(

                      UpdateSpecImpl[MobileNumbersSpec](
                        set = mobiles collect {
                          case MobileNumbers(mobile1, mobile2) =>
                            Some(MobileNumbersSpec(
                              UpdateSpecImpl[String](set = mobile1 map Option[String]),
                              UpdateSpecImpl[String](set = mobile2 map Option[String])))
                        }),

                      UpdateSpecImpl[ContactInfoSpec](
                        set = home collect {
                          case ContactInfo(email, phoneNumber, fax) => Some(ContactInfoSpec(
                            UpdateSpecImpl[String](set = email map Option[String]),
                            UpdateSpecImpl[String](set = fax map Option[String]),
                            UpdateSpecImpl[String](set = phoneNumber map Option[String])))
                        }),

                      UpdateSpecImpl[ContactInfoSpec](
                        set = work collect {
                          case ContactInfo(email, phoneNumber, fax) => Some(ContactInfoSpec(
                            UpdateSpecImpl[String](set = email map Option[String]),
                            UpdateSpecImpl[String](set = fax map Option[String]),
                            UpdateSpecImpl[String](set = phoneNumber map Option[String])))
                        }),

                      site = UpdateSpecImpl[String](set = site map Option[String])
                      ))
                })

            override lazy val homeAddress =
              UpdateSpecImpl[AddressInfoSpec](
                set = sHomeAddress collect {
                  case AddressInfo(city, country, postalCode, streetAddress) =>
                    Some(AddressInfoSpec(
                      city = UpdateSpecImpl[String](set = city map Option[String]),
                      country = UpdateSpecImpl[String](set = country map Option[String]),
                      postalCode = UpdateSpecImpl[String](set = postalCode map Option[String]),
                      streetAddress = UpdateSpecImpl[String](set = streetAddress map Option[String])))
                })

            override lazy val primaryEmail = Some(sPrimaryEmail)

            override lazy val cin = Some(sCIN)

            override lazy val givenName = Some(sGivenName)

            override lazy val familyName = Some(sFamilyName)

            override lazy val gender = Some(sGender)

            override lazy val updatedBy = sCreatedBy

            // override lazy val accessRights = Some(Set(sAccessRights: _*)) // TODO: add student access rights here
            override lazy val accessRights = SetSpec(SetSpec.Add(Set(accessRightsByAlias(Schools.accessRights.STUDENT):_*)))
          })

          userService.getUser(oId) match {
            case Some(user) => user
            case _          => throw new RuntimeException("addStudent.UserNotFound")
          }

        case _ =>

          userService.saveUser(
            sCIN, sPrimaryEmail, sGivenName, sFamilyName, "Student" /* TODO: translate */ , sCreatedBy, sGender, sHomeAddress, None, sContacts, suspended = false, changePasswordAtNextLogin = true, accessRightsByAlias(Schools.accessRights.STUDENT))
      }

      db.withTransaction { implicit session =>

        val user =
          userService.getUserByCIN(sCIN) match {
            case Some(fUser) => UpsertUserAndGet(fUser.id)
            case _          => UpsertUserAndGet(None)
          }

        val student = Students insert (
          s"STUDENT-${user.id.get}" /* TODO: use an student number generation system  */ ,
          user.id.get,
          nationality,
          dateOB,
          None // guardianId
          )

        (org, courseId) match {
          case (Some(o), Some(c)) =>

            val admission = Admissions insert (
              org = o,
              studentId = student.id.get,
              courseId = c,
              createdBy = sCreatedBy,
              admDate = admDate getOrElse dateNow)

            batch match {
              case Some(b) => Inscriptions insert (admission.id.get, b, sCreatedBy)
              case _       => {}
            }

            StudentInfo(
              student.id.get,
              student.regNo,
              student.dateOB,
              student.nationality,
              student.userId,
              user.cin,
              user.primaryEmail,
              user.givenName,
              user.familyName,
              user.jobTitle,
              user.createdAt,
              user.createdBy,
              user.lastLoginTime,
              user.lastModifiedAt,
              user.lastModifiedBy,
              user.stars,
              user.gender,
              user.homeAddress,
              user.contacts,
              user.suspended)

          case _ =>

            StudentInfo(
              student.id.get,
              student.regNo,
              student.dateOB,
              student.nationality,
              student.userId,
              user.cin,
              user.primaryEmail,
              user.givenName,
              user.familyName,
              user.jobTitle,
              user.createdAt,
              user.createdBy,
              user.lastLoginTime,
              user.lastModifiedAt,
              user.lastModifiedBy,
              user.stars,
              user.gender,
              user.homeAddress,
              user.contacts,
              user.suspended)
        }
      }
    }

    def endStudentAdm(
      admissionId: Uuid,
      reason: ClosureStatus,
      remarques: Option[String]) = db.withTransaction { implicit s =>

      oq.admissions
        .end(admissionId)
        .update(Some(reason), remarques, Some(now)) == 1
    }

    def updateStudent(org: Uuid, studentId: Uuid, spec: StudentSpec) =
      db.withTransaction {
        implicit session =>

          oq.userId(studentId).firstOption match {
            case Some(userId) =>

              val currentTimestamp = Some(now: java.time.LocalDateTime)

              val _1 = userService.updateUser(userId, spec)

              val _2 = _1 && (spec.nationality map {
                nationality =>

                  val _1 = (oq.updates
                    .nationality(studentId)
                    .update(nationality) == 1)

                  _1 && (oq.updates
                    .modified(userId)
                    .update(currentTimestamp, spec.updatedBy) == 1)

              } getOrElse true)

              _2 && (spec.dateOB map {
                dateOB =>

                  val _1 = (oq.updates
                    .dateOB(studentId)
                    .update(dateOB) == 1)

                  _1 && (oq.updates
                    .modified(userId)
                    .update(currentTimestamp, spec.updatedBy) == 1)

              } getOrElse true)

            case _ => false
          }
      }

    def setGuardian(
      id: Uuid,
      sCIN: String,
      sPrimaryEmail: String,
      sGivenName: String,
      sFamilyName: String,
      sJobTitle: String,
      sGender: Gender,
      sHomeAddress: Option[AddressInfo],
      sWorkAddress: Option[AddressInfo],
      sContacts: Option[Contacts],
      relation: Option[GuardianRelation], 
      sCreatedBy: Option[Uuid]) = {
      
      def UpsertUserAndGet(id: Option[Uuid])(implicit session: jdbc.Q.Session) = id match {
        case Some(oId) =>

          userService.updateUser(oId, new DefaultUserSpec {
            override lazy val contacts =
              UpdateSpecImpl[ContactsSpec](
                set = sContacts collect {
                  case Contacts(mobiles, home, work, site) =>
                    Some(ContactsSpec(

                      UpdateSpecImpl[MobileNumbersSpec](
                        set = mobiles collect {
                          case MobileNumbers(mobile1, mobile2) =>
                            Some(MobileNumbersSpec(
                              UpdateSpecImpl[String](set = mobile1 map Option[String]),
                              UpdateSpecImpl[String](set = mobile2 map Option[String])))
                        }),

                      UpdateSpecImpl[ContactInfoSpec](
                        set = home collect {
                          case ContactInfo(email, phoneNumber, fax) => Some(ContactInfoSpec(
                            UpdateSpecImpl[String](set = email map Option[String]),
                            UpdateSpecImpl[String](set = fax map Option[String]),
                            UpdateSpecImpl[String](set = phoneNumber map Option[String])))
                        }),

                      UpdateSpecImpl[ContactInfoSpec](
                        set = work collect {
                          case ContactInfo(email, phoneNumber, fax) => Some(ContactInfoSpec(
                            UpdateSpecImpl[String](set = email map Option[String]),
                            UpdateSpecImpl[String](set = fax map Option[String]),
                            UpdateSpecImpl[String](set = phoneNumber map Option[String])))
                        }),

                      site = UpdateSpecImpl[String](set = site map Option[String])
                      ))
                })

            override lazy val homeAddress =
              UpdateSpecImpl[AddressInfoSpec](
                set = sHomeAddress collect {
                  case AddressInfo(city, country, postalCode, streetAddress) =>
                    Some(AddressInfoSpec(
                      city = UpdateSpecImpl[String](set = city map Option[String]),
                      country = UpdateSpecImpl[String](set = country map Option[String]),
                      postalCode = UpdateSpecImpl[String](set = postalCode map Option[String]),
                      streetAddress = UpdateSpecImpl[String](set = streetAddress map Option[String])))
                })

            override lazy val workAddress =
              UpdateSpecImpl[AddressInfoSpec](
                set = sWorkAddress collect {
                  case AddressInfo(city, country, postalCode, streetAddress) =>
                    Some(AddressInfoSpec(
                      city = UpdateSpecImpl[String](set = city map Option[String]),
                      country = UpdateSpecImpl[String](set = country map Option[String]),
                      postalCode = UpdateSpecImpl[String](set = postalCode map Option[String]),
                      streetAddress = UpdateSpecImpl[String](set = streetAddress map Option[String])))
                })              

            override lazy val primaryEmail = Some(sPrimaryEmail)

            override lazy val cin = Some(sCIN)

            override lazy val givenName = Some(sGivenName)

            override lazy val familyName = Some(sFamilyName)

            override lazy val gender = Some(sGender)

            override lazy val updatedBy = sCreatedBy

            // override lazy val accessRights = Some(Set(sAccessRights: _*)) // TODO: add guardian access rights here
          })

          userService.getUser(oId) match {
            case Some(user) => user
            case _          => throw new RuntimeException("setGuardian.UserNotFound")
          }

        case _ =>

          userService.saveUser(
            sCIN, sPrimaryEmail, sGivenName, sFamilyName, sJobTitle, sCreatedBy, sGender, sHomeAddress, sWorkAddress, sContacts, suspended = false, changePasswordAtNextLogin = true, List( /* TODO: add guardian access right */ ))
      }

      db.withTransaction { implicit session =>

        val user =
          userService.getUserByCIN(sCIN) match {
            case Some(fUser) => UpsertUserAndGet(fUser.id)
            case _           => UpsertUserAndGet(None)
          }

        val guardian = Guardians insert (relation getOrElse GuardianRelation.Other, user.id.get)

        GuardianInfo(
          guardian.id.get,
          user.cin,
          guardian.userId,
          user.primaryEmail,
          user.givenName,
          user.familyName,
          user.jobTitle,
          guardian.relation,
          user.createdAt,
          user.createdBy,
          user.lastLoginTime,
          user.lastModifiedAt,
          user.lastModifiedBy,
          user.stars,
          user.gender,
          user.homeAddress,
          user.workAddress,
          user.contacts,
          user.suspended)
      }
    }

    def addInscription(admissionId: Uuid, studentId: Uuid, createdBy: Option[Uuid]) = 
      db.withTransaction {implicit s =>
        (Inscriptions insert(admissionId, studentId, createdBy)) > 0
      }

    def fetchStudents =
      db.withSession { implicit s =>
        oq.students
          .all
          .list map {
            case (
              sId,
              regNo,
              dateOB,
              nationality,
              userId,
              sCin,
              primaryEmail,
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
              contacts,
              suspended
              ) =>
              StudentInfo(
                sId,
                regNo,
                dateOB,
                nationality,
                userId,
                sCin,
                primaryEmail,
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
                contacts,
                suspended)
          }
      }

    def fetchPurgedStudents =
      db.withSession { implicit s =>
        oq.students
          .trash
          .list map {
            case (
              sId,
              regNo,
              dateOB,
              nationality,
              userId,
              sCin,
              primaryEmail,
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
              contacts,
              suspended
              ) =>
              StudentInfo(
                sId,
                regNo,
                dateOB,
                nationality,
                userId,
                sCin,
                primaryEmail,
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
                contacts,
                suspended)
          }
      }

    def fetchStudentsByCIN(cin: String) =
      db.withSession { implicit s =>
        oq.students
          .likeCIN(cin)
          .list map {
            case (
              sId,
              regNo,
              dateOB,
              nationality,
              userId,
              sCin,
              primaryEmail,
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
              contacts,
              suspended
              ) =>
              StudentInfo(
                sId,
                regNo,
                dateOB,
                nationality,
                userId,
                sCin,
                primaryEmail,
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
                contacts,
                suspended)
          }
      }

    def fetchStudentByCIN(cin: String) =
      db.withSession { implicit s =>
        oq.students
          .byCIN(cin)
          .firstOption map {
            case (
              sId,
              regNo,
              dateOB,
              nationality,
              userId,
              sCin,
              primaryEmail,
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
              contacts,
              suspended
              ) =>
              StudentInfo(
                sId,
                regNo,
                dateOB,
                nationality,
                userId,
                sCin,
                primaryEmail,
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
                contacts,
                suspended)
          }
      }

    def fetchAdmissions(org: Uuid) =
      db.withSession { implicit s =>
        oq.admissions
          .org(org)
          .list map {
            case (sId,
              sOrg,
              course,
              student,
              status,
              endStatus,
              endRemarques,
              admDate,
              endDate,
              createdAt,
              createdBy) =>
              AdmissionInfo(
                sId,
                sOrg,
                course,
                student,
                status,
                endStatus,
                endRemarques,
                admDate,
                endDate,
                createdAt,
                createdBy)
          }
      }

    def fetchInscriptions(org: Uuid) =
      db.withSession { implicit s =>
        oq.inscriptions
          .org(org)
          .list map {
            case (compaignId,
              admissionId,
              createdAt,
              createdBy) =>
              InscriptionInfo(
                compaignId,
                admissionId,
                createdAt,
                createdBy)
          }
      }

    def fetchOrgStudentInscriptions(studentId: Uuid) = 
      db.withSession { implicit s =>
        oq.inscriptions
          .student(studentId)
          .list map {
            case (compaignId,
              admissionId,
              createdAt,
              createdBy) =>
              InscriptionInfo(
                compaignId,
                admissionId,
                createdAt,
                createdBy)
          }
      }    

    def fetchGuardian(id: Uuid) =
      db.withSession { implicit s =>
        oq.students
          .guardian(id)
          .firstOption map {
            case (sId,
              cin,
              userId,
              primaryEmail,
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
              suspended,
              relation) =>
              GuardianInfo(
                sId,
                cin,
                userId,
                primaryEmail,
                givenName,
                familyName,
                jobTitle,
                relation,
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
                suspended)
          }
      }

    def fetchOrgStudentEvents(
      admissionId: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDate]) = ???

    def purgeStudent(studentId: Uuid) =
      db.withTransaction { implicit s =>

        oq.userId(studentId).firstOption match {
          case Some(oId) =>
            userService.purgeUsers(Set(oId))
          case _ => {}
        }

        Admissions deleteForStudent (studentId)
        Students delete (studentId)
      }*/
  }
}