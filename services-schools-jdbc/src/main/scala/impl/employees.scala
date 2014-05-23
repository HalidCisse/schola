package ma.epsilon.schola
package school

package impl

import _root_.ma.epsilon.schola.domain._, school.domain._

trait EmployeeServicesComponentImpl extends EmployeeServicesComponent {
  this: EmployeeServicesRepoComponent =>

  trait EmployeesServices extends Employees {

    /*    def saveEmployee(
      org: Option[Uuid],
      cin: String,
      username: String,
      givenName: String,
      familyName: String,
      jobTitle: String,
      deptId: Option[Uuid],
      gender: Gender,
      homeAddress: Option[AddressInfo],
      workAddress: Option[AddressInfo],
      contacts: Option[Contacts],
      suspended: Boolean,
      joinDate: Option[java.time.LocalDate],
      createdBy: Option[Uuid],
      accessRights: List[Uuid]) = employeeServiceRepo.saveEmployee(org, cin, username, givenName, familyName, jobTitle, deptId, gender, homeAddress, workAddress, contacts, suspended, joinDate, createdBy, accessRights)

    def updateEmployee(org: Uuid, id: Uuid, spec: EmployeeSpec) = employeeServiceRepo.updateEmployee(org, id, spec)
    def fetchEmployees = employeeServiceRepo.fetchEmployees
    def fetchTrashedEmployees = employeeServiceRepo.fetchTrashedEmployees
    def fetchEmployee(id: Uuid) = employeeServiceRepo.fetchEmployee(id)
    def searchEmployeesByCIN(cin: String) = employeeServiceRepo.searchEmployeesByCIN(cin)
    def fetchEmployeeByCIN(cin: String) = employeeServiceRepo.fetchEmployeeByCIN(cin)
    def fetchEmployements(id: Uuid) = employeeServiceRepo.fetchEmployements(id)

    def endEmployment(org: Uuid, id: Uuid, reason: ClosureStatus, remarques: Option[String]) = employeeServiceRepo.endEmployment(org, id, reason, remarques)

    def fetchOrgEmploymentEvents(
      id: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDate]) = employeeServiceRepo.fetchOrgEmploymentEvents(id, startDate, endDate)

    def purgeEmployee(id: Uuid) = employeeServiceRepo.purgeEmployee(id)*/
  }
}

trait EmployeeServicesRepoComponentImpl extends EmployeeServicesRepoComponent {
  /*  this: jdbc.WithDatabase with UserServicesComponent =>

  import school.schema._
  import jdbc.Q._*/

  protected val employeeServiceRepo = new EmployeesServicesRepoImpl

  class EmployeesServicesRepoImpl extends EmployeesServicesRepo {

    /*    private[this] object oq {

      val employees = new {

        val activeBatch = {
          def getBatchId(args: (Column[Uuid] /*org*/ , Column[Uuid] /*empId*/ )) =
            Batches
              .filter(batch => batch.endDate > dateNow && batch.org === args._1 && batch.empId === args._2)

          Compiled(getBatchId _)
        }

        val org = {
          def getOrgEmployment(args: (Column[Uuid] /*org*/ , Column[Uuid] /*empId*/ )) =
            OrgEmployments
              .filter(orgEmployee => orgEmployee.org === args._1 && orgEmployee.empId === args._2)
              .map {
                orgEmployee =>
                  (orgEmployee.endDate, orgEmployee.endStatus, orgEmployee.endRemarques)
              }

          Compiled(getOrgEmployment _)
        }

        val all = Compiled {
          for {
            ((employee, orgEmployee), user) <- Employees leftJoin OrgEmployments on (_.id === _.empId) innerJoin Users on (_._1.userId === _.id) if !user._deleted
          } yield (
            employee.id,
            employee.empNo,
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
            user.workAddress,
            user.contacts,
            user.suspended,
            orgEmployee.org?,
            orgEmployee.deptId)
        }

        val trash = Compiled {
          for {
            ((employee, orgEmployee), user) <- Employees leftJoin OrgEmployments on (_.id === _.empId) innerJoin Users on (_._1.userId === _.id) if user._deleted
          } yield (
            employee.id,
            employee.empNo,
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
            user.workAddress,
            user.contacts,
            user.suspended,
            orgEmployee.org?,
            orgEmployee.deptId)
        }

        val id = {
          def getEmployee(id: Column[Uuid]) =
            for {
              ((employee, orgEmployee), user) <- Employees leftJoin OrgEmployments on (_.id === _.empId) innerJoin Users on (_._1.userId === _.id) if !user._deleted && employee.id === id
            } yield (
              employee.id,
              employee.empNo,
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
              user.workAddress,
              user.contacts,
              user.suspended,
              orgEmployee.org?,
              orgEmployee.deptId)

          Compiled(getEmployee _)
        }

        val byCIN = {
          def getEmployee(cin: Column[String]) =
            for {
              ((employee, orgEmployee), user) <- Employees leftJoin OrgEmployments on (_.id === _.empId) innerJoin Users on (_._1.userId === _.id) if !user._deleted && user.cin === cin
            } yield (
              employee.id,
              employee.empNo,
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
              user.workAddress,
              user.contacts,
              user.suspended,
              orgEmployee.org?,
              orgEmployee.deptId)

          Compiled(getEmployee _)
        }

        val likeCIN = {
          def getEmployees(cin: Column[String]) =
            for {
              ((employee, orgEmployee), user) <- Employees leftJoin OrgEmployments on (_.id === _.empId) innerJoin Users on (_._1.userId === _.id) if !user._deleted && (user.cin like cin)
            } yield (
              employee.id,
              employee.empNo,
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
              user.workAddress,
              user.contacts,
              user.suspended,
              orgEmployee.org?,
              orgEmployee.deptId)

          Compiled(getEmployees _)
        }

        val work = {

          def getEmployeeFinishedBatches(id: Column[Uuid]) =
            Batches
              .filter(batch => batch.endDate < dateNow && batch.empId === id)
              .map {
                batch =>
                  (batch.id,
                    batch.org,
                    batch.name,
                    batch.empId,
                    batch.courseId,
                    batch.subjectId,
                    None: Option[Uuid],
                    batch.startDate,
                    None: Option[java.time.LocalDateTime],
                    batch.createdAt,
                    batch.createdBy)
              }

          def getEmployeeActiveBatch(id: Column[Uuid]) =
            Batches
              .filter(batch => batch.endDate > dateNow && batch.empId === id)
              .map {
                batch =>
                  (batch.id,
                    batch.org,
                    batch.name,
                    batch.empId,
                    batch.courseId,
                    batch.subjectId,
                    None: Option[Uuid],
                    batch.startDate,
                    None: Option[java.time.LocalDateTime],
                    batch.createdAt,
                    batch.createdBy)
              }

          def getEmployeeTeachingHistory(id: Column[Uuid]) =
            TeachingHistories
              .filter(_.empId === id)
              .innerJoin(Batches).on(_.batchId === _.id)
              .map {
                case (teachingHistory, batch) =>
                  (
                    batch.id,
                    batch.org,
                    batch.name,
                    teachingHistory.empId?,
                    batch.courseId,
                    batch.subjectId,
                    teachingHistory.id?,
                    teachingHistory.startDate,
                    teachingHistory.endDate?,
                    teachingHistory.createdAt,
                    teachingHistory.createdBy)
              }

          Compiled {
            (id: Column[Uuid]) =>

              (getEmployeeFinishedBatches(id) ++
                getEmployeeActiveBatch(id) ++
                getEmployeeTeachingHistory(id)).sortBy(_._1.desc)
          }
        }
      }

      val userId = {
        def getUserId(id: Column[Uuid]) =
          for {
            (employee, user) <- Employees innerJoin Users on (_.userId === _.id) if employee.id === id
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

        /*val jobTitle = {
          def getJobTitle(id: Column[Uuid]) = 
            Employees filter(_.id === id) map(_.jobTitle)

          Compiled(getJobTitle _)
        }*/

        val dept = {
          def getDeptId(args: (Column[Uuid] /*org*/ , Column[Uuid] /*empId*/ )) =
            OrgEmployments filter (
              orgEmployee =>
                orgEmployee.org === args._1 &&
                  orgEmployee.empId === args._2) map (_.deptId)

          Compiled(getDeptId _)
        }
      }
    }

    def saveEmployee(
      org: Option[Uuid],
      sCIN: String,
      sPrimaryEmail: String,
      sGivenName: String,
      sFamilyName: String,
      sJobTitle: String,
      deptId: Option[Uuid],
      sGender: Gender,
      sHomeAddress: Option[AddressInfo],
      sWorkAddress: Option[AddressInfo],
      sContacts: Option[Contacts],
      sSuspended: Boolean,
      joinDate: Option[java.time.LocalDate],
      sCreatedBy: Option[Uuid],
      sAccessRights: List[Uuid]) = {

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

                      site = UpdateSpecImpl[String](set = site map Option[String])))
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

            override lazy val jobTitle = Some(sJobTitle)

            override lazy val gender = Some(sGender)

            override lazy val suspended = Some(sSuspended)

            override lazy val updatedBy = sCreatedBy

            override lazy val accessRights = SetSpec(SetSpec.Add(Set(sAccessRights: _*)))
          })

          userService.getUser(oId) match {
            case Some(user) => user
            case _          => throw new RuntimeException("saveEmployee.UserNotFound")
          }

        case _ =>

          userService.saveUser(
            sCIN, sPrimaryEmail, sGivenName, sFamilyName, sJobTitle, sCreatedBy, sGender, sHomeAddress, sWorkAddress, sContacts, sSuspended, changePasswordAtNextLogin = true, sAccessRights)
      }

      db.withTransaction { implicit session =>

        val user =
          userService.getUserByCIN(sCIN) match {
            case Some(user) => UpsertUserAndGet(user.id)
            case _          => UpsertUserAndGet(None)
          }

        val employee = Employees insert (s"EMPLOYEE-${user.id.get}" /* TODO: use an employee number generation system  */ , user.id.get)

        org match {
          case Some(o) =>

            val orgEmployee = OrgEmployments insert (
              o,
              employee.id.get,
              deptId,
              joinDate getOrElse dateNow,
              createdBy = sCreatedBy)

            EmployeeInfo(
              employee.id.get,
              employee.empNo,
              // employee.jobTitle,              
              employee.userId,
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
              user.workAddress,
              user.contacts,
              user.suspended,
              Some(orgEmployee.org),
              orgEmployee.deptId)

          case _ =>

            EmployeeInfo(
              employee.id.get,
              employee.empNo,
              // employee.jobTitle,              
              employee.userId,
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
              user.workAddress,
              user.contacts,
              user.suspended,
              None, None)
        }
      }
    }

    def updateEmployee(org: Uuid, id: Uuid, spec: EmployeeSpec) =
      db.withTransaction {
        implicit session =>

          oq.userId(id).firstOption match {
            case Some(userId) =>

              val currentTimestamp = Some(now: java.time.LocalDateTime)

              val _1 = userService.updateUser(userId, spec)

              /*            val _2 = _1 && (spec.jobTitle map {
              jobTitle =>

                val _1 = (oq.updates
                   .jobTitle(uid)
                   .update(jobTitle) == 1)
                
                _1 && (oq.updates
                   .modified(userId)
                   .update((currentTimestamp, updatedBy)) == 1)

            } getOrElse true)
*/
              _1 && (spec.dept foreach {
                deptId =>

                  val _1 = (oq.updates
                    .dept(org, id)
                    .update(deptId) == 1)

                  _1 && (oq.updates
                    .modified(userId)
                    .update(currentTimestamp, spec.updatedBy) == 1)

              })

            case _ => false
          }
      }

    def fetchEmployees =
      db.withSession { implicit session =>
        oq.employees
          .all
          .list map {
            case (
              id,
              empNo,
              userId,
              cin,
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
              org,
              deptId
              ) =>
              EmployeeInfo(
                id,
                empNo,
                userId,
                cin,
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
                org,
                deptId)
          }
      }

    def fetchTrashedEmployees =
      db.withSession { implicit session =>
        oq.employees
          .trash
          .list map {
            case (
              id,
              empNo,
              userId,
              cin,
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
              org,
              deptId
              ) =>
              EmployeeInfo(
                id,
                empNo,
                userId,
                cin,
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
                org,
                deptId)
          }
      }

    def fetchEmployee(id: Uuid) =
      db.withSession { implicit session =>
        oq.employees
          .id(id)
          .firstOption map {
            case (
              sId,
              empNo,
              userId,
              cin,
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
              org,
              deptId
              ) =>
              EmployeeInfo(
                sId,
                empNo,
                userId,
                cin,
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
                org,
                deptId)
          }
      }

    def searchEmployeesByCIN(cin: String) =
      db.withSession { implicit session =>
        oq.employees
          .likeCIN(cin)
          .list map {
            case (
              id,
              empNo,
              userId,
              sCIN,
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
              org,
              deptId
              ) =>
              EmployeeInfo(
                id,
                empNo,
                userId,
                sCIN,
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
                org,
                deptId)
          }
      }

    def fetchEmployeeByCIN(cin: String) =
      db.withSession { implicit session =>
        oq.employees
          .byCIN(cin)
          .firstOption map {
            case (
              id,
              empNo,
              userId,
              sCIN,
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
              org,
              deptId
              ) =>
              EmployeeInfo(
                id,
                empNo,
                userId,
                sCIN,
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
                org,
                deptId)
          }
      }

    def fetchEmployements(id: Uuid) =
      db.withSession { implicit session =>
        oq.employees
          .work(id)
          .list map Employment.tupled
      }

    def endEmployment(org: Uuid, id: Uuid, reason: ClosureStatus, remarques: Option[String]) =
      db.withTransaction { implicit session =>

        oq.employees.activeBatch(org, id).firstOption collect {
          case Batch(_, name, _, courseId, subjectId, _, _, createdAt, createdBy, batchId) =>

            assert(
              oq.employees
                .org(org, id)
                .update(Some(now), Some(reason), remarques) == 1)

            val teachingHistory = TeachingHistories insert (
              id,
              batchId.get,
              createdAt.toLocalDate, /* if employee changed, most have been modified to match his starting date */
              now, /* endDate */
              now, /* this record createdAt */
              createdBy = createdBy)

            Employment(
              batchId.get,
              org,
              name,
              Some(id),
              courseId,
              subjectId,
              teachingHistory.id,
              teachingHistory.startDate,
              Some(teachingHistory.endDate),
              teachingHistory.createdAt,
              teachingHistory.createdBy)
        }
      }

    def fetchOrgEmploymentEvents(
      id: Uuid,
      startDate: java.time.LocalDate,
      endDate: Option[java.time.LocalDate]) = ???

    def purgeEmployee(id: Uuid) =
      db.withTransaction { implicit s =>

        oq.userId(id).firstOption match {
          case Some(oId) =>
            userService.purgeUsers(Set(oId))
          case _ => {}
        }

        OrgEmployments deleteForEmployee (id)
        Employees delete (id)
      }*/
  }
}