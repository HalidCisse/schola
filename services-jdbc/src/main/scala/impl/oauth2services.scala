package ma.epsilon.schola

package impl

trait OAuthServicesComponentImpl extends OAuthServicesComponent {
  this: OAuthServicesRepoComponent =>

  class OAuthServicesImpl extends OAuthServices {

    def getTokenSecret(accessToken: String) = oauthServiceRepo.getTokenSecret(accessToken)

    def getRefreshToken(refreshToken: String) = oauthServiceRepo.getRefreshToken(refreshToken)

    def exchangeRefreshToken(refreshToken: String) = oauthServiceRepo.exchangeRefreshToken(refreshToken)

    def getUserTokens(userId: String) = oauthServiceRepo.getUserTokens(userId)

    def getUserSession(params: Map[String, String]) = oauthServiceRepo.getUserSession(params)

    def revokeToken(accessToken: String) = oauthServiceRepo.revokeToken(accessToken)

    def getClient(id: String, secret: String) = oauthServiceRepo.getClient(id, secret)

    def authUser(username: String, password: String) = oauthServiceRepo.authUser(username, password)

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], accessRights: Set[domain.AccessRight]) =
      oauthServiceRepo.saveToken(accessToken, refreshToken, macKey, uA, clientId, redirectUri, userId, expiresIn, refreshExpiresIn, accessRights)

    def getUserAccessRights(userId: String) = oauthServiceRepo.getUserAccessRights(userId)
  }
}

trait OAuthServicesRepoComponentImpl extends OAuthServicesRepoComponent {

  import schema._
  import domain._
  import Q._

  private[this] val log = Logger("oadmin.oauthserviceRepoImpl")

  protected val db: Database

  protected val oauthServiceRepo = new OAuthServicesRepoImpl

  class OAuthServicesRepoImpl extends OAuthServicesRepo {

    private[this] object oq {

      val tokenSecret = {
        def getTokenSecret(accessToken: Column[String]) =
          for {
            t <- OAuthTokens if t.accessToken is accessToken
          } yield t.macKey

        Compiled(getTokenSecret _)
      }

      val lastAccessTime = {
        def getLastAccessTime(accessToken: Column[String]) =
          for {
            t <- OAuthTokens if t.accessToken is accessToken
          } yield t.lastAccessTime

        Compiled(getLastAccessTime _)
      }

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
            t.accessRights)

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
            t.accessRights)

        Compiled(getRefreshToken _)
      }

      val bearerToken = {
        def getAccessToken(accessToken: Column[String]) =
          OAuthTokens where (_.accessToken is accessToken)

        Compiled(getAccessToken _)
      }

      val accessToken = {
        def getAccessToken(mAccessToken: Column[String]) =
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
            t.accessRights)

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
            t.accessRights)

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
            (u, t) <- Users leftJoin OAuthTokens on (_.id is _.userId)
            if /*(t.userId is uuid(userId)) &&
                         */ (t.accessToken is bearerToken) &&
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
            t.accessRights))

        Compiled(getUserSession _)
      }

      val auth = {
        def getUser(username: Column[String]) =
          for {
            u <- Users if !u._deleted && (u.primaryEmail is username)
          } yield (u.id, u.password)

        Compiled(getUser _)
      }

      val forLastLoginTime = {
        def getLastLoginTime(id: Column[java.util.UUID]) =
          Users where (_.id is id) map (_.lastLoginTime)

        Compiled(getLastLoginTime _)
      }

      val userRights = {
        def getUserAccessRights(id: Column[java.util.UUID]) =
          for {
            (userAccessRight, accessRight) <- UsersAccessRights leftJoin AccessRights on (_.accessRightId is _.id) if userAccessRight.userId is id
          } yield accessRight

        Compiled(getUserAccessRights _)
      }
    }

    def getTokenSecret(accessToken: String) = {
      import Database.dynamicSession

      val token = oq.tokenSecret(accessToken)

      db.withDynSession {
        token.firstOption
      }
    }

    def getRefreshToken(refreshToken: String) = {

      val token = oq.refreshToken(refreshToken)

      val result = db.withSession { implicit session =>
        token.firstOption
      }

      result flatMap {
        case (accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, sAccessRights) =>

          def isExpired = refreshExpires exists (_ * 1000 + createdAt < System.currentTimeMillis)

          if (isExpired) {
            db withTransaction { implicit session =>
              if (oq.bearerToken(accessToken).delete != 1)
                throw new Exception("getRefreshToken: can't delete expired refresh token")
              None
            }
          } else Some(OAuthToken(accessToken, clientId, redirectUri, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, accessRights = sAccessRights))
      }
    }

    def exchangeRefreshToken(refreshToken: String) = db.withTransaction { implicit session =>

      val token = oq.forExchange(refreshToken)

      token.firstOption flatMap {
        case (aAccessToken, clientId, redirectUri, userId, uA, Some(aRefreshToken), issuedTime, expiresIn, refreshExpiresIn, aAccessRights) if refreshExpiresIn map (issuedTime + 1000 * _ > System.currentTimeMillis) getOrElse true => //aRefreshToken exists

          def generateToken = utils.Crypto.generateSecureToken
          def generateRefreshToken = utils.Crypto.generateSecureToken
          def generateMacKey = utils.Crypto.genMacKey(s"$userId:${System.nanoTime}")

          try {

            val currentTimestamp = System.currentTimeMillis

            Some {

              OAuthTokens insert (
                generateToken,
                clientId,
                redirectUri,
                userId,
                Some(generateRefreshToken),
                generateMacKey,
                uA,
                expiresIn,
                refreshExpiresIn,
                currentTimestamp,
                currentTimestamp,
                "mac",
                aAccessRights)
            }

          } finally utils.tryo {
            oq.bearerToken(aAccessToken).delete
          }

        case _ => None
      }
    }

    def revokeToken(accessToken: String) =
      db.withTransaction { implicit session =>
        val token = oq.tokenSecret(accessToken)
        token.delete == 1
      }

    def getUserTokens(userId: String) = {
      import Database.dynamicSession

      val userTokens = oq.userTokens(uuid(userId))

      val result = db.withDynSession {
        userTokens.list
      }

      result map {
        case (sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sAccessRights) =>
          OAuthToken(sAccessToken, sClientId, sRedirectUri, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, accessRights = sAccessRights)
      }
    }

    def getUserSession(params: Map[String, String]) = {
      val bearerToken = params("bearerToken")
      val userAgent = params("userAgent")

      val session = oq.session(bearerToken, userAgent)

      val result = db.withTransaction {
        implicit s => session.firstOption
      }

      result map {
        case (sUser, (sAccessToken, sClientId, sRefreshToken, sMacKey, sUA, sExpiresIn, sRefreshExpiresIn, sCreatedAt, sLastAccessTime, sAccessRights)) =>

          utils.tryo {
            db.withTransaction {
              implicit s =>
                oq.lastAccessTime(sAccessToken) update System.currentTimeMillis
            }
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
            superUser = sUser.id == U.SuperUser.id,
            suspended = sUser.suspended,
            changePasswordAtNextLogin = sUser.changePasswordAtNextLogin,
            user = Profile(sUser.id.get, sUser.primaryEmail, sUser.givenName, sUser.familyName, sUser.createdAt, sUser.createdBy, sUser.lastModifiedAt, sUser.lastModifiedBy, sUser.gender, sUser.homeAddress, sUser.workAddress, sUser.contacts),
            userAgent = sUA,
            accessRights = sAccessRights)
      }
    }

    def getClient(id: String, secret: String) = {
      import Database.dynamicSession

      val client = oq.client(id, secret)

      val result = db.withDynSession {
        client.firstOption
      }

      result map {
        case (cId, cSecret, redirectUri) =>
          OAuthClient(cId, cSecret, redirectUri)
      }
    }

    def authUser(username: String, password: String) = {
      val user = oq.auth(username)

      val result = db.withSession { implicit session =>
        user.firstOption
      }

      result collect {
        case (id, sPasswd) if passwords verify (password, sPasswd) =>

          db.withTransaction { implicit session =>
            oq.forLastLoginTime(id) update Some(System.currentTimeMillis)
          }

          id.toString
      }
    }

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], accessRights: Set[domain.AccessRight]) =
      db.withTransaction {
        implicit session =>
          val currentTimestamp = System.currentTimeMillis

          OAuthTokens insert (
            accessToken,
            clientId,
            redirectUri,
            uuid(userId),
            refreshToken,
            macKey,
            uA,
            expiresIn,
            refreshExpiresIn,
            currentTimestamp,
            currentTimestamp,
            "mac",
            accessRights)
      }

    def getUserAccessRights(userId: String) = db.withSession { implicit session => oq.userRights(uuid(userId)).list }
  }
}
