package schola
package oadmin

package impl

import org.clapper.avsl.Logger

trait OAuthServicesComponentImpl extends OAuthServicesComponent {
  self: OAuthServicesRepoComponent =>

  val oauthService = new OAuthServicesImpl

  class OAuthServicesImpl extends OAuthServices {

    def getUsers = oauthServiceRepo.getUsers

    def getUser(id: String) = oauthServiceRepo.getUser(id)

    def removeUser(id: String) = oauthServiceRepo.removeUser(id)

    def getPurgedUsers = oauthServiceRepo.getPurgedUsers

    def purgeUsers(users: Set[String]) = oauthServiceRepo.purgeUsers(users)

    def getToken(bearerToken: String) = oauthServiceRepo.getToken(bearerToken)

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

  class OAuthServicesRepoImpl extends OAuthServicesRepo {
    import oauthService._

    import schema._
    import domain._

    def getUsers = {
      import Database.dynamicSession

      val q = for {
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
          u.passwordValid)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (id, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(Some(id), email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid)
      }
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val q = for {
        u <- Users if ! u._deleted && (u.id is java.util.UUID.fromString(id))
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

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (sId, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(Some(sId), email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid)
      }
    }

    def removeUser(id: String) = {
      import Database.dynamicSession

      db.withDynSession {
        (Users.forDeletion(id) update(true)) == 1
      }
    }

    def getPurgedUsers = {
      import Database.dynamicSession

      val q = for {
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
          u.passwordValid)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (id, email, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid) =>
          User(Some(id), email, None, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, passwordValid = passwordValid)
      }
    }

    def purgeUsers(users: Set[String]) = db.withTransaction { implicit sesssion =>
      val q = for { u <- Users if u.id inSet (users map java.util.UUID.fromString) } yield u

      users foreach(id => avatars ! utils.Avatars.Purge(id))

      q.delete == users.size
    }

    def getToken(bearerToken: String) = db.withTransaction { implicit session =>

      val q = for {
        (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is bearerToken
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


      q.firstOption map {
        case (accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, sCreatedAt, sLastAccessTime, sScopes) =>

          OAuthTokens map(_.lastAccessTime) update(System.currentTimeMillis) // Touch session

          OAuthToken(accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, sCreatedAt, sLastAccessTime, scopes = sScopes)
      }
    }

    def getTokenSecret(accessToken: String) = {
      import Database.dynamicSession

      val q = for {
        t <- OAuthTokens if t.accessToken is accessToken
      } yield t.macKey


      db.withDynSession {
        q.firstOption
      }
    }

    def getRefreshToken(refreshToken: String) = {
      import Database.dynamicSession

      val q = for {
        (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.refreshToken is refreshToken
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

      val result = db.withDynSession {
        q.firstOption
      }

      result flatMap {
        case (accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, sScopes) =>

          def expired = refreshExpires exists (_ + createdAt < System.currentTimeMillis)

          if(expired) {
            if(!((OAuthTokens where(_.accessToken is accessToken) delete) == 1)) throw new Exception("getRefreshToken: can't delete expired refresh token")
            None
          }
          else Some(OAuthToken(accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, scopes = sScopes))
      }
    }

    def exchangeRefreshToken(refreshToken: String) = db.withTransaction { implicit session =>

      val q = for {
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

      q.firstOption flatMap {
        case (aAccessToken, clientId, redirectUri, userId, uA, Some(aRefreshToken), issuedTime, expiresIn, refreshExpiresIn, aScopes) if refreshExpiresIn map(t => issuedTime + t * 1000 > System.currentTimeMillis) getOrElse true => //aRefreshToken exists

          def generateToken = utils.SHA3Utils digest s"$clientId:$userId:${System.nanoTime}"
          def generateRefreshToken(accessToken: String) = utils.SHA3Utils digest s"$accessToken:$userId:${System.nanoTime}"
          def generateMacKey = utils.genPasswd(s"$userId:${System.nanoTime}")

          val accessToken = generateToken

          val currentTimestamp = System.currentTimeMillis

          if((OAuthTokens.forInsert +=
            (accessToken, clientId, redirectUri, userId, Some(generateRefreshToken(accessToken)), generateMacKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", aScopes)) != 1)
              throw new Exception("could not refresh Token")

//          OAuthTokens += OAuthToken(
//            accessToken,
//            clientId,
//            redirectUri,
//            userId,
//            Some(generateRefreshToken(accessToken)),
//            macKey = generateMacKey,
//            uA = uA,
//            expiresIn = Some(AccessTokenSessionLifeTime),
//            refreshExpiresIn = Some(RefreshTokenSessionLifeTime),
//            scopes = aScopes
//          )

          val q2 = for {
            (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is accessToken
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

          q2.firstOption map {
            case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, dScopes) =>

              if((for {
                t <- OAuthTokens if t.accessToken is aAccessToken
              } yield t).delete != 1) throw new Exception("couldn't create new token")

              OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, scopes = dScopes)
          }

        case _ => None
      }
    }

    def revokeToken(accessToken: String) =
      db.withTransaction { implicit session =>

        val q = for {
          t <- OAuthTokens if t.accessToken is accessToken
        } yield t

        q.delete == 1
      }

    def getUserTokens(userId: String) = {
      import Database.dynamicSession

      val q = for {
        t <- OAuthTokens if (t.userId is java.util.UUID.fromString(userId))
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

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
          OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
      }
    }

    def getUserSession(params: Map[String, String]) =
      db.withTransaction{ implicit session =>
  //      val userId = params("userId")
        val bearerToken = params("bearerToken")
        val userAgent = params("userAgent")

        val q = for {
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

        q.firstOption map {
          case (sUser, (sAccessToken, sClientId, sRefreshToken, sMacKey, sUA, sExpiresIn, sRefreshExpiresIn, sCreatedAt, sLastAccessTime, sScopes)) =>

            scala.util.control.Exception.allCatch.opt {
              OAuthTokens map(_.lastAccessTime) update(System.currentTimeMillis)
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
              roles = Set(accessControlService.getUserRoles(sUser.id map(_.toString) get) map(_.role) : _*), // TODO: is this dependency safe
              permissions = {
                val userPermissions = accessControlService.getUserPermissions(sUser.id map(_.toString) get)
                Map(accessControlService.getPermissions map(p => (p.name, userPermissions contains p.name)) : _*)
              },
              scopes = sScopes
            )
        }
      }

    def getClient(id: String, secret: String) = {
      import Database.dynamicSession

      val q = for {
        c <- OAuthClients if (c.id is id) && (c.secret is secret)
      } yield (
          c.id,
          c.secret,
          c.redirectUri)

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (cId, cSecret, redirectUri) =>
          OAuthClient(cId, cSecret, redirectUri)
      }
    }

    def authUser(username: String, password: String) = {
      import Database.dynamicSession

      val q = for {
        u <- Users if !u._deleted && (u.email is username)
      } yield (u.id, u.password)

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (id, sPasswd) if passwords verify(password, sPasswd) => id.toString
      }
    }

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      db.withTransaction { implicit session =>

        val currentTimestamp = System.currentTimeMillis

        if((OAuthTokens.forInsert += (accessToken, clientId, redirectUri, java.util.UUID.fromString(userId), refreshToken, macKey, uA, expiresIn, refreshExpiresIn, currentTimestamp, currentTimestamp, "mac", scopes)) != 1) throw new IllegalArgumentException("can't save token")

        val q = for {
          (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is accessToken
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

        q.firstOption map {
          case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
            OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
        }
      }

    def saveUser(email: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) =
      db.withTransaction { implicit session =>
        val id = java.util.UUID.randomUUID

        val currentTimestamp = System.currentTimeMillis

        if((
          Users.forInsert += (id, email, passwords crypt password, firstname, lastname, currentTimestamp, createdBy map java.util.UUID.fromString, Some(currentTimestamp), createdBy map java.util.UUID.fromString, gender, homeAddress, workAddress, contacts, passwordValid)
          ) != 1) throw new IllegalArgumentException("saveUser: can't save user")

        val q = for{
          u <- Users if u.id is id
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

        q.firstOption map {
          case (sId, uEmail, uFirstname, uLastname, createdAt, uCreatedBy, lastModifiedAt, lastModifiedBy, sGender, sHomeAddress, sWorkAddress, sContacts, sAvatar, sPasswordValid) =>
            User(Some(sId), uEmail, None, uFirstname, uLastname, createdAt, uCreatedBy, lastModifiedAt, lastModifiedBy, sGender, sHomeAddress, sWorkAddress, sContacts, sAvatar, passwordValid = sPasswordValid)
        }
      }

    def updateUser(id: String, spec: utils.UserSpec) = {
      val q = schema.Users filter (_.id is java.util.UUID.fromString(id))

      db.withSession {
        implicit session => (for {u <- q} yield (u.password, u.contacts)).firstOption
      } match {

        case Some((sPassword, sContacts)) => if (db.withTransaction {
          implicit session =>

            val currentTimestamp = System.currentTimeMillis

            val _1 = spec.email map {
              email =>
                (q map (_.email) update (email)) == 1
            } getOrElse true

            val _2 = spec.password map {
              password =>
                spec.oldPassword.nonEmpty &&
                  (passwords verify(spec.oldPassword.get, sPassword)) &&
                  ((q map (o => (o.password, o.passwordValid, o.lastModifiedAt, o.lastModifiedBy)) update(passwords crypt (password), true, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1)
            } getOrElse true

            val _3 = spec.firstname map {
              firstname =>
                (q map (o => (o.firstname, o.lastModifiedAt, o.lastModifiedBy)) update(firstname, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1
            } getOrElse true

            val _4 = spec.lastname map {
              lastname =>
                (q map (o => (o.lastname, o.lastModifiedAt, o.lastModifiedBy)) update(lastname, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1
            } getOrElse true

            val _5 = spec.gender map {
              gender =>
                (q map (o => (o.gender, o.lastModifiedAt, o.lastModifiedBy)) update(gender, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1
            } getOrElse true

            val _6 = spec.homeAddress foreach {
              case homeAddress =>
                (q map (o => (o.homeAddress, o.lastModifiedAt, o.lastModifiedBy)) update(homeAddress, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1
            }

            val _7 = spec.workAddress foreach {
              case workAddress =>
                (q map (o => (o.workAddress, o.lastModifiedAt, o.lastModifiedBy)) update(workAddress, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1
            }

            val _8 = spec.avatar foreach {
              case Some((avatar, data)) =>
                avatars ! utils.Avatars.Add(id, avatar, data)
                (q map (o => (o.avatar, o.lastModifiedAt, o.lastModifiedBy)) update(Some(avatar), Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1

              case _ =>
                avatars ! utils.Avatars.Purge(id)
                (q map (o => (o.avatar, o.lastModifiedAt, o.lastModifiedBy)) update(None, Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1
            }

            val _9 = spec.contacts map (contacts => (q map (o => (o.contacts, o.lastModifiedAt, o.lastModifiedBy)) update(contacts.diff(sContacts), Some(currentTimestamp), Some(java.util.UUID.fromString(id)))) == 1) getOrElse true

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

      implicit val timeout = akka.util.Timeout(2 seconds) // needed for `?` below

      val q = (avatars ? utils.Avatars.Get(id)).mapTo[(domain.AvatarInfo, Array[Byte])]

      allCatch.opt {
        Await.result(q, timeout.duration)
      }
    }

    def emailExists(email: String) = {
      import Database.dynamicSession

      val byEmail = for {
        email <- Parameters[String]
        u <- Users if u.email.toLowerCase is email
      } yield true


      db.withDynSession {
        byEmail(email.toLowerCase).firstOption
      } getOrElse false
    }
  }
}