package ma.epsilon.schola

package impl

trait UserServicesComponentImpl extends UserServicesComponent {
  this: UserServicesRepoComponent =>

  class UserServicesImpl extends UserServices {

    def getUsersStats = userServiceRepo.getUsersStats

    def getUsers(page: Int) = userServiceRepo.getUsers(page)

    def getUser(id: String) = userServiceRepo.getUser(id)

    def getUserByCIN(cin: String) = userServiceRepo.getUserByCIN(cin)

    def removeUser(id: String) = userServiceRepo.removeUser(id)

    def removeUsers(users: Set[String]) = userServiceRepo.removeUsers(users)

    def getPurgedUsers = userServiceRepo.getPurgedUsers

    def purgeUsers(users: Set[String]) = userServiceRepo.purgeUsers(users)

    def undeleteUsers(users: Set[String]) = userServiceRepo.undeleteUsers(users)

    def suspendUsers(users: Set[String]) = userServiceRepo.suspendUsers(users)

    def saveUser(
      cin: String, 
      username: String, 
      password: String, 
      givenName: String, 
      familyName: String, 
      createdBy: Option[String], 
      gender: domain.Gender, 
      homeAddress: Option[domain.AddressInfo], 
      workAddress: Option[domain.AddressInfo], 
      contacts: Option[domain.Contacts], 
      suspended: Boolean, 
      changePasswordAtNextLogin: Boolean, 
      accessRights: List[String]) = userServiceRepo.saveUser(cin, username, password, givenName, familyName, createdBy, gender, homeAddress, workAddress, contacts, suspended, changePasswordAtNextLogin, accessRights)

    def updateUser(id: String, spec: domain.UserSpec) = userServiceRepo.updateUser(id, spec)

    def primaryEmailExists(primaryEmail: String) = userServiceRepo.primaryEmailExists(primaryEmail)

    def labelUser(userId: String, labels: Set[String]) {
      userServiceRepo.labelUser(userId, labels)
    }

    def unLabelUser(userId: String, labels: Set[String]) {
      userServiceRepo.unLabelUser(userId, labels)
    }

    def getUserLabels(userId: String) = userServiceRepo.getUserLabels(userId)

    def createPasswdResetReq(username: String) = userServiceRepo.createPasswdResetReq(username)

    def checkActivationReq(username: String, ky: String) = userServiceRepo.checkActivationReq(username, ky)

    def resetPasswd(username: String, ky: String, newPasswd: String) = userServiceRepo.resetPasswd(username, ky, newPasswd)

    def getPage(userId: String) = userServiceRepo.getPage(userId)
  }
}

trait UserServicesRepoComponentImpl extends UserServicesRepoComponent {
  this: UploadServicesComponent with MailingComponent =>

  import schema._
  import domain._
  import jdbc.Q._

  private[this] val log = Logger("oadmin.userServiceRepoImpl")

  protected val db: Database

  protected val userServiceRepo = new UserServicesRepoImpl

  class UserServicesRepoImpl extends UserServicesRepo {

    private[this] object oq {

      object Convs {
        import scala.slick.jdbc._
        import play.api.libs.json.Json
        import conversions.json._

        implicit object SetUUIDOption extends SetParameter[Option[java.util.UUID]] { def apply(v: Option[java.util.UUID], pp: PositionedParameters) { pp.setObjectOption(v, java.sql.Types.OTHER) } }

        implicit object GetUUIDOption extends GetResult[Option[java.util.UUID]] { def apply(rs: PositionedResult) = rs.nextStringOption map uuid }
        implicit object GetGender extends GetResult[domain.Gender] { def apply(rs: PositionedResult) = domain.Gender.withName(rs.nextString()) }
        implicit object GetAddressOption extends GetResult[Option[domain.AddressInfo]] { def apply(rs: PositionedResult) = rs.nextStringOption map(s => Json.parse(s).as[domain.AddressInfo]) }
        implicit object GetContacts extends GetResult[Option[domain.Contacts]] { def apply(rs: PositionedResult) = rs.nextStringOption map (s => Json.parse(s).as[domain.Contacts]) }
        implicit object GetScopes extends GetResult[Option[List[domain.Scope]]] { def apply(rs: PositionedResult) = rs.nextStringOption map (s => Json.parse(s).as[List[domain.Scope]]) }
        
        import java.util.Calendar

        def sqlTimestamp2LocalDateTime(ts: java.sql.Timestamp): java.time.LocalDateTime = {
            val cal = Calendar.getInstance()
            cal.setTime(ts)
            java.time.LocalDateTime.of(
              cal.get(Calendar.YEAR),
              cal.get(Calendar.MONTH) +1,
              cal.get(Calendar.DAY_OF_MONTH),
              cal.get(Calendar.HOUR_OF_DAY),
              cal.get(Calendar.MINUTE),
              cal.get(Calendar.SECOND),
              cal.get(Calendar.MILLISECOND) * 1000
            )
          }

        implicit object GetLocalDateTime extends GetResult[java.time.LocalDateTime] { 
          def apply(rs: PositionedResult) = 
            rs.nextTimestampOption map(sqlTimestamp2LocalDateTime) getOrElse null.asInstanceOf[java.time.LocalDateTime] 
        }

        implicit object GetLocalDateTimeOption extends GetResult[Option[java.time.LocalDateTime]] { 
          def apply(rs: PositionedResult) = 
            rs.nextTimestampOption map sqlTimestamp2LocalDateTime
        }
      }

      def users(page: Int) = {
        import scala.slick.jdbc.{ StaticQuery => T }
        import T.interpolation

        import Convs._

        type ResultType =
          Tuple19[
            Option[java.util.UUID], 
            String, 
            String, 
            String, 
            String, 
            java.time.LocalDateTime, 
            Option[java.util.UUID], 
            Option[java.time.LocalDateTime], 
            Option[java.time.LocalDateTime], 
            Option[java.util.UUID], 
            Int, 
            domain.Gender, 
            Option[domain.AddressInfo], 
            Option[domain.AddressInfo], 
            Option[domain.Contacts], 
            Boolean, 
            Boolean, 
            Option[String], 
            Option[java.util.UUID]/*, 
            Option[java.util.UUID], 
            Option[String], 
            Option[String], 
            Option[String], 
            Option[java.util.UUID], 
            Option[List[Scope]]*/]

        sql"""select
                x5.x3, x5.x4, x5.x5, x5.x6, x5.x7, x5.x8, x5.x9, x5.x10, x5.x11, x5.x12, x5.x13, x5.x14, x5.x15, x5.x16, x5.x17, x5.x18, x5.x19, x3.label, x5.user_id/*, x5.access_right_id, x5.access_right_alias, x5.access_right_display_name, x5.access_right_redirect_uri, x5.access_right_app_id, x5.access_right_scopes*/
              from ((
                  select x19."id" as x3,
                    x19."cin" as x4,
                    x19."primary_email" as x5,
                    x19."given_name" as x6,
                    x19."family_name" as x7,
                    x19."created_at" as x8,
                    x19."created_by" as x9,
                    x19."last_login_time" as x10,
                    x19."last_modified_at" as x11,
                    x19."last_modified_by" as x12,
                    x19."stars" as x13,
                    x19."gender" as x14,
                    x19."home_address" as x15,
                    x19."work_address" as x16,
                    x19."contacts" as x17,
                    x19."suspended" as x18,
                    x19."change_password_at_next_login" as x19
                  from "users" x20 where not x20."_deleted" and x20."id" <> ${U.SuperUser.id}
                  /* order by last_login_time desc nulls last, order by last_modified_at desc nulls last, created_at desc */
                  limit $MaxResults offset ${page * MaxResults}
                ) x2 left join (
                    select x41.user_id as "user_id"/*,
                      x42.id as "access_right_id",
                      x42.alias as "access_right_alias",
                      x42.display_name as "access_right_display_name",
                      x42.redirect_uri as "access_right_redirect_uri",
                      x42.app_id as "access_right_app_id",
                      x42.scopes as "access_right_scopes"*/
                    from users_access_rights x41 left join access_rights x42 on (x41."access_right_id" = x42."id")
                ) x4 on (x2.x3 = x4."user_id")) x5 left join users_labels x3 on (x5.x3 = x3."user_id");""".as[ResultType]
      }

      val trashedUsers = Compiled(for {
        u <- Users if u._deleted
      } yield (
        u.id,
        u.cin,
        u.primaryEmail,
        u.givenName,
        u.familyName,
        u.createdAt,
        u.createdBy,
        u.lastLoginTime,
        u.lastModifiedAt,
        u.lastModifiedBy,
        u.stars,
        u.gender,
        u.homeAddress,
        u.workAddress,
        u.contacts,
        u.changePasswordAtNextLogin))

      val userById = {

        def getUser(id: Column[java.util.UUID]) =
          for {
            u <- Users if !u._deleted && (u.id === id)
          } yield (
            u.id,
            u.cin,
            u.primaryEmail,
            u.password,
            u.givenName,
            u.familyName,
            u.createdAt,
            u.createdBy,
            u.lastLoginTime,
            u.lastModifiedAt,
            u.lastModifiedBy,
            u.stars,
            u.gender,
            u.homeAddress,
            u.workAddress,
            u.contacts,
            u.changePasswordAtNextLogin)

        Compiled(getUser _)
      }

      val userByCIN = {

        def getUser(cin: Column[String]) =
          for {
            u <- Users if !u._deleted && (u.cin === cin)
          } yield (
            u.id,
            u.cin,
            u.primaryEmail,
            u.password,
            u.givenName,
            u.familyName,
            u.createdAt,
            u.createdBy,
            u.lastLoginTime,
            u.lastModifiedAt,
            u.lastModifiedBy,
            u.stars,
            u.gender,
            u.homeAddress,
            u.workAddress,
            u.contacts,
            u.changePasswordAtNextLogin)

        Compiled(getUser _)
      }

      val primaryEmailExists = {
        def getPrimaryEmail(primaryEmail: Column[String]) =
          Users
            .filter(_.primaryEmail.toLowerCase === primaryEmail)
            .exists

        Compiled(getPrimaryEmail _)
      }

      val userLabels = {
        def getUserLabels(userId: Column[java.util.UUID]) =
          UsersLabels filter (_.userId === userId) map (_.label)

        Compiled(getUserLabels _)
      }

      val labelled = {
        def getLabel(label: Column[String]) =
          Labels filter (_.name === label)

        Compiled(getLabel _)
      }

      val forActivationKey = {
        def getActivationKey(username: Column[String]) =
          Users filter (_.primaryEmail === username) map (o => (o.activationKey, o.suspended))

        Compiled(getActivationKey _)
      }

      val forActivation = {
        def getActivation(username: Column[String]) =
          Users filter (_.primaryEmail === username) map (o => (o.activationKey, o.password, o.suspended))

        Compiled(getActivation _)
      }

      val userUpdates = {

        def forPrimaryEmail(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.primaryEmail, o.lastModifiedAt, o.lastModifiedBy))

        def forCIN(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.cin, o.lastModifiedAt, o.lastModifiedBy))

        def forStars(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.stars, o.lastModifiedAt, o.lastModifiedBy))

        def forPasswd(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.password, o.changePasswordAtNextLogin, o.lastModifiedAt, o.lastModifiedBy))

        def forFN(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.givenName, o.lastModifiedAt, o.lastModifiedBy))

        def forLN(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.familyName, o.lastModifiedAt, o.lastModifiedBy))

        def forGender(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.gender, o.lastModifiedAt, o.lastModifiedBy))

        def forSuspended(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.suspended, o.lastModifiedAt, o.lastModifiedBy))

        def forHomeAddress(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.homeAddress, o.lastModifiedAt, o.lastModifiedBy))

        def forWorkAddress(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.workAddress, o.lastModifiedAt, o.lastModifiedBy))

        def forContacts(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (o => (o.contacts, o.lastModifiedAt, o.lastModifiedBy))

        new {
          val cin = Compiled(forCIN _)
          val stars = Compiled(forStars _)
          val primaryEmail = Compiled(forPrimaryEmail _)
          val password = Compiled(forPasswd _)
          val givenName = Compiled(forFN _)
          val familyName = Compiled(forLN _)
          val gender = Compiled(forGender _)
          val suspended = Compiled(forSuspended _)
          val homeAddress = Compiled(forHomeAddress _)
          val workAddress = Compiled(forWorkAddress _)
          val contacts = Compiled(forContacts _)
        }
      }
    }

    def getUsersStats =
      UsersStats(db.withSession { implicit session =>
        Users.length.run - 1 /* Don't return the super user row */
      })

    def getUsers(page: Int) = { // TODO: possible bug on large number of users
      import Database.dynamicSession

      val users = oq.users(page)

      val result = db.withDynSession {
        users.list
      }

      result.groupBy(_._1).flatMap {
        case (id, user :: rest) =>
          user match {
            case (
              _,
              cin,
              primaryEmail,
              givenName,
              familyName,
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
              changePasswordAtNextLogin,
              label,
              _/*,
              accessRightId,
              accessRightAlias,
              accessRightDisplayName,
              accessRightRedirectUri,
              accessRightAppId,
              scopes*/) =>

              User(
                cin,
                primaryEmail,
                None,
                givenName,
                familyName,
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
                suspended = suspended,
                changePasswordAtNextLogin = changePasswordAtNextLogin,
                id = id,
                labels = if (label.isDefined) label.get :: rest.filter(_._18.isDefined).map(_._18.get) else rest.filter(_._18.isDefined).map(_._18.get)/*,
                accessRights = if (accessRightId.isDefined) AccessRight(accessRightAlias.get, accessRightDisplayName.get, accessRightRedirectUri.get, accessRightAppId.get, scopes.get, id = accessRightId) :: rest.filter(_._18.isDefined).map(o => AccessRight(o._19.get, o._20.get, o._21.get, o._22.get, o._23.get, id = o._18)) else rest.filter(_._18.isDefined).map(o => AccessRight(o._19.get, o._20.get, o._21.get, o._22.get, o._23.get, id = o._18))*/) :: Nil
          }
      }.toList
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val user = oq.userById(uuid(id))

      val result = db.withDynSession {
        user.firstOption
      }

      result map {
        case (sId, cin, primaryEmail, _, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) =>
          User(cin, primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(sId), labels = getUserLabels(sId.toString))
      }
    }

    def getUserByCIN(cin: String) = {
      import Database.dynamicSession

      val user = oq.userByCIN(cin)

      val result = db.withDynSession {
        user.firstOption
      }

      result map {
        case (sId, cin, primaryEmail, _, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) =>
          User(cin, primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(sId), labels = getUserLabels(sId.toString))
      }
    }

    def removeUser(id: String) = db.withSession {
      implicit session =>
        Users.delete(id)
    }

    def removeUsers(users: Set[String]) {
      val q = for { u <- Users if (u.id =!= U.SuperUser.id) && (u.id inSet (users map uuid)) } yield u._deleted

      db.withTransaction { implicit session =>
        q.update(true)
      }
    }

    def getPurgedUsers = {
      import Database.dynamicSession

      val trash = oq.trashedUsers

      val result = db.withDynSession {
        trash.list
      }

      result map {
        case (id, cin, primaryEmail, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) =>
          User(cin, primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(id))
      }
    }

    def purgeUsers(users: Set[String]) = {
      val q = for { u <- Users if (u.id =!= U.SuperUser.id) && (u.id inSet (users map uuid)) } yield u

      users foreach {
        id =>
          if (U.SuperUser.id exists (_.toString != id))
            uploadServices.purgeUpload(id)
      }

      db.withTransaction { implicit session =>
        q.delete
      }
    }

    def undeleteUsers(users: Set[String]) = {
      val deleted = for { u <- Users if u._deleted && (u.id inSet (users map uuid)) } yield u._deleted

      db.withTransaction { implicit session =>
        deleted.update(false)
      }
    }

    def suspendUsers(users: Set[String]) {
      val suspended = for { u <- Users if !u.suspended && (u.id inSet (users map uuid)) } yield u.suspended

      db.withTransaction {
        implicit session => suspended update true
      }
    }

    def saveUser(
      cin: String, 
      primaryEmail: String, 
      password: String, 
      givenName: String, 
      familyName: String, 
      createdBy: Option[String], 
      gender: domain.Gender, 
      homeAddress: Option[domain.AddressInfo], 
      workAddress: Option[domain.AddressInfo], 
      contacts: Option[domain.Contacts], 
      suspended: Boolean, 
      changePasswordAtNextLogin: Boolean, 
      sAccessRights: List[String]) = 
      db.withTransaction { implicit session =>
        val currentTimestamp = java.time.LocalDateTime.now

        try {

          val user =
            Users insert (
              cin,
              primaryEmail,
              passwords crypt password,
              givenName,
              familyName,
              currentTimestamp,
              createdBy map uuid,
              gender = gender,
              homeAddress = homeAddress,
              workAddress = workAddress,
              contacts = contacts,
              suspended = suspended,
              changePasswordAtNextLogin = changePasswordAtNextLogin)

          mailer.sendWelcomeEmail(primaryEmail, password)

          sAccessRights match {
            case Nil => {}
            case rights => utils.tryo {

              updateUser(user.id.get.toString, new DefaultUserSpec {

                override lazy val updatedBy = createdBy

                override lazy val accessRights = Option(Set(rights: _*))

              })
            }
          }

          user

        } catch {
          case scala.util.control.NonFatal(ex) =>

            log.info(s"[saveUser failed with $ex]")
            throw ex
        }
      }

    def updateUser(id: String, spec: UserSpec) = {
      val uid = uuid(id)
      val updatedBy = spec.updatedBy map uuid
      val user = oq.userById(uid)

      db.withSession {
        implicit session => user.firstOption
      } match {

        case Some((_, sUsername, sPassword, _, _, _, _, _, _, _, _, _, _, sHomeAddress, sWorkAddress, sContacts, _)) =>
          db.withTransaction {
            implicit session =>

              val currentTimestamp = Some(java.time.LocalDateTime.now)

              val _1 = spec.cin map {
                cin =>
                  oq.userUpdates.cin(uid).update(cin, currentTimestamp, updatedBy) == 1
              } getOrElse true

              val _2 = spec.stars map {
                stars =>
                  oq.userUpdates.stars(uid).update(stars, currentTimestamp, updatedBy) == 1
              } getOrElse true

              val _3 = _2 && (spec.primaryEmail map {
                primaryEmail =>
                  oq.userUpdates.primaryEmail(uid).update(primaryEmail, currentTimestamp, updatedBy) == 1
              } getOrElse true)

              val _4 = _3 && (spec.password map {
                password =>
                  spec.oldPassword.nonEmpty &&
                    (passwords verify (spec.oldPassword.get, sPassword)) &&
                    (oq.userUpdates.password(uid).update(passwords crypt password, false, currentTimestamp, updatedBy) == 1) &&
                    { mailer.sendPasswordChangedNotice(sUsername); true }
              } getOrElse true)

              val _5 = _4 && (spec.givenName map {
                givenName =>
                  oq.userUpdates.givenName(uid).update(givenName, currentTimestamp, updatedBy) == 1
              } getOrElse true)

              val _6 = _5 && (spec.familyName map {
                familyName =>
                  oq.userUpdates.familyName(uid).update(familyName, currentTimestamp, updatedBy) == 1
              } getOrElse true)

              val _7 = _6 && (spec.gender map {
                gender =>
                  oq.userUpdates.gender(uid).update(gender, currentTimestamp, updatedBy) == 1
              } getOrElse true)

              val _8 = _7 && (spec.suspended map {
                case suspended =>
                  oq.userUpdates.suspended(uid).update(suspended, currentTimestamp, updatedBy) == 1
              } getOrElse true)

              val _9 = _8 && (spec.homeAddress foreach {
                case Some(s) =>

                  sHomeAddress match {

                    case Some(AddressInfo(curCity, curCountry, curPostalCode, curStreetAddress)) =>

                      oq.userUpdates
                        .homeAddress(uid)
                        .update((
                          Some(AddressInfo(
                            city = if (s.city.isEmpty) curCity else s.city.set.get,
                            country = if (s.country.isEmpty) curCountry else s.country.set.get,
                            postalCode = if (s.postalCode.isEmpty) curPostalCode else s.postalCode.set.get,
                            streetAddress = if (s.streetAddress.isEmpty) curStreetAddress else s.streetAddress.set.get)),
                          currentTimestamp,
                          updatedBy)) == 1

                    case _ =>

                      oq.userUpdates
                        .homeAddress(uid)
                        .update((
                          Some(AddressInfo(
                            city = if (s.city.isEmpty) None else s.city.set.get,
                            country = if (s.country.isEmpty) None else s.country.set.get,
                            postalCode = if (s.postalCode.isEmpty) None else s.postalCode.set.get,
                            streetAddress = if (s.streetAddress.isEmpty) None else s.streetAddress.set.get)),
                          currentTimestamp,
                          updatedBy)) == 1

                  }

                case _ =>
                  oq.userUpdates.homeAddress(uid).update(None, currentTimestamp, updatedBy) == 1
              })

              val _10 = _9 && (spec.workAddress foreach {
                case Some(s) =>

                  sWorkAddress match {
                    case Some(AddressInfo(curCity, curCountry, curPostalCode, curStreetAddress)) =>

                      oq.userUpdates
                        .workAddress(uid)
                        .update((
                          Some(AddressInfo(
                            city = if (s.city.isEmpty) curCity else s.city.set.get,
                            country = if (s.country.isEmpty) curCountry else s.country.set.get,
                            postalCode = if (s.postalCode.isEmpty) curPostalCode else s.postalCode.set.get,
                            streetAddress = if (s.streetAddress.isEmpty) curStreetAddress else s.streetAddress.set.get)),
                          currentTimestamp,
                          updatedBy)) == 1

                    case _ =>

                      oq.userUpdates
                        .workAddress(uid)
                        .update((
                          Some(AddressInfo(
                            city = if (s.city.isEmpty) None else s.city.set.get,
                            country = if (s.country.isEmpty) None else s.country.set.get,
                            postalCode = if (s.postalCode.isEmpty) None else s.postalCode.set.get,
                            streetAddress = if (s.streetAddress.isEmpty) None else s.streetAddress.set.get)),
                          currentTimestamp,
                          updatedBy)) == 1
                  }

                case _ =>
                  oq.userUpdates.workAddress(uid).update(None, currentTimestamp, updatedBy) == 1
              })

              val _11 = _10 && (spec.accessRights map { rights =>

                try {

                  UsersAccessRights
                    .filter(_.userId === uid)
                    .delete

                  UsersAccessRights ++= (rights map (o => UserAccessRight(uid, uuid(o), grantedBy = updatedBy)))

                  true
                } catch {
                  case scala.util.control.NonFatal(_) => false
                }
              } getOrElse true)

              _11 && {

                val qContacts = oq.userUpdates.contacts(uid)

                spec.contacts foreach {

                  case Some(s) =>

                    import utils.If

                    sContacts match {

                      case Some(Contacts(curMobiles, curHome, curWork)) =>

                        val newHome = if (s.home.isEmpty) curHome else if (s.home.set.get eq None) None else Some {
                          val tmp = s.home.set.get.get
                          val empt = curHome.nonEmpty

                          ContactInfo(
                            email = If(tmp.email.set eq None, If(empt, curHome.get.email, None), tmp.email.set.get),
                            fax = If(tmp.fax.set eq None, If(empt, curHome.get.fax, None), tmp.fax.set.get),
                            phoneNumber = If(tmp.phoneNumber.set eq None, If(empt, curHome.get.phoneNumber, None), tmp.phoneNumber.set.get))
                        }

                        val newWork = if (s.work.isEmpty) curWork else if (s.work.set.get eq None) None else Some {
                          val tmp = s.work.set.get.get
                          val empt = curWork.nonEmpty

                          ContactInfo(
                            email = If(tmp.email.set eq None, If(empt, curHome.get.email, None), tmp.email.set.get),
                            fax = If(tmp.fax.set eq None, If(empt, curHome.get.fax, None), tmp.fax.set.get),
                            phoneNumber = If(tmp.phoneNumber.set eq None, If(empt, curHome.get.phoneNumber, None), tmp.phoneNumber.set.get))
                        }

                        val newMobiles = if (s.mobiles.isEmpty) curMobiles else if (s.mobiles.set.get eq None) None else Some {
                          val tmp = s.mobiles.set.get.get
                          val empt = curMobiles.nonEmpty

                          MobileNumbers(
                            mobile1 = If(tmp.mobile1.set eq None, If(empt, curMobiles.get.mobile1, None), tmp.mobile1.set.get),
                            mobile2 = If(tmp.mobile2.set eq None, If(empt, curMobiles.get.mobile2, None), tmp.mobile2.set.get))
                        }

                        (qContacts update ((
                          Some(Contacts(
                            mobiles = newMobiles,
                            home = newHome,
                            work = newWork)),
                          currentTimestamp,
                          updatedBy))) == 1

                      case _ =>

                        val newHome = if (s.home.isEmpty || (s.home.set.get eq None)) None else Some {
                          val tmp = s.home.set.get.get

                          ContactInfo(
                            email = If(tmp.email.set eq None, None, tmp.email.set.get),
                            fax = If(tmp.fax.set eq None, None, tmp.fax.set.get),
                            phoneNumber = If(tmp.phoneNumber.set eq None, None, tmp.phoneNumber.set.get))
                        }

                        val newWork = if (s.work.isEmpty || (s.work.set.get eq None)) None else Some {
                          val tmp = s.work.set.get.get

                          ContactInfo(
                            email = If(tmp.email.set eq None, None, tmp.email.set.get),
                            fax = If(tmp.fax.set eq None, None, tmp.fax.set.get),
                            phoneNumber = If(tmp.phoneNumber.set eq None, None, tmp.phoneNumber.set.get))
                        }

                        val newMobiles = if (s.mobiles.isEmpty || (s.mobiles.set.get eq None)) None else Some {
                          val tmp = s.mobiles.set.get.get

                          MobileNumbers(
                            mobile1 = If(tmp.mobile1.set eq None, None, tmp.mobile1.set.get),
                            mobile2 = If(tmp.mobile2.set eq None, None, tmp.mobile2.set.get))
                        }

                        (qContacts update ((
                          Some(Contacts(
                            mobiles = newMobiles,
                            home = newHome,
                            work = newWork)),
                          currentTimestamp,
                          updatedBy))) == 1
                    }

                  case _ =>

                    (qContacts update ((None, currentTimestamp, updatedBy))) == 1
                }
              }
          }

        case _ => false
      }
    }

    def primaryEmailExists(primaryEmail: String) = {
      import Database.dynamicSession

      val primaryEmailExists = oq.primaryEmailExists(primaryEmail.toLowerCase)

      db.withDynSession {
        primaryEmailExists.run
      }
    }

    def labelUser(userId: String, labels: Set[String]) = {
      val id = uuid(userId)

      labels.par foreach {
        label =>

          val labelInDB = oq.labelled(label)

          val result = db.withSession { implicit session =>
            labelInDB.firstOption
          }

          result match {
            case Some(Label(name, _)) => db.withTransaction { implicit s => UsersLabels += UserLabel(id, name) }
            case _                    => {}
          }
      }
    }

    def unLabelUser(userId: String, labels: Set[String]) =
      db.withTransaction { implicit session =>
        val userLabel = UsersLabels filter (uL => (uL.userId === uuid(userId)) && (uL.label inSet labels))

        userLabel.delete
      }

    def getUserLabels(userId: String) = {
      import Database.dynamicSession

      val userLabels = oq.userLabels(uuid(userId))

      db.withDynSession {
        userLabels.list
      }
    }

    def createPasswdResetReq(username: String) = db.withTransaction { implicit session =>
      val key = utils.Crypto.generateSecureToken

      val user = oq.forActivationKey(username)

      if (user.update(Some(utils.genPasswd(key)), true /* Suspend account */ ) == 1) mailer.sendPasswordResetEmail(username, key)
      else throw new Exception("createPasswdResetReq: can not update user_activation_key")
    }

    def checkActivationReq(username: String, ky: String) = {
      val user = oq.forActivationKey(username)

      val result = db.withSession { implicit session =>
        user.firstOption
      }

      result match {
        case Some((Some(hashed), _)) => passwords verify (ky, hashed)
        case _                       => false
      }
    }

    def resetPasswd(username: String, ky: String, newPasswd: String) = db.withTransaction { implicit session =>
      val user = oq.forActivation(username)

      val result = db.withSession { implicit session =>
        user.firstOption
      }

      result match {
        case Some((Some(hashed), _, _)) if passwords verify (ky, hashed) =>

          utils.If(
            user.update(None, passwords crypt newPasswd, false /* Enable account */ ) == 1,
            { mailer.sendPasswordChangedNotice(username); true },
            false)

        case _ => false
      }
    }

    def getPage(userId: String) = {
      import scala.slick.jdbc.{ StaticQuery => T }
      import T.interpolation

      val page = sql""" SELECT (row_number() OVER () - 1) / $MaxResults as page FROM users WHERE id = CAST($userId AS UUID); """.as[Int]

      db.withSession { implicit session =>
        page.firstOption getOrElse 0
      }
    }
  }
}