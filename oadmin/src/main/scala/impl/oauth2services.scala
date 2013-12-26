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

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) = oauthServiceRepo.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts, passwordValid)

    def updateUser(id: String, spec: utils.UserSpec) = oauthServiceRepo.updateUser(id, spec)

    def getAvatar(id: String) = oauthServiceRepo.getAvatar(id)

    def emailExists(email: String) = oauthServiceRepo.emailExists(email)
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
        u.email,
        u.firstname,
        u.lastname,
        u.createdAt,
        u.createdBy,
        u.lastModifiedAt,
        u.lastModifiedBy,
        u.gender,
        u.homeAddress,
        u.workAddress,
        u.contacts,
        u.avatar,
        u.passwordValid))

    val trashedUsers = Compiled(for {
      u <- Users if u._deleted
    } yield (
        u.id,
        u.email,
        u.firstname,
        u.lastname,
        u.createdAt,
        u.createdBy,
        u.lastModifiedAt,
        u.lastModifiedBy,
        u.gender,
        u.homeAddress,
        u.workAddress,
        u.contacts,
        u.avatar,
        u.passwordValid))

    val userById = {

      def getUser(id: Column[java.util.UUID]) =
        for {
          u <- Users if ! u._deleted && (u.id is id)
        } yield (
          u.id,
          u.email,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.avatar,
          u.passwordValid)

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
          u <- Users if !u._deleted && (u.email is username)
        } yield (u.id, u.password)

      Compiled(getUser _)
    }

    val emailExists = {
      def getEmail(email: Column[String]) =
        for {
          u <- Users if u.email.toLowerCase is email
        } yield true

      Compiled(getEmail _)
    }

    val userUpdates = {

      def forPasswdAndContacts(id: Column[java.util.UUID]) =
        Users where(_.id is id) map(o => (o.password, o.contacts))
      
      def forEmail(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.email, o.lastModifiedAt, o.lastModifiedBy))

      def forPasswd(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.password, o.passwordValid, o.lastModifiedAt, o.lastModifiedBy))

      def forFN(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.firstname, o.lastModifiedAt, o.lastModifiedBy))

      def forLN(id: Column[java.util.UUID]) = 
        Users where(_.id is id) map(o => (o.lastname, o.lastModifiedAt, o.lastModifiedBy))

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
        val email = Compiled(forEmail _)
        val password = Compiled(forPasswd _)
        val firstname = Compiled(forFN _)
        val lastname = Compiled(forLN _)
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
        case (id, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid, id = Some(id))
      }
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val user = q.userById(java.util.UUID.fromString(id))

      val result = db.withDynSession {
        user.firstOption
      }

      result map {
        case (sId, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid, id = Some(sId))
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
        case (id, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid, id = Some(id))
      }
    }

    def purgeUsers(users: Set[String]) = db.withTransaction { implicit sesssion =>
      val q = for { u <- Users if u.id inSet (users map java.util.UUID.fromString) } yield u

      users foreach(id => avatars ! utils.Avatars.Purge(id))

      q.delete
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

          def expired = refreshExpires exists (_ + createdAt < System.currentTimeMillis)

          if(expired) {
            db withTransaction { implicit session =>
              if(q.tokenSecret(accessToken).delete != 1) throw new Exception("getRefreshToken: can't delete expired refresh token")
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

          def generateToken = utils.SHA3Utils digest s"$clientId:$userId:${System.nanoTime}"
          def generateRefreshToken(accessToken: String) = utils.SHA3Utils digest s"$accessToken:$userId:${System.nanoTime}"
          def generateMacKey = utils.genPasswd(s"$userId:${System.nanoTime}")

          val accessToken = generateToken

          val currentTimestamp = System.currentTimeMillis

          if((OAuthTokens.forInsert +=
            (accessToken, clientId, redirectUri, userId, Some(generateRefreshToken(accessToken)), generateMacKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", aScopes)) != 1)
              throw new Exception("could not refresh Token")

          val newToken = q.accessToken(accessToken)

          newToken.firstOption map {
            case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, dScopes) =>

              if(q.tokenSecret(aAccessToken).delete != 1) throw new Exception("couldn't create new token")

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
              roles = Set(accessControlService.getUserRoles(sUser.id map(_.toString) get) map(_.role) : _*),
              permissions = {
                val userPermissions = accessControlService.getUserPermissions(sUser.id map(_.toString) get) // TODO: is this dependency safe
                Map(accessControlService.getPermissions map(p => (p.name, userPermissions contains p.name)) : _*)
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
      import Database.dynamicSession

      val user = q.auth(username)

      val result = db.withDynSession {
        user.firstOption
      }

      result collect {
        case (id, sPasswd) if passwords verify(password, sPasswd) => id.toString
      }
    }

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      db.withTransaction { implicit session =>

        val currentTimestamp = System.currentTimeMillis

        if((OAuthTokens.forInsert += (accessToken, clientId, redirectUri, java.util.UUID.fromString(userId), refreshToken, macKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", scopes)) != 1) throw new IllegalArgumentException("can't save token")

        val token = q.accessToken(accessToken)

        token.firstOption map {
          case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
            OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
        }
      }

    def saveUser(email: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) =
      db.withTransaction { implicit session =>
        val currentTimestamp = System.currentTimeMillis

        scala.util.control.Exception.allCatch.opt {

          Users insert(
            email, passwords crypt password, firstname, lastname, currentTimestamp, createdBy map java.util.UUID.fromString, Some(currentTimestamp), createdBy map java.util.UUID.fromString, gender, homeAddress, workAddress, contacts, passwordValid)
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

            val _1 = spec.email map {
              email =>
                q.userUpdates.email(uuid).update(email, currentTimestamp, Some(uuid)) == 1
            } getOrElse true

            val _2 = spec.password map {
              password =>
                spec.oldPassword.nonEmpty &&
                  (passwords verify(spec.oldPassword.get, sPassword)) &&
                  (q.userUpdates.password(uuid).update(passwords crypt password, true, currentTimestamp, Some(uuid)) == 1)
            } getOrElse true

            val _3 = spec.firstname map {
              firstname =>
                q.userUpdates.firstname(uuid).update(firstname, currentTimestamp, Some(uuid)) == 1
            } getOrElse true

            val _4 = spec.lastname map {
              lastname =>
                q.userUpdates.firstname(uuid).update(lastname, currentTimestamp, Some(uuid)) == 1
            } getOrElse true

            val _5 = spec.gender map {
              gender =>
                q.userUpdates.gender(uuid).update(gender, currentTimestamp, Some(uuid)) == 1
            } getOrElse true

            val _6 = spec.homeAddress foreach {
              case homeAddress =>
                q.userUpdates.homeAddress(uuid).update(homeAddress, currentTimestamp, Some(uuid)) == 1
            }

            val _7 = spec.workAddress foreach {
              case workAddress =>
                q.userUpdates.workAddress(uuid).update(workAddress, currentTimestamp, Some(uuid)) == 1
            }

            val _8 = spec.avatar foreach {
              case Some((avatar, data)) =>
                avatars ! utils.Avatars.Add(id, avatar, data)
                q.userUpdates.avatar(uuid).update(Some(avatar), currentTimestamp, Some(uuid)) == 1

              case _ =>
                avatars ! utils.Avatars.Purge(id)
                q.userUpdates.avatar(uuid).update(None, currentTimestamp, Some(uuid)) == 1
            }

            val _9 = spec.contacts map (contacts => q.userUpdates.contacts(uuid).update(contacts.diff(sContacts), currentTimestamp, Some(uuid)) == 1) getOrElse true

            _1 && _2 && _3 && _4 && _5 && _6 && _7 && _8 && _9
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

    def emailExists(email: String) = {
      import Database.dynamicSession

      db.withDynSession {
        q.emailExists(email.toLowerCase).firstOption
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
      cachingServices.get[List[domain.User]](ManyParams("users")) { super.getUsers.asInstanceOf[List[domain.User]] } getOrElse Nil

    override def getUser(id: String) =
      cachingServices.get[Option[domain.User]](Params(id)) { super.getUser(id).asInstanceOf[Option[domain.User]] } getOrElse None

    override def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) =
      super.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts, passwordValid) collect{
        case my: domain.User =>
          cachingServices.purge(Params(my.id.get.toString))
          cachingServices.purge(ManyParams("users"))
          my
      }

    override def updateUser(id: String, spec: utils.UserSpec) =
      super.updateUser(id, spec) collect {
        case my: domain.User =>
          cachingServices.purge(Params(my.id.get.toString))
          cachingServices.purge(ManyParams("users"))
          my
      }

    override def removeUser(id: String): Boolean =
      if(super.removeUser(id)){
        cachingServices.purge(Params(id))
        cachingServices.purge(ManyParams("users"))
        true
      } else false

    override def purgeUsers(users: Set[String]) {
      super.purgeUsers(users)
      users foreach(o => cachingServices.purge(Params(o)))
      cachingServices.purge(ManyParams("users"))
    }
  }

  override val accessControlService = new CachingAccessControlServicesImpl

  class CachingAccessControlServicesImpl extends AccessControlServicesImpl {

    override def getRoles =
      cachingServices.get[List[domain.Role]](ManyParams("roles")) { super.getRoles.asInstanceOf[List[domain.Role]] } getOrElse Nil

    override def getRole(roleName: String) =
      cachingServices.get[Option[domain.Role]](Params(roleName)) { super.getRole(roleName).asInstanceOf[Option[domain.Role]] } getOrElse None

    override def saveRole(name: String, parent: Option[String], createdBy: Option[String]) =
      super.saveRole(name, parent, createdBy) collect{
        case my: domain.Role =>
          cachingServices.purge(Params(my.name))
          cachingServices.purge(ManyParams("roles"))
          my
      }

    override def updateRole(name: String, newName: String, parent: Option[String]) =
      if(super.updateRole(name, newName, parent)) {
        cachingServices.purge(Params(newName))
        cachingServices.purge(ManyParams("roles"))
        true
      } else false

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
      implicit val tm = Timeout(10 seconds)

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