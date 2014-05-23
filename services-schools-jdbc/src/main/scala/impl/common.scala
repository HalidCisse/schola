package ma.epsilon.schola
package school

package impl

import _root_.ma.epsilon.schola.domain._, school.domain._

import scala.util.control.NonFatal

trait CommonSchoolServicesComponentImpl extends CommonSchoolServicesComponent {
  this: CommonSchoolServicesRepoComponent =>

  trait CommonServices extends Common {

    def getCompaigns(org: Uuid) = commonServicesRepo.getCompaigns(org)
    def newCompaign(org: Uuid, duration: Range[java.time.Instant], moduleType: ModuleType) = commonServicesRepo.newCompaign(org, duration, moduleType)
    def updateCompaign(id: Uuid, newDuration: Range[java.time.Instant]) = commonServicesRepo.updateCompaign(id, newDuration)
    def delCompaign(id: Uuid) = commonServicesRepo.delCompaign(id)

    def getOrgCompositions(compaign: Uuid) = commonServicesRepo.getOrgCompositions(compaign)
    def newComposition(compaign: Uuid, name: String, duration: Range[java.time.Instant], coefficient: Option[Double]) = commonServicesRepo.newComposition(compaign, name, duration, coefficient)
    def updateComposition(id: Uuid, spec: ScheduledSpec) = commonServicesRepo.updateComposition(id, spec)
    def delComposition(id: Uuid) = commonServicesRepo.delComposition(id)

    def getUniversities = commonServicesRepo.getUniversities
    def saveUniversity(name: String, website: Option[String], contacts: ContactInfo, address: AddressInfo) = commonServicesRepo.saveUniversity(name, website, contacts, address)
    def updateUniversity(id: Uuid, spec: UniversitySpec) = commonServicesRepo.updateUniversity(id, spec)
    def delUniversity(id: Uuid) = commonServicesRepo.delUniversity(id)

    def getOrgs = commonServicesRepo.getOrgs
    def getTrashedOrgs = commonServicesRepo.getTrashedOrgs
    def saveOrg(name: String, acronyms: Option[String], website: Option[String], university: Option[Uuid], contacts: ContactInfo, address: AddressInfo, createdBy: Option[Uuid]) = commonServicesRepo.saveOrg(name, acronyms, website, university, contacts, address, createdBy)
    def updateOrg(id: Uuid, spec: OrgSpec) = commonServicesRepo.updateOrg(id, spec)
    def updateOrgSettings(org: Uuid, spec: OrgSettingSpec) = commonServicesRepo.updateOrgSettings(org, spec)
    def fetchOrgSettings(org: Uuid) = commonServicesRepo.fetchOrgSettings(org)
    def delOrg(id: Uuid) = commonServicesRepo.delOrg(id)
    def purgeOrg(id: Uuid) = commonServicesRepo.purgeOrg(id)

    def getDepts(org: Uuid) = commonServicesRepo.getDepts(org)
    def saveDept(name: String, org: Uuid, departmentChefId: Option[Uuid]) = commonServicesRepo.saveDept(name, org, departmentChefId)
    def updateDept(id: Uuid, spec: DeptSpec) = commonServicesRepo.updateDept(id, spec)
    def delDept(id: Uuid) = commonServicesRepo.delDept(id)
  }
}

trait CommonSchoolServicesRepoComponentImpl extends CommonSchoolServicesRepoComponent {
  this: jdbc.WithDatabase =>

  import school.schema._
  import jdbc.Q._

  val commonServicesRepo = new CommonServicesRepoImpl

  class CommonServicesRepoImpl extends CommonServicesRepo {

    private[this] object oq {

      val universities = {

        def forWebsite(id: Column[Uuid]) =
          Universities filter (_.id === id) map (o => (o.website))

        def getUniversity(id: Column[Uuid]) =
          Universities filter (_.id === id)

        def forName(id: Column[Uuid]) =
          Universities filter (_.id === id) map (o => (o.name))

        def forAddress(id: Column[Uuid]) =
          Universities filter (_.id === id) map (o => (o.address))

        def forContacts(id: Column[Uuid]) =
          Universities filter (_.id === id) map (o => (o.contacts))

        new {
          val id = Compiled(getUniversity _)
          val all = Compiled { Universities map identity }
          val address = Compiled(forAddress _)
          val name = Compiled(forName _)
          val website = Compiled { forWebsite _ }
          val contacts = Compiled(forContacts _)
        }
      }

      val settings = {

        def getOrgSettings(org: Column[Uuid]) =
          OrgSettings
            .filter(_.org === org)

        def forSessionDuration(org: Column[Uuid]) =
          OrgSettings
            .filter(_.org === org)
            .map(_.sessionDuration)

        def forStartOfInscription(org: Column[Uuid]) =
          OrgSettings
            .filter(_.org === org)
            .map(_.startOfInscription)

        def forEndOfInscription(org: Column[Uuid]) =
          OrgSettings
            .filter(_.org === org)
            .map(_.endOfInscription)

        def forWeekDays(org: Column[Uuid]) =
          OrgSettings
            .filter(_.org === org)
            .map(_.weekDays)

        def forAttendance(org: Column[Uuid]) =
          OrgSettings
            .filter(_.org === org)
            .map(_.attendanceEnabled)

        new {
          val org = Compiled { getOrgSettings _ }
          val sessionDuration = Compiled { forSessionDuration _ }
          val weekDays = Compiled { forWeekDays _ }
          val startOfInscription = Compiled { forStartOfInscription _ }
          val endOfInscription = Compiled { forEndOfInscription _ }
          val attendanceEnabled = Compiled { forAttendance _ }
        }
      }

      val orgs = {

        def getOrg(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id)

        def forWebsite(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id) map (o => (o.website))

        def forName(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id) map (o => (o.name))

        def forAcronyms(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id) map (o => (o.acronyms))

        def forAddress(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id) map (o => (o.address))

        def forContacts(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id) map (o => (o.contacts))

        def forUniversity(id: Column[Uuid]) =
          Orgs filter (org => org._deleted === false && org.id === id) map (o => (o.universityId))

        new {
          val id = Compiled(getOrg _)
          val all = Compiled(Orgs filter (_._deleted === false))
          val trash = Compiled(Orgs filter (_._deleted === true))
          val name = Compiled(forName _)
          val acronyms = Compiled(forAcronyms _)
          val website = Compiled { forWebsite _ }
          val address = Compiled(forAddress _)
          val contacts = Compiled(forContacts _)
          val university = Compiled(forUniversity _)
        }
      }

      val depts = {

        def forNamed(args: (Column[Uuid], Column[String])) =
          Depts filter (d => args._1 === d.org && args._2.toLowerCase === d.name.toLowerCase)

        def forName(id: Column[Uuid]) =
          Depts filter (_.id === id) map (_.name)

        def forDeptChef(id: Column[Uuid]) =
          Depts filter (_.id === id) map (_.departmentChefId)

        def getDepts(org: Column[Uuid]) =
          Depts filter (_.org === org)

        new {
          val org = Compiled(getDepts _)
          val all = Compiled { Depts map identity }
          val name = Compiled(forName _)
          val named = Compiled(forNamed _)
          val departmentChef = Compiled(forDeptChef _)
        }
      }

      val compaigns = {

        def forDuring(id: Column[Uuid]) =
          Compaigns filter (_.id === id) map (_.during)        

        def forCompaigns(org: Column[Uuid]) =
          Compaigns filter(compaign => compaign.org === org)          

        new {
          val org = Compiled{forCompaigns _}          
          val during = Compiled{forDuring _}
        }
      }

      val compositions = {

        def forDuring(id: Column[Uuid]) =
          OrgCompositions filter (_.id === id) map (_.during)        

        def forCoefficient(id: Column[Uuid]) =
          OrgCompositions filter (_.id === id) map (_.coefficient)

        def forName(id: Column[Uuid]) =
          OrgCompositions filter (_.id === id) map (_.name)   

        def forCompaign(id: Column[Uuid]) =
          OrgCompositions 
            .innerJoin(
              Compaigns
                .filter(compaign => compaign.id === id))
            .on(_.compaignId === _.id)
            .map(_._1)                         

        new {
          val name = Compiled{forName _}
          val during = Compiled{forDuring _}
          val coefficient = Compiled{forCoefficient _}
          val compaign = Compiled{forCompaign _}
        }
      }
    }

    def getCompaigns(org: Uuid) = 
      db.withSession {
        implicit session => 
          oq.compaigns
            .org(org)
            .list
      }

    def newCompaign(org: Uuid, duration: Range[java.time.Instant], moduleType: ModuleType) = 
      db.withTransaction { implicit s =>
        Compaigns insert(org, duration, moduleType)
      }    

    def updateCompaign(id: Uuid, newDuration: Range[java.time.Instant]) =
      db.withTransaction { implicit s =>
        oq.compaigns
          .during(id)
          .update(
            com.github.tminglei.slickpg.Range[java.sql.Timestamp](
              java.sql.Timestamp.from(newDuration.start),
              java.sql.Timestamp.from(newDuration.end),
              com.github.tminglei.slickpg.`[_,_]`)
          ) == 1
      }

    def delCompaign(id: Uuid) = 
      db.withTransaction { implicit s =>
        Compaigns delete(id)
      }

    def getOrgCompositions(compaign: Uuid) = 
      db.withSession {
        implicit session => 
          oq.compositions
            .compaign(compaign)
            .list
      }

    def newComposition(compaign: Uuid, name: String, duration: Range[java.time.Instant], coefficient: Option[Double]) = 
      db.withTransaction { implicit s =>
        OrgCompositions insert(compaign, name, duration, coefficient)
      }    
    
    def updateComposition(id: Uuid, spec: ScheduledSpec) =
      db.withTransaction { implicit s =>

        val _1 = spec.name map {
          name =>

            oq.compositions
              .name(id)
              .update(name) == 1

        } getOrElse true

        val _2 = _1 && (spec.during map {
          during =>

            oq.compositions
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

            oq.compositions
              .coefficient(id)
              .update(Some(coefficient)) == 1

        } getOrElse true)        
      }    

    def delComposition(id: Uuid) = 
      db.withTransaction { implicit s =>
        OrgCompositions delete(id)
      }

    def getUniversities =
      db.withSession {
        implicit session => oq.universities.all.list
      }

    def saveUniversity(name: String, website: Option[String], contacts: ContactInfo, address: AddressInfo) =
      db.withTransaction { implicit session =>
        Universities insert (name, website, contacts, address)
      }

    def updateUniversity(id: Uuid, spec: UniversitySpec) =
      db.withSession { implicit s => oq.universities.id(id).firstOption } match {
        case Some(University(_, _, sContactInfo, sAddress, _)) =>

          db.withTransaction { implicit session =>

            val _1 = spec.name map {
              name => oq.universities.name(id).update(name) == 1
            } getOrElse true

            val _2 = _1 && (spec.website map {
              website => oq.universities.website(id).update(Some(website)) == 1
            } getOrElse true)

            val _3 = _2 && (spec.address foreach {
              case Some(s) =>

                sAddress match {

                  case AddressInfo(curCity, curCountry, curPostalCode, curStreetAddress) =>

                    oq.universities
                      .address(id)
                      .update(
                        AddressInfo(
                          city = if (s.city.isEmpty) curCity else s.city.set.get,
                          country = if (s.country.isEmpty) curCountry else s.country.set.get,
                          postalCode = if (s.postalCode.isEmpty) curPostalCode else s.postalCode.set.get,
                          streetAddress = if (s.streetAddress.isEmpty) curStreetAddress else s.streetAddress.set.get)) == 1

                  case _ =>

                    oq.universities
                      .address(id)
                      .update(
                        AddressInfo(
                          city = if (s.city.isEmpty) None else s.city.set.get,
                          country = if (s.country.isEmpty) None else s.country.set.get,
                          postalCode = if (s.postalCode.isEmpty) None else s.postalCode.set.get,
                          streetAddress = if (s.streetAddress.isEmpty) None else s.streetAddress.set.get)) == 1

                }

              case _ => oq.universities.address(id).update(AddressInfo()) == 1
            })

            _3 && (spec.contacts foreach {
              case Some(s) =>

                val newContactInfo =
                  ContactInfo(
                    email = utils.If(s.email.set eq None, sContactInfo.email, s.email.set.get),
                    fax = utils.If(s.fax.set eq None, sContactInfo.fax, s.fax.set.get),
                    phoneNumber = utils.If(s.phoneNumber.set eq None, sContactInfo.phoneNumber, s.phoneNumber.set.get))

                oq.universities.contacts(id).update(newContactInfo) == 1

              case _ => oq.universities.contacts(id).update(ContactInfo()) == 1
            })
          }

        case _ => false
      }

    def delUniversity(id: Uuid) =
      db.withTransaction { implicit session =>
        Universities delete (id)
      }

    def getOrgs =
      db.withSession { implicit session =>
        oq.orgs.all.list
      }

    def getTrashedOrgs =
      db.withSession { implicit session =>
        oq.orgs.trash.list
      }

    def saveOrg(name: String, acronyms: Option[String], website: Option[String], universityId: Option[Uuid], contacts: ContactInfo, address: AddressInfo, createdBy: Option[Uuid]) =
      db.withTransaction { implicit session =>

        def accessRightsByAlias(aliases: List[String]) =
          AccessRights
            .filter(_.alias inSet Set(aliases: _*))
            .map(_.id)
            .list

        def ofOrg(org: Uuid) = {
          import Schools._, accessRights._

          List(

            /* Director of org */
            AccessRight(
              DIRECTOR(org),
              s"${DISPLAYNAME(DIRECTOR_ALIAS)} - ${name}",
              redirectUri = "/Director",
              appId = appId,
              scopes = scopes.DIRECTOR,
              grantOptions = accessRightsByAlias(List(STUDENT, TEACHER, SUPERVISOR))),

            /* admission officer of org */
            AccessRight(
              ADMISSION_OFFICER(org),
              s"${DISPLAYNAME(ADMISSION_OFFICER_ALIAS)} - ${name}",
              redirectUri = "/AdmissionOfficer",
              appId = appId,
              scopes = scopes.ADMISSION_OFFICER,
              grantOptions = List()))
        }

        val org@Org(_, _, _, _, _, _, _, _, _, Some(id)) = Orgs insert (name, acronyms, website, universityId, contacts, address, createdBy)

        Modules insert(id, ":M1")

        for (right <- ofOrg(id))
          AccessRights.insert(right)

        org
      }

    def updateOrg(id: Uuid, spec: OrgSpec) =
      db.withSession { implicit s => oq.orgs.id(id).firstOption } match {
        case Some(Org(_, _, _, sContactInfo, sAddress, sUniversityId, _, _, _, _)) =>

          db.withTransaction { implicit session =>

            val _1 = spec.name map {
              name => oq.orgs.name(id).update(name) == 1
            } getOrElse true

            val _2 = _1 && (spec.acronyms map {
              acronyms => oq.orgs.acronyms(id).update(Some(acronyms)) == 1
            } getOrElse true)

            val _3 = _2 && (spec.website map {
              website => oq.orgs.website(id).update(Some(website)) == 1
            } getOrElse true)

            val _4 = _3 && (spec.address foreach {
              case Some(s) =>

                sAddress match {

                  case AddressInfo(curCity, curCountry, curPostalCode, curStreetAddress) =>

                    oq.orgs
                      .address(id)
                      .update(
                        AddressInfo(
                          city = if (s.city.isEmpty) curCity else s.city.set.get,
                          country = if (s.country.isEmpty) curCountry else s.country.set.get,
                          postalCode = if (s.postalCode.isEmpty) curPostalCode else s.postalCode.set.get,
                          streetAddress = if (s.streetAddress.isEmpty) curStreetAddress else s.streetAddress.set.get)) == 1

                  case _ =>

                    oq.orgs
                      .address(id)
                      .update(
                        AddressInfo(
                          city = if (s.city.isEmpty) None else s.city.set.get,
                          country = if (s.country.isEmpty) None else s.country.set.get,
                          postalCode = if (s.postalCode.isEmpty) None else s.postalCode.set.get,
                          streetAddress = if (s.streetAddress.isEmpty) None else s.streetAddress.set.get)) == 1

                }

              case _ =>

                oq.orgs
                  .address(id)
                  .update(AddressInfo()) == 1
            })

            val _5 = _4 && (spec.contacts foreach {
              case Some(s) =>

                val newContactInfo =
                  ContactInfo(
                    email = utils.If(s.email.set eq None, sContactInfo.email, s.email.set.get),
                    fax = utils.If(s.fax.set eq None, sContactInfo.fax, s.fax.set.get),
                    phoneNumber = utils.If(s.phoneNumber.set eq None, sContactInfo.phoneNumber, s.phoneNumber.set.get))

                oq.orgs.contacts(id).update(newContactInfo) == 1

              case _ => oq.orgs.contacts(id).update(ContactInfo()) == 1
            })

            _5 && (spec.university foreach {
              case university => oq.orgs.university(id).update(university) == 1
            })
          }

        case _ => false
      }

    def updateOrgSettings(org: Uuid, spec: OrgSettingSpec) =
      db.withTransaction { implicit s =>

        val _1 = spec.sessionDuration map {
          sessionDuration =>

            oq.settings
              .sessionDuration(org)
              .update(sessionDuration) == 1

        } getOrElse true

        val _2 = _1 && (spec.weekDays map {
          weekDays =>

            oq.settings
              .weekDays(org)
              .update(weekDays) == 1

        } getOrElse true)

        val _3 = _2 && (spec.startOfInscription map {
          startOfInscription =>

            oq.settings
              .startOfInscription(org)
              .update(Some(startOfInscription)) == 1

        } getOrElse true)

        val _4 = _3 && (spec.endOfInscription map {
          endOfInscription =>

            oq.settings
              .endOfInscription(org)
              .update(Some(endOfInscription)) == 1

        } getOrElse true)

        _4 && (spec.attendanceEnabled map {
          attendanceEnabled =>

            oq.settings
              .attendanceEnabled(org)
              .update(attendanceEnabled) == 1

        } getOrElse true)
      }

    def fetchOrgSettings(org: Uuid) =
      db.withSession { implicit s => oq.settings.org(org).firstOption } match {
        case Some(settings) => settings
        case _ =>

          db.withTransaction { implicit s =>

            OrgSettings insert (
              org,
              java.time.Duration.ofMinutes(config.getInt("schools.default_session_duration")),
              WeekDays(), None, None,
              attendanceEnabled = true)
          }
      }

    def delOrg(id: Uuid) =
      db.withTransaction { implicit session =>
        Orgs delete (id)
      }

    def purgeOrg(id: Uuid) =
      db.withTransaction { implicit session =>
        Orgs purge (id)
      }

    def getDepts(org: Uuid) =
      db.withSession { implicit session =>
        oq.depts.org(org).list
      }

    def saveDept(sName: String, org: Uuid, sDepartmentChefId: Option[Uuid]) =
      db.withSession { implicit s => oq.depts.named(org, sName).firstOption } match {
        case Some(Dept(_, _, _, Some(id))) =>
          updateDept(id, new DefaultDeptSpec {
            override lazy val name = Some(sName)
            override lazy val departmentChefId = UpdateSpecImpl[Uuid](set = sDepartmentChefId map Option[Uuid])
          }); Dept(sName, org, sDepartmentChefId, id = Some(id))
        case _ =>
          db.withTransaction { implicit session =>
            Depts insert (sName, org, sDepartmentChefId)
          }
      }

    def updateDept(id: Uuid, spec: DeptSpec) =
      db.withTransaction { implicit s =>

        val _1 = spec.name map {
          name => oq.depts.name(id).update(name) == 1
        } getOrElse true

        _1 && (spec.departmentChefId foreach {
          case chef => oq.depts.departmentChef(id).update(chef) == 1
        })
      }

    def delDept(id: Uuid) =
      db.withTransaction { implicit session =>
        Depts delete (id)
      }
  }
}