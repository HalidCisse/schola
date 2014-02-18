package schola
package oadmin

package impl

trait UserServicesComponentImpl extends UserServicesComponent {
  this: UserServicesRepoComponent =>

  class UserServicesImpl extends UserServices {

    def getUsersStats = userServiceRepo.getUsersStats

    def getUsers(page: Int) = userServiceRepo.getUsers(page)

    def getUser(id: String) = userServiceRepo.getUser(id)

    def removeUser(id: String) = userServiceRepo.removeUser(id)

    def removeUsers(users: Set[String]) = userServiceRepo.removeUsers(users)

    def getPurgedUsers = userServiceRepo.getPurgedUsers

    def purgeUsers(users: Set[String]) = userServiceRepo.purgeUsers(users)

    def undeleteUsers(users: Set[String]) = userServiceRepo.undeleteUsers(users)

    def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Option[domain.Contacts], changePasswordAtNextLogin: Boolean) = userServiceRepo.saveUser(username, password, givenName, familyName, createdBy, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin)

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
  this: AvatarServicesComponent with MailingComponent =>

  import schema._
  import domain._
  import Q._

  private[this] val log = Logger("oadmin.userServiceRepoImpl")

  protected val db: Database

  protected val userServiceRepo = new UserServicesRepoImpl

  class UserServicesRepoImpl extends UserServicesRepo {

    private[this] object oq {

      object Convs {
        import scala.slick.jdbc._

        implicit object SetUUIDOption extends SetParameter[Option[java.util.UUID]] { def apply(v: Option[java.util.UUID], pp: PositionedParameters) { pp.setObjectOption(v, java.sql.Types.OTHER) } }

        implicit object GetUUIDOption extends GetResult[Option[java.util.UUID]] { def apply(rs: PositionedResult) = rs.nextStringOption map (uuid) }
        implicit object GetGender extends GetResult[domain.Gender] { def apply(rs: PositionedResult) = domain.Gender.withName(rs.nextString()) }
        implicit object GetAddressOption extends GetResult[Option[domain.AddressInfo]] { def apply(rs: PositionedResult) = conversions.jdbc.addressInfoTypeMapper.nextOption(rs) }
        implicit object GetContacts extends GetResult[Option[domain.Contacts]] { def apply(rs: PositionedResult) = conversions.jdbc.contactsTypeMapper.nextOption(rs) }
      }

      def users(page: Int) = {
        import scala.slick.jdbc.{ StaticQuery => T }
        import T.interpolation

        import Convs._

        type Result = (Option[java.util.UUID], String, String, String, Long, Option[java.util.UUID], Option[Long], Option[Long], Option[java.util.UUID], domain.Gender, Option[domain.AddressInfo], Option[domain.AddressInfo], Option[domain.Contacts], Option[String], Boolean, Option[String])

        sql"""select
                x2.x3, x2.x4, x2.x5, x2.x6, x2.x7, x2.x8, x2.x9, x2.x10, x2.x11, x2.x12, x2.x13, x2.x14, x2.x15, x2.x16, x2.x17, x3.label
              from (
                  select x18."id" as x3,
                    x18."primary_email" as x4,
                    x18."given_name" as x5,
                    x18."family_name" as x6,
                    x18."created_at" as x7,
                    x18."created_by" as x8,
                    x18."last_login_time" as x9,
                    x18."last_modified_at" as x10,
                    x18."last_modified_by" as x11,
                    x18."gender" as x12,
                    x18."home_address" as x13,
                    x18."work_address" as x14,
                    x18."contacts" as x15,
                    x18."avatar" as x16,
                    x18."change_password_at_next_login" as x17
                  from "users" x18 where not x18."_deleted" and x18."id" <> ${U.SuperUser.id}
                  order by last_modified_at desc nulls last, created_at desc
                  limit $MaxResults offset ${page * MaxResults}
                ) x2 left join users_labels x3 on (x2.x3 = x3.user_id)""".as[Result]
      }

      val trashedUsers = Compiled(for {
        u <- Users if u._deleted
      } yield (
        u.id,
        u.primaryEmail,
        u.givenName,
        u.familyName,
        u.createdAt,
        u.createdBy,
        u.lastLoginTime,
        u.lastModifiedAt,
        u.lastModifiedBy,
        u.gender,
        u.homeAddress,
        u.workAddress,
        u.contacts,
        u.avatar,
        u.changePasswordAtNextLogin))

      val userById = {

        def getUser(id: Column[java.util.UUID]) =
          for {
            u <- Users if !u._deleted && (u.id is id)
          } yield (
            u.id,
            u.primaryEmail,
            u.givenName,
            u.familyName,
            u.createdAt,
            u.createdBy,
            u.lastLoginTime,
            u.lastModifiedAt,
            u.lastModifiedBy,
            u.gender,
            u.homeAddress,
            u.workAddress,
            u.contacts,
            u.avatar,
            u.changePasswordAtNextLogin)

        Compiled(getUser _)
      }

      val primaryEmailExists = {
        def getPrimaryEmail(primaryEmail: Column[String]) =
          Query(Users where (_.primaryEmail.toLowerCase is primaryEmail) exists)

        Compiled(getPrimaryEmail _)
      }

      val userLabels = {
        def getUserLabels(userId: Column[java.util.UUID]) =
          UsersLabels where (_.userId is userId) map(_.label)

        Compiled(getUserLabels _)
      }

      val labelled = {
        def getLabel(label: Column[String]) =
          Labels where (_.name is label)

        Compiled(getLabel _)
      }

      val forActivationKey = {
        def getActivationKey(username: Column[String]) =
          Users where (_.primaryEmail is username) map (o => (o.activationKey, o.suspended))

        Compiled(getActivationKey _)
      }

      val forActivation = {
        def getActivation(username: Column[String]) =
          Users where (_.primaryEmail is username) map (o => (o.activationKey, o.password, o.suspended))

        Compiled(getActivation _)
      }

      val userUpdates = {

        def forUserPasswd(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.primaryEmail, o.password))

        def forPrimaryEmail(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.primaryEmail, o.lastModifiedAt, o.lastModifiedBy))

        def forPasswd(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.password, o.changePasswordAtNextLogin, o.lastModifiedAt, o.lastModifiedBy))

        def forFN(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.givenName, o.lastModifiedAt, o.lastModifiedBy))

        def forLN(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.familyName, o.lastModifiedAt, o.lastModifiedBy))

        def forGender(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.gender, o.lastModifiedAt, o.lastModifiedBy))

        def forHomeAddress(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.homeAddress, o.lastModifiedAt, o.lastModifiedBy))

        def forWorkAddress(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.workAddress, o.lastModifiedAt, o.lastModifiedBy))

        def forAvatar(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.avatar, o.lastModifiedAt, o.lastModifiedBy))

        def forContacts(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (o => (o.contacts, o.lastModifiedAt, o.lastModifiedBy))

        new {
          val userPasswd = Compiled(forUserPasswd _)
          val primaryEmail = Compiled(forPrimaryEmail _)
          val password = Compiled(forPasswd _)
          val givenName = Compiled(forFN _)
          val familyName = Compiled(forLN _)
          val gender = Compiled(forGender _)
          val homeAddress = Compiled(forHomeAddress _)
          val workAddress = Compiled(forWorkAddress _)
          val avatar = Compiled(forAvatar _)
          val contacts = Compiled(forContacts _)
        }
      }
    }

    def getUsersStats = {
      import Database.dynamicSession

      val num = Query(Users where(_.id isNot U.SuperUser.id) map (_.id) length)

      val result = db.withDynSession {
        num.firstOption
      }

      UsersStats(result getOrElse 0)
    }

    def getUsers(page: Int) = {
      import Database.dynamicSession

      val users = oq.users(page)

      val result = db.withDynSession {
        users.list
      }

      result.groupBy(_._1).flatMap {
        case (id, user :: rest) => User(user._2, None, user._3, user._4, user._5, user._6, user._7, user._8, user._9, user._10, user._11, user._12, user._13, user._14, changePasswordAtNextLogin = user._15, id = user._1, labels = if (user._16.isDefined) user._16.get :: rest.filter(_._16.isDefined).map(_._16.get) else rest.filter(_._16.isDefined).map(_._16.get)) :: Nil
      }.toList
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val user = oq.userById(uuid(id))

      val result = db.withDynSession {
        user.firstOption
      }

      result map {
        case (sId, primaryEmail, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin) =>
          User(primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(sId), labels = getUserLabels(sId.toString))
      }
    }

    def removeUser(id: String) = {
      import Database.dynamicSession

      val q = Users.forDeletion(uuid(id))

      db.withDynSession {
        q.update(true) == 1
      }
    }

    def removeUsers(users: Set[String]) {
      val q = for { u <- Users if (u.id isNot U.SuperUser.id) && (u.id inSet (users map uuid)) } yield u._deleted

      db.withTransaction { implicit sesssion =>
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
        case (id, primaryEmail, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin) =>
          User(primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(id))
      }
    }

    def purgeUsers(users: Set[String]) = {
      val q = for { u <- Users if (u.id isNot U.SuperUser.id) && (u.id inSet (users map uuid)) } yield u

      users foreach {
        id =>
          if (U.SuperUser.id exists (_.toString != id))
            avatarServices.purgeAvatarForUser(id)
      }

      db.withTransaction { implicit sesssion =>
        q.delete
      }
    }

    def undeleteUsers(users: Set[String]) = {
      val deleted = for { u <- Users if u._deleted && (u.id inSet (users map uuid)) } yield u._deleted

      db.withTransaction { implicit sesssion =>
        deleted.update(false)
      }
    }

    def saveUser(primaryEmail: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Option[domain.Contacts], changePasswordAtNextLogin: Boolean) =
      db.withTransaction { implicit session =>
        import scala.util.control.Exception.allCatch

        val currentTimestamp = System.currentTimeMillis

        allCatch.opt {

          try

            Users insert (
              primaryEmail,
              passwords crypt password,
              givenName,
              familyName,
              currentTimestamp,
              createdBy map uuid,
              Some(currentTimestamp),
              createdBy map uuid,
              gender,
              homeAddress,
              workAddress,
              contacts,
              changePasswordAtNextLogin)

          catch {
            case ex: Throwable =>

              log.info(s"[saveUser failed with $ex]")
              throw ex
          } finally {
            mailer.sendWelcomeEmail(primaryEmail, password)
          }
        }
      }

    def updateUser(id: String, spec: UserSpec) = {
      val uid = uuid(id)
      val userPasswd = oq.userUpdates.userPasswd(uid)

      db.withSession {
        implicit session => userPasswd.firstOption
      } match {

        case Some((sUsername, sPassword)) =>
          db.withTransaction {
            implicit session =>

              val currentTimestamp = Some(System.currentTimeMillis)

              val _1 = spec.primaryEmail map {
                primaryEmail =>
                  oq.userUpdates.primaryEmail(uid).update(primaryEmail, currentTimestamp, Some(uid)) == 1
              } getOrElse true

              val _2 = _1 && (spec.password map {
                password =>
                  spec.oldPassword.nonEmpty &&
                    (passwords verify (spec.oldPassword.get, sPassword)) &&
                    (oq.userUpdates.password(uid).update(passwords crypt password, false, currentTimestamp, Some(uid)) == 1) &&
                    { mailer.sendPasswordChangedNotice(sUsername); true }
              } getOrElse true)

              val _3 = _2 && (spec.givenName map {
                givenName =>
                  oq.userUpdates.givenName(uid).update(givenName, currentTimestamp, Some(uid)) == 1
              } getOrElse true)

              val _4 = _3 && (spec.familyName map {
                familyName =>
                  oq.userUpdates.familyName(uid).update(familyName, currentTimestamp, Some(uid)) == 1
              } getOrElse true)

              val _5 = _4 && (spec.gender map {
                gender =>
                  oq.userUpdates.gender(uid).update(gender, currentTimestamp, Some(uid)) == 1
              } getOrElse true)

              val _6 = _5 && (spec.homeAddress foreach {
                case Some(homeAddress) =>

                  (homeAddress.city foreach {
                    city => 
                      oq.userUpdates.homeAddress(uid).update((Some(AddressInfo(city = city)), currentTimestamp, Some(uid))) == 1
                  }) && (homeAddress.country foreach {
                    country => 
                      oq.userUpdates.homeAddress(uid).update((Some(AddressInfo(country = country)), currentTimestamp, Some(uid))) == 1
                  }) && (homeAddress.postalCode foreach {
                    postalCode => 
                      oq.userUpdates.homeAddress(uid).update((Some(AddressInfo(postalCode = postalCode)), currentTimestamp, Some(uid))) == 1
                  }) && (homeAddress.streetAddress foreach {
                    streetAddress => 
                      oq.userUpdates.homeAddress(uid).update((Some(AddressInfo(streetAddress = streetAddress)), currentTimestamp, Some(uid))) == 1
                  })

                case _ =>
                  oq.userUpdates.homeAddress(uid).update(None, currentTimestamp, Some(uid)) == 1
              })

              val _7 = _6 && (spec.workAddress foreach {
                case Some(workAddress) =>

                  (workAddress.city foreach {
                    city => 
                      oq.userUpdates.workAddress(uid).update(Some(AddressInfo(city = city)), currentTimestamp, Some(uid)) == 1
                  }) && (workAddress.country foreach {
                    country => 
                      oq.userUpdates.workAddress(uid).update(Some(AddressInfo(country = country)), currentTimestamp, Some(uid)) == 1
                  }) && (workAddress.postalCode foreach {
                    postalCode => 
                      oq.userUpdates.workAddress(uid).update(Some(AddressInfo(postalCode = postalCode)), currentTimestamp, Some(uid)) == 1
                  }) && (workAddress.streetAddress foreach {
                    streetAddress => 
                      oq.userUpdates.workAddress(uid).update(Some(AddressInfo(streetAddress = streetAddress)), currentTimestamp, Some(uid)) == 1
                  })

                case _ =>
                  oq.userUpdates.workAddress(uid).update(None, currentTimestamp, Some(uid)) == 1
              })

              val _8 = _7 && (spec.avatar foreach {
                case avatarId =>
                  oq.userUpdates.avatar(uid).update(avatarId, currentTimestamp, Some(uid)) == 1
              })

              _8 && {

                  val qContacts = oq.userUpdates.contacts(uid)

                  spec.contacts foreach {                    

                    case Some(s) =>

                      (s.mobiles foreach {
                        case Some(mobiles) =>
                          (mobiles.mobile1 foreach {
                            case m1 @ Some(_) =>
                              (qContacts update ((
                                Some(Contacts(
                                  mobiles = Some(MobileNumbers(mobile1 = m1)))),
                                currentTimestamp,
                                Some(uid)))) == 1
                            case _ =>
                              (qContacts update ((
                                Some(Contacts(
                                  mobiles = Some(MobileNumbers(mobile1 = None)))),
                                currentTimestamp,
                                Some(uid)))) == 1
                          }) && (mobiles.mobile2 foreach {
                            case m2 @ Some(_) =>
                              (qContacts update ((
                                Some(Contacts(
                                  mobiles = Some(MobileNumbers(mobile2 = m2)))),
                                currentTimestamp,
                                Some(uid)))) == 1
                            case _ =>

                              (qContacts update ((
                                Some(Contacts(
                                  mobiles = Some(MobileNumbers(mobile2 = None)))),
                                currentTimestamp,
                                Some(uid)))) == 1
                          })
                        case _ =>

                          (qContacts update ((
                            Some(Contacts(
                              mobiles = None)),
                            currentTimestamp,
                            Some(uid)))) == 1

                        }) && (s.home foreach {
                            case Some(home) =>

                              (home.email foreach {
                                email =>

                                  (qContacts update ((
                                    Some(Contacts(
                                      home = Some(ContactInfo(email = email)))),
                                    currentTimestamp,
                                    Some(uid)))) == 1

                              }) && (home.phoneNumber foreach {
                                phoneNumber =>

                                  (qContacts update ((
                                    Some(Contacts(
                                      home = Some(ContactInfo(phoneNumber = phoneNumber)))),
                                    currentTimestamp,
                                    Some(uid)))) == 1

                              }) && (home.fax foreach {
                                fax =>

                                  (qContacts update ((
                                    Some(Contacts(
                                      home = Some(ContactInfo(fax = fax)))),
                                    currentTimestamp,
                                    Some(uid)))) == 1

                              })

                            case _ =>

                              (qContacts update ((
                                Some(Contacts(
                                  home = None)),
                                currentTimestamp,
                                Some(uid)))) == 1

                      }) && (s.work foreach {
                          case Some(work) =>
                            (work.email foreach {
                              email =>

                                (qContacts update ((
                                  Some(Contacts(
                                    work = Some(ContactInfo(email = email)))),
                                  currentTimestamp,
                                  Some(uid)))) == 1

                            }) && (work.phoneNumber foreach {
                              phoneNumber =>

                                (qContacts update ((
                                  Some(Contacts(
                                    work = Some(ContactInfo(phoneNumber = phoneNumber)))),
                                  currentTimestamp,
                                  Some(uid)))) == 1

                            }) && (work.fax foreach {
                              fax =>

                                (qContacts update ((
                                  Some(Contacts(
                                    work = Some(ContactInfo(fax = fax)))),
                                  currentTimestamp,
                                  Some(uid)))) == 1

                            })
                          case _ =>

                            (qContacts update ((
                              Some(Contacts(
                                work = None)),
                              currentTimestamp,
                              Some(uid)))) == 1
                            
                            })

                    case _ =>

                      (qContacts update ((None, currentTimestamp, Some(uid)))) == 1
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
        primaryEmailExists.firstOption
      } getOrElse false
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
        val userLabel = UsersLabels where (uL => (uL.userId is uuid(userId)) && (uL.label inSet labels))

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