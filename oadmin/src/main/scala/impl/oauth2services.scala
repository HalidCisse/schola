package schola
package oadmin

package impl

trait OAuthServicesComponentImpl extends OAuthServicesComponent[({type λ[α] = α })#λ] {
  self: OAuthServicesRepoComponent[({type λ[α] = α })#λ] =>

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

    def revokeToken(accessToken: String) = oauthServiceRepo.revokeToken(accessToken)

    def getClient(id: String, secret: String) = oauthServiceRepo.getClient(id, secret)

    def authUser(username: String, password: String) = oauthServiceRepo.authUser(username, password)

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      oauthServiceRepo.saveToken(accessToken, refreshToken, macKey, clientId, redirectUri, userId, expiresIn, refreshExpiresIn, scopes)

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo]) = oauthServiceRepo.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts)
  }
}

trait OAuthServicesRepoComponentImpl extends OAuthServicesRepoComponent[({type λ[α] = α })#λ] {
  self: OAuthServicesComponent[({type λ[α] = α })#λ] =>

  import Q._

  protected val db: Database

  protected val oauthServiceRepo = new OAuthServicesRepoImpl

  class OAuthServicesRepoImpl extends OAuthServicesRepo {

    import schema._
    import domain._

    def getUsers = {
      import Database.dynamicSession

      val q = for {
        u <- Users if ! u._deleted
      } yield (
          u.id,
          u.username,
          u.password,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (id, username, password, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts) =>
          User(id, username, password, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts)
      }
    }

    def getUser(id: String) = {
      import Database.dynamicSession

      val q = for {
        u <- Users if ! u._deleted && (u.id is java.util.UUID.fromString(id))
      } yield (
          u.id,
          u.username,
          u.password,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts)

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (sId, username, password, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts) =>
          User(sId, username, password, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts)
      }
    }

    def removeUser(id: String) = {
      import Database.dynamicSession
      val q = Users.filter(_.id is java.util.UUID.fromString(id)).map(_._deleted)
      db.withDynSession {
        q.update(true) == 1
      }
    }

    def getPurgedUsers = {
      import Database.dynamicSession

      val q = for {
        u <- Users if u._deleted
      } yield (
          u.id,
          u.username,
          u.password,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (id, username, password, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts) =>
          User(id, username, password, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts)
      }
    }

    def purgeUsers(users: Set[String]) = db.withTransaction { implicit sesssion =>
      val q = for { u <- Users if u.id inSet (users map java.util.UUID.fromString) } yield u
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
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)


      q.firstOption map {
        case (accessToken, clientId, redirectUri, userId, refreshToken, macKey, expiresIn, refreshExpiresIn, sCreatedAt, sLastAccessTime, sScopes) =>

          OAuthTokens map(_.lastAccessTime) update(System.currentTimeMillis) // Touch session

          OAuthToken(accessToken, clientId, redirectUri, userId, refreshToken, macKey, expiresIn, refreshExpiresIn, sCreatedAt, sLastAccessTime, scopes = sScopes)
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
          t.expiresIn,
          t.refreshExpiresIn,
          t.createdAt,
          t.lastAccessTime,
          t.scopes)

      val result = db.withDynSession {
        q.firstOption
      }

      result flatMap {
        case (accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, expires, refreshExpires, createdAt, lastAccessTime, sScopes) =>

          def expired = refreshExpires exists (_ + createdAt < System.currentTimeMillis)

          if(expired) {
            if(!((OAuthTokens where(_.accessToken is accessToken) delete) == 1)) throw new Exception("getRefreshToken: can't delete expired refresh token")
            None
          }
          else Some(OAuthToken(accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, expires, refreshExpires, createdAt, lastAccessTime, scopes = sScopes))
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
          t.refreshToken,
          t.scopes)

      q.firstOption flatMap {
        case (aAccessToken, clientId, redirectUri, userId, Some(aRefreshToken), aScopes) => //aRefreshToken exists

          def generateToken = util.SHA3Utils digest s"$clientId:$userId:${System.nanoTime}"
          def generateRefresh(accessToken: String) = util.SHA3Utils digest s"$accessToken:$userId:${System.nanoTime}"
          def generateMacKey = util.genPasswd(s"$userId:${System.nanoTime}")

          val accessToken = generateToken

          OAuthTokens += OAuthToken(
            accessToken,
            clientId,
            redirectUri,
            userId,
            Some(generateRefresh(accessToken)),
            macKey = generateMacKey,
            expiresIn = Some(AccessTokenSessionLifeTime),
            refreshExpiresIn = Some(RefreshTokenSessionLifeTime),
            scopes = aScopes
          )

          val q2 = for {
            (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is accessToken
          } yield (
              t.accessToken,
              t.clientId,
              c.redirectUri,
              t.userId,
              t.refreshToken,
              t.macKey,
              t.expiresIn,
              t.refreshExpiresIn,
              t.createdAt,
              t.lastAccessTime,
              t.scopes)

          q2.firstOption map {
            case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, dScopes) =>

              if((for {
                t <- OAuthTokens if t.accessToken is aAccessToken
              } yield t).delete != 1) throw new Exception("couldn't create new exception")

              OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sExpires, sRefreshExpires, dCreatedAt, dLastAccessTime, scopes = dScopes)
          }
      }
    }

    def revokeToken(accessToken: String) =
    db.withTransaction { implicit session =>

      val q = for {
        t <- OAuthTokens if t.accessToken is accessToken
      } yield t

      q.delete == 1
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
        u <- Users if !u._deleted && (u.username is username)
      } yield (
          u.id,
          u.username,
          u.password,
          u.firstname,
          u.lastname,
          u.createdAt,
          u.createdBy,
          u.lastModifiedAt,
          u.lastModifiedBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts)

      val result = db.withDynSession {
        q.firstOption
      }

      result collect {
        case (id, uUsername, uPassword, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts) if passwords verify(password, uPassword) =>
          User(id, uUsername, uPassword, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts)
      }
    }

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      db.withTransaction { implicit session =>

        if((OAuthTokens += OAuthToken(
          accessToken,
          clientId,
          redirectUri,
          java.util.UUID.fromString(userId),
          refreshToken,
          macKey = macKey,
          expiresIn = expiresIn,
          refreshExpiresIn = refreshExpiresIn,
          scopes = scopes
        )) != 1) throw oauth2.BadTokenException("can't save token")

        val q = for {
          (t, c) <- OAuthTokens leftJoin OAuthClients on (_.clientId is _.id) if t.accessToken is accessToken
        } yield (
            t.accessToken,
            t.clientId,
            c.redirectUri,
            t.userId,
            t.refreshToken,
            t.macKey,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.scopes)

        q.firstOption map {
          case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sScopes) =>
            OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, scopes = sScopes)
        }
      }

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo]) =
      db.withTransaction { implicit session =>
        val id = java.util.UUID.randomUUID

        if((
          Users += User(id, username, passwords crypt password, firstname, lastname, createdBy = createdBy map java.util.UUID.fromString, gender = gender, homeAddress = homeAddress, workAddress = workAddress, contacts = contacts)
          ) != 1) throw new Exception("saveUser: can't save user")

        val q = for{
          u <- Users if u.id is id
        } yield (
            u.id,
            u.username,
            u.password,
            u.firstname,
            u.lastname,
            u.createdAt,
            u.createdBy,
            u.lastModifiedAt,
            u.lastModifiedBy,
            u.gender,
            u.homeAddress,
            u.workAddress,
            u.contacts)

        q.firstOption map {
          case (sId, uUsername, uPassword, uFirstname, uLastname, createdAt, uCreatedBy, lastModifiedAt, lastModifiedBy, sGender, sHomeAddress, sWorkAddress, sContacts) =>
            User(sId, uUsername, uPassword, uFirstname, uLastname, createdAt, uCreatedBy, lastModifiedAt, lastModifiedBy, sGender, sHomeAddress, sWorkAddress, sContacts)
        }
      }
  }
}