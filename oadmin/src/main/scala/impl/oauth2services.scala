package schola
package oadmin

package impl

import org.clapper.avsl.Logger
import schola.oadmin.impl.CacheActor.{FindValue, PurgeValue}

trait OAuthServicesComponentImpl extends OAuthServicesComponent {
  self: OAuthServicesRepoComponent =>

  val oauthService = new OAuthServicesImpl

  class OAuthServicesImpl extends OAuthServices {

    def getUsers = oauthServiceRepo.getUsers

    def getUser(id: String) = oauthServiceRepo.getUser(id)

    def removeUser(id: String) = oauthServiceRepo.removeUser(id)

    def getPurgedUsers = oauthServiceRepo.getPurgedUsers

    def purgeUsers(users: Set[String]) = oauthServiceRepo.purgeUsers(users)
    
    def undeleteUsers(users: Set[String]) = oauthServiceRepo.undeleteUsers(users)

    def getTokenSecret(accessToken: String) = oauthServiceRepo.getTokenSecret(accessToken)

    def getRefreshToken(refreshToken: String) = oauthServiceRepo.getRefreshToken(refreshToken)

    def exchangeRefreshToken(refreshToken: String) = oauthServiceRepo.exchangeRefreshToken(refreshToken)

    def getUserTokens(userId: String) = oauthServiceRepo.getUserTokens(userId)

    def getUserSession(params: Map[String, String]) = oauthServiceRepo.getUserSession(params)

    def revokeToken(accessToken: String) = oauthServiceRepo.revokeToken(accessToken)

    def getClient(id: String, secret: String) = oauthServiceRepo.getClient(id, secret)

    def authUser(username: String, password: String) = oauthServiceRepo.authUser(username, password)

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      oauthServiceRepo.saveToken(accessToken, refreshToken, macKey, uA, clientId, redirectUri, userId, expiresIn, refreshExpiresIn, scopes)

    def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: domain.Contacts, changePasswordAtNextLogin: Boolean) = oauthServiceRepo.saveUser(username, password, givenName, familyName, createdBy, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin)

    def updateUser(id: String, spec: utils.UserSpec) = oauthServiceRepo.updateUser(id, spec)

    def getAvatar(id: String) = oauthServiceRepo.getAvatar(id)

    def primaryEmailExists(primaryEmail: String) = oauthServiceRepo.primaryEmailExists(primaryEmail)
  }
}

trait OAuthServicesRepoComponentImpl extends OAuthServicesRepoComponent {
  self: OAuthServicesComponent with AccessControlServicesComponent =>

  import Q._

  val log = Logger("oadmin.oauthserviceRepoImpl")

  protected val db: Database

  protected val oauthServiceRepo = new OAuthServicesRepoImpl

  private object q {
    import schema._

    val users = Compiled(for {
      u <- Users if ! u._deleted
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
          u <- Users if ! u._deleted && (u.id is id)
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

    val tokenSecret = {
      def getTokenSecret(accessToken: Column[String]) =
        for {
          t <- OAuthTokens if t.accessToken is accessToken
        } yield t.macKey

      Compiled(getTokenSecret _)
    }

    val lastAccessTime = Compiled(OAuthTokens map(_.lastAccessTime))

    val refreshToken = {
      def getRefreshToken(mRefreshToken: Column[String]) =
        for {
          (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.refreshToken is mRefreshToken
        } yield (
          t.accessToken,
          t.clientId,
          c.redirectUri,
          t.userId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)

      Compiled(getRefreshToken _)
    }

    val forExchange = {
      def getRefreshToken(refreshToken: Column[String]) =
        for {
          (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.refreshToken is refreshToken
        } yield (
          t.accessToken,
          t.clientId,
          c.redirectUri,
          t.userId,
          t.uA,
          t.refreshToken,
          t.createdAt,
          t.expiresIn,
          t.refreshExpiresIn,
          t.scopes)

      Compiled(getRefreshToken _)
    }

    val bearerToken = {
      def getAccessToken(accessToken: Column[String]) = 
        OAuthTokens where (_.accessToken is accessToken)

      Compiled(getAccessToken _)
    }

    val accessToken = {
      def getAccessToken(mAccessToken : Column[String]) =
        for {
          (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is mAccessToken
        } yield (
          t.accessToken,
          t.clientId,
          c.redirectUri,
          t.userId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)

      Compiled(getAccessToken _)
    }

    val userTokens = {
      def getUserTokens(userId: Column[java.util.UUID]) =
        for {
          t <- OAuthTokens if (t.userId is userId)
        } yield (
          t.accessToken,
          t.clientId,
          t.redirectUri,
          t.userId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)

      Compiled(getUserTokens _)
    }

    val client = {
      def getClient(id: Column[String], secret: Column[String]) =
        for {
          c <- OAuthClients if (c.id is id) && (c.secret is secret)
        } yield (
          c.id,
          c.secret,
          c.redirectUri)

      Compiled(getClient _)
    }

    val session = {
      def getUserSession(bearerToken: Column[String], userAgent: Column[String]) =
        for {
          (u, t) <- Users leftJoin OAuthTokens on(_.id is _.userId)
          if /*(t.userId is java.util.UUID.fromString(userId)) &&
                       */(t.accessToken is bearerToken) &&
            (t.uA is userAgent)
        } yield (u, (
          t.accessToken,
          t.clientId,
          t.refreshToken,
          t.macKey,
          t.uA,
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes))

      Compiled(getUserSession _)
    }

    val auth = {
      def getUser(username: Column[String]) =
        for {
          u <- Users if !u._deleted && !u.suspended && (u.primaryEmail is username)
        } yield (u.id, u.password)

      Compiled(getUser _)
    }

    val forLastLoginTime = {
      def getLastLoginTime(id: Column[java.util.UUID]) =
        Users where(_.id is id) map(_.lastLoginTime)

      Compiled(getLastLoginTime _)
    }

    val primaryEmailExists = {
      def getPrimaryEmail(primaryEmail: Column[String]) =
        for {
          u <- Users if u.primaryEmail.toLowerCase is primaryEmail
        } yield true

      Compiled(getPrimaryEmail _)
    }

    val userUpdates = {

      def forPasswdAndContacts(id: Column[java.util.UUID]) =
        Users where(_.id is id) map(o => (o.password, o.contacts))
      
      def forPrimaryEmail(id: Column[java.util.UUID]) =
        Users where(_.id is id) map(o => (o.primaryEmail, o.lastModifiedAt, o.lastModifiedBy))

      def forPasswd(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.password, o.changePasswordAtNextLogin, o.lastModifiedAt, o.lastModifiedBy))

      def forFN(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.givenName, o.lastModifiedAt, o.lastModifiedBy))

      def forLN(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.familyName, o.lastModifiedAt, o.lastModifiedBy))

      def forGender(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.gender, o.lastModifiedAt, o.lastModifiedBy))

      def forHomeAddress(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.homeAddress, o.lastModifiedAt, o.lastModifiedBy))

      def forWorkAddress(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.workAddress, o.lastModifiedAt, o.lastModifiedBy))

      def forAvatar(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.avatar, o.lastModifiedAt, o.lastModifiedBy))

      def forContacts(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.contacts, o.lastModifiedAt, o.lastModifiedBy))

      new {
        val passwdAndContacts = Compiled(forPasswdAndContacts _)
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

  class OAuthServicesRepoImpl extends OAuthServicesRepo {
    import oauthService._

    import schema._
    import domain._

    def getUsers = {
      import Database.dynamicSession

      val users = q.users

      val result = db.withDynSession {
        users.list
      }

      result map {
        case (id, primaryEmail, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin) =>
          User(primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(id))
      }
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val user = q.userById(java.util.UUID.fromString(id))

      val result = db.withDynSession {
        user.firstOption
      }

      result map {
        case (sId, primaryEmail, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin) =>
          User(primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(sId))
      }
    }

    def removeUser(id: String) = {
      import Database.dynamicSession

      val q = Users.forDeletion(java.util.UUID.fromString(id))

      db.withDynSession {
        q.update(true) == 1
      }
    }

    def getPurgedUsers = {
      import Database.dynamicSession

      val trash = q.trashedUsers

      val result = db.withDynSession {
        trash.list
      }

      result map {
        case (id, primaryEmail, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin) =>
          User(primaryEmail, None, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, changePasswordAtNextLogin = changePasswordAtNextLogin, id = Some(id))
      }
    }

    def purgeUsers(users: Set[String]) = {
      val q = for { u <- Users if u.id inSet (users map java.util.UUID.fromString) } yield u

      users foreach(id => avatars ! utils.Avatars.Purge(id))

      db.withTransaction { implicit sesssion =>
        q.delete
      }
    }

    def undeleteUsers(users: Set[String]) = {
      val q = for { u <- Users if u._deleted && (u.id inSet (users map java.util.UUID.fromString)) } yield u._deleted
      
      db.withTransaction { implicit sesssion =>
        q.update(false)
      }
    }

    def getTokenSecret(accessToken: String) = {
      import Database.dynamicSession

      val token = q.tokenSecret(accessToken)

      db.withDynSession {
        token.firstOption
      }
    }

    def getRefreshToken(refreshToken: String) = {

      val token = q.refreshToken(refreshToken)

      val result = db.withSession { implicit session =>
        token.firstOption
      }

      result flatMap {
        case (accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, sScopes) =>

          def isExpired = refreshExpires exists (_ * 1000 + createdAt < System.currentTimeMillis)

          if(isExpired) {
            db withTransaction { implicit session =>
              if(q.bearerToken(accessToken).delete != 1)
                throw new Exception("getRefreshToken: can't delete expired refresh token")
              None
            }
          }
          else Some(OAuthToken(accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, scopes = sScopes))
      }
    }

    def exchangeRefreshToken(refreshToken: String) = db.withTransaction { implicit session =>

      val token = q.forExchange(refreshToken)

      token.firstOption flatMap {
        case (aAccessToken, clientId, redirectUri, userId, uA, Some(aRefreshToken), issuedTime, expiresIn, refreshExpiresIn, aScopes) if refreshExpiresIn map(issuedTime + 1000 * _ > System.currentTimeMillis) getOrElse true => //aRefreshToken exists

          def generateToken = utils.Crypto.generateToken() // utils.SHA3Utils digest s"$clientId:$userId:${System.nanoTime}"
          def generateRefreshToken(accessToken: String) = utils.Crypto.generateToken() // utils.SHA3Utils digest s"$accessToken:$userId:${System.nanoTime}"
          def generateMacKey = utils.genPasswd(s"$userId:${System.nanoTime}")

          val accessToken = generateToken

          val currentTimestamp = System.currentTimeMillis

          if((OAuthTokens.forInsert +=
            (accessToken, clientId, redirectUri, userId, Some(generateRefreshToken(accessToken)), generateMacKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", aScopes)) != 1)
              throw new Exception("could not refresh Token")

          val newToken = q.accessToken(accessToken)

          newToken.firstOption map {
            case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, dScopes) =>

              if(q.bearerToken(aAccessToken).delete != 1)
                throw new Exception("couldn't delete old token")

              OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, scopes = dScopes)
          }

        case _ => None
      }
    }

    def revokeToken(accessToken: String) =
      db.withTransaction { implicit session =>
        val token = q.tokenSecret(accessToken)
        token.delete == 1
      }

    def getUserTokens(userId: String) = {
      import Database.dynamicSession

      val userTokens = q.userTokens(java.util.UUID.fromString(userId))

      val result = db.withDynSession {
        userTokens.list
      }

      result map {
        case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
          OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
      }
    }

    def getUserSession(params: Map[String, String]) =
      db.withTransaction{ implicit s =>
  //      val userId = params("userId")
        val bearerToken = params("bearerToken")
        val userAgent = params("userAgent")

        val session = q.session(bearerToken, userAgent)

        session.firstOption map {
          case (sUser, (sAccessToken, sClientId, sRefreshToken, sMacKey, sUA, sExpiresIn, sRefreshExpiresIn, sCreatedAt, sLastAccessTime, sScopes)) =>

            import scala.util.control.Exception.allCatch

            allCatch.opt {
              q.lastAccessTime update(System.currentTimeMillis)
            } // Touch session

            val userId = sUser.id map(_.toString) get

            Session(
              sAccessToken,
              sMacKey,
              sClientId,
              sCreatedAt,
              sExpiresIn,
              sRefreshExpiresIn,
              sRefreshToken,
              sLastAccessTime,
              user = sUser copy(password = None),
              userAgent = sUA,
              hasRole = Map(accessControlService.getRoles map(r => (r.name, accessControlService.userHasRole(userId, r.name))) : _*) withDefaultValue(false),
              hasPermission = {
                val userPermissions = accessControlService.getUserPermissions(userId) // TODO: is this dependency safe
                Map(accessControlService.getPermissions map(p => (p.name, userPermissions contains p.name)) : _*) withDefaultValue(false)
              },
              scopes = sScopes
            )
        }
      }

    def getClient(id: String, secret: String) = {
      import Database.dynamicSession

      val client = q.client(id, secret)

      val result = db.withDynSession {
        client.firstOption
      }

      result map {
        case (cId, cSecret, redirectUri) =>
          OAuthClient(cId, cSecret, redirectUri)
      }
    }

    def authUser(username: String, password: String) = {
      val user = q.auth(username)

      val result = db.withSession { implicit session =>
        user.firstOption
      }

      result collect {
        case (id, sPasswd) if passwords verify(password, sPasswd) =>

          db.withTransaction { implicit session =>
            q.forLastLoginTime(id) update(Some(System.currentTimeMillis))
          }

          id.toString
      }
    }

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) = {
      val currentTimestamp = System.currentTimeMillis

      if(
        db.withTransaction { implicit session =>
          (OAuthTokens.forInsert += (accessToken, clientId, redirectUri, java.util.UUID.fromString(userId), refreshToken, macKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", scopes)) != 1
        }) throw new IllegalStateException("can't save token")

      val token = q.accessToken(accessToken)

      val result = db.withSession { implicit session =>
        token.firstOption
      }

      result map {
        case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
          OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
      }
    }

    def saveUser(primaryEmail: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: domain.Contacts, changePasswordAtNextLogin: Boolean) =
      db.withTransaction { implicit session =>
        val currentTimestamp = System.currentTimeMillis

        scala.util.control.Exception.allCatch.opt {

          Users insert(
            primaryEmail, passwords crypt password, givenName, familyName, currentTimestamp, createdBy map java.util.UUID.fromString, Some(currentTimestamp), createdBy map java.util.UUID.fromString, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin)
        }
      }

    def updateUser(id: String, spec: utils.UserSpec) = {
      val uuid = java.util.UUID.fromString(id)
      val passwdAndContacts = q.userUpdates.passwdAndContacts(uuid)

      db.withSession {
        implicit session => passwdAndContacts.firstOption
      } match {

        case Some((sPassword, sContacts)) => if (db.withTransaction {
          implicit session =>

            val currentTimestamp = Some(System.currentTimeMillis)

            val _1 = spec.primaryEmail map {
              primaryEmail =>
                q.userUpdates.primaryEmail(uuid).update(primaryEmail, currentTimestamp, Some(uuid)) == 1
            } getOrElse true

            val _2 = _1 && (spec.password map {
              password =>
                spec.oldPassword.nonEmpty &&
                  (passwords verify(spec.oldPassword.get, sPassword)) &&
                  (q.userUpdates.password(uuid).update(passwords crypt password, false, currentTimestamp, Some(uuid)) == 1)
            } getOrElse true)

            val _3 = _2 && (spec.givenName map {
              givenName =>
                q.userUpdates.givenName(uuid).update(givenName, currentTimestamp, Some(uuid)) == 1
            } getOrElse true)

            val _4 = _3 && (spec.familyName map {
              familyName =>
                q.userUpdates.givenName(uuid).update(familyName, currentTimestamp, Some(uuid)) == 1
            } getOrElse true)

            val _5 = _4 && (spec.gender map {
              gender =>
                q.userUpdates.gender(uuid).update(gender, currentTimestamp, Some(uuid)) == 1
            } getOrElse true)

            val _6 = _5 && (spec.homeAddress foreach {
              case homeAddress =>
                q.userUpdates.homeAddress(uuid).update(homeAddress, currentTimestamp, Some(uuid)) == 1
            })

            val _7 = _6 && (spec.workAddress foreach {
              case workAddress =>
                q.userUpdates.workAddress(uuid).update(workAddress, currentTimestamp, Some(uuid)) == 1
            })

            val _8 = _7 && (spec.avatar foreach {
              case Some((avatar, data)) =>
                avatars ! utils.Avatars.Add(id, avatar, data)
                q.userUpdates.avatar(uuid).update(Some(avatar), currentTimestamp, Some(uuid)) == 1

              case _ =>
                avatars ! utils.Avatars.Purge(id)
                q.userUpdates.avatar(uuid).update(None, currentTimestamp, Some(uuid)) == 1
            })

            _8 && {

              spec.contacts map{
                case s =>

                  val qContacts = q.userUpdates.contacts(uuid)

                  val Contacts(curHome, curWork, MobileNumbers(curMobile1, curMobile2)) = sContacts

                  (qContacts update(
                    Contacts(
                      home = if(s.home.set eq None) curHome else s.home.set.get,
                      work = if(s.work.set eq None) curWork else s.work.set.get,
                      mobiles = MobileNumbers(if(s.mobiles.mobile1.set eq None) curMobile1 else s.mobiles.mobile1.set.get, if(s.mobiles.mobile2.set eq None) curMobile2 else s.mobiles.mobile2.set.get)),
                    currentTimestamp,
                    Some(uuid)
                    )) == 1

              } getOrElse true
            }

        }) getUser(id)

        else None

        case _ => None
      }
    }

    def getAvatar(id: String) = {
      import scala.concurrent.duration._
      import akka.pattern._
      import scala.util.control.Exception.allCatch
      import scala.concurrent.Await

      implicit val timeout = akka.util.Timeout(60 seconds) // needed for `?` below

      val q = (avatars ? utils.Avatars.Get(id)).mapTo[Option[(domain.AvatarInfo, String)]]

      allCatch.opt {
        Await.result(q, timeout.duration)
      } getOrElse None
    }

    def primaryEmailExists(primaryEmail: String) = {
      import Database.dynamicSession

      db.withDynSession {
        q.primaryEmailExists(primaryEmail.toLowerCase).firstOption
      } getOrElse false
    }
  }
}

trait CachingOAuthServicesComponentImpl extends OAuthServicesComponentImpl with AccessControlServicesComponentImpl{
  self: CachingServicesComponent with OAuthServicesRepoComponent with AccessControlServicesRepoComponent with impl.CacheSystemProvider =>

  import caching._

  import scala.concurrent.duration._
  import akka.pattern._
  import akka.util.Timeout
  import scala.util.control.Exception.allCatch
  import scala.concurrent.Await

  override val oauthService = new CachingOAuthServicesImpl

  class CachingOAuthServicesImpl extends OAuthServicesImpl {

    override def getUsers =
      cachingServices.get[List[UserLike]](ManyParams("users")) { super.getUsers } getOrElse Nil

    override def getUser(id: String) =
      cachingServices.get[Option[UserLike]](Params(id)) { super.getUser(id) } flatten

    override def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: domain.Contacts, changePasswordAtNextLogin: Boolean) =
      super.saveUser(username, password, givenName, familyName, createdBy, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) collect{
        case my: UserLike =>
          cachingServices.purge(Params(my.id.get.toString))
          cachingServices.purge(ManyParams("users"))
          my
      }

    override def updateUser(id: String, spec: utils.UserSpec) =
      super.updateUser(id, spec) collect {
        case my: UserLike =>
          cachingServices.purge(Params(my.id.get.toString))
          cachingServices.purge(ManyParams("users"))
          my
      }

    override def removeUser(id: String): Boolean =
      super.removeUser(id) && {
        cachingServices.purge(Params(id))
        cachingServices.purge(ManyParams("users"))
        true
      }

    override def purgeUsers(users: Set[String]) {
      super.purgeUsers(users)
      users foreach(o => cachingServices.purge(Params(o)))
      cachingServices.purge(ManyParams("users"))
    }
  }

  override val accessControlService = new CachingAccessControlServicesImpl

  class CachingAccessControlServicesImpl extends AccessControlServicesImpl {

    override def getRoles =
      cachingServices.get[List[RoleLike]](ManyParams("roles")) { super.getRoles } getOrElse Nil

    override def getRole(roleName: String) =
      cachingServices.get[Option[RoleLike]](Params(roleName)) { super.getRole(roleName) } flatten

    override def saveRole(name: String, parent: Option[String], createdBy: Option[String]) =
      super.saveRole(name, parent, createdBy) collect{
        case my: RoleLike =>
          cachingServices.purge(Params(my.name))
          cachingServices.purge(ManyParams("roles"))
          my
      }

    override def updateRole(name: String, newName: String, parent: Option[String]) =
      super.updateRole(name, newName, parent) && {
        cachingServices.purge(Params(newName))
        cachingServices.purge(ManyParams("roles"))
        true
      }

    override def purgeRoles(roles: Set[String]) {
      super.purgeRoles(roles)
      roles foreach(o => cachingServices.purge(Params(o)))
      cachingServices.purge(ManyParams("roles"))
    }
  }
}

trait CachingServicesComponentImpl extends CachingServicesComponent {
  self: impl.CacheSystemProvider =>

  import scala.concurrent.duration._
  import akka.pattern._
  import akka.util.Timeout
  import scala.util.control.Exception.allCatch
  import scala.concurrent.Await

  protected val cachingServices = new CachingServicesImpl

  protected lazy val cacheActor = cacheSystem.createCacheActor("OAdmin", new impl.OAdminCacheActor(_))

  class CachingServicesImpl extends CachingServices {

    def get[T : scala.reflect.ClassTag](params: impl.CacheActor.Params)(default: => T): Option[T] = {
      implicit val tm = Timeout(60 seconds)

      val q = (cacheActor ? impl.CacheActor.FindValue(params, () => default)).mapTo[Option[T]]

      allCatch.opt {
        Await.result(q, tm.duration)
      } getOrElse None
    }

    def purge(params: impl.CacheActor.Params) {
      cacheActor ! impl.CacheActor.PurgeValue(params)
    }
  }
}