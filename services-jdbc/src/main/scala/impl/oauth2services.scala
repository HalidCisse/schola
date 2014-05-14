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

    def getClient(id: String, secret: String) = Some(domain.OAuthClient(OAUTH_CLIENT, OAUTH_CLIENT_SECRET, OAUTH_REDIRECT_URI)) // oauthServiceRepo.getClient(id, secret)

    def authUser(username: String, password: String) = oauthServiceRepo.authUser(username, password)

    def saveToken(
      accessToken: String, 
      refreshToken: Option[String], 
      macKey: String, 
      uA: String, 
      userId: String, 
      expiresIn: Option[java.time.Duration], 
      refreshExpiresIn: Option[java.time.Duration], 
      accessRights: Set[domain.AccessRight],
      activeAccessRight: Option[String]) = oauthServiceRepo.saveToken(accessToken, refreshToken, macKey, uA, userId, expiresIn, refreshExpiresIn, accessRights, activeAccessRight)

    def setUserAccessRight(accessToken: String, accessRightId: String) = oauthServiceRepo.setUserAccessRight(accessToken, accessRightId)

    def getUserAccessRights(userId: String) = oauthServiceRepo.getUserAccessRights(userId)
  }
}

trait OAuthServicesRepoComponentImpl extends OAuthServicesRepoComponent {

  import schema._
  import domain._
  import jdbc.Q._

  private[this] val log = Logger("oadmin.oauthserviceRepoImpl")

  protected val db: Database

  protected val oauthServiceRepo = new OAuthServicesRepoImpl

  class OAuthServicesRepoImpl extends OAuthServicesRepo {

    private[this] object oq {

      val tokenSecret = {
        def getTokenSecret(accessToken: Column[String]) =
          for {
            t <- OAuthTokens if t.accessToken === accessToken
          } yield t.macKey

        Compiled(getTokenSecret _)
      }

      val lastAccessTime = {
        def getLastAccessTime(accessToken: Column[String]) =
          for {
            t <- OAuthTokens if t.accessToken === accessToken
          } yield t.lastAccessTime

        Compiled(getLastAccessTime _)
      }

      val refreshToken = {
        def getRefreshToken(mRefreshToken: Column[String]) =
          for {
            /*(*/t/*, c)*/ <- OAuthTokens/* leftJoin OAuthClients on (_.clientId === _.id)*/ if t.refreshToken === mRefreshToken
          } yield (
            t.accessToken,
            t.userId,
            t.refreshToken,
            t.macKey,
            t.uA,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.accessRights,
            t.activeAccessRightId)

        Compiled(getRefreshToken _)
      }

      val forExchange = {
        def getRefreshToken(refreshToken: Column[String]) =
          for {
            /*(*/t/*, c)*/ <- OAuthTokens /*leftJoin OAuthClients on (_.clientId === _.id)*/ if t.refreshToken === refreshToken
          } yield (
            t.accessToken,
            t.userId,
            t.uA,
            t.refreshToken,
            t.createdAt,
            t.expiresIn,
            t.refreshExpiresIn,
            t.accessRights,
            t.activeAccessRightId)

        Compiled(getRefreshToken _)
      }

      val bearerToken = {
        def getAccessToken(accessToken: Column[String]) =
          OAuthTokens filter (_.accessToken === accessToken)

        Compiled(getAccessToken _)
      }

      val accessToken = {
        def getAccessToken(mAccessToken: Column[String]) =
          for {
            /*(*/t/*, c)*/ <- OAuthTokens /*leftJoin OAuthClients on (_.clientId === _.id)*/ if t.accessToken === mAccessToken
          } yield (
            t.accessToken,
            t.userId,
            t.refreshToken,
            t.macKey,
            t.uA,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.accessRights,
            t.activeAccessRightId)

        Compiled(getAccessToken _)
      }

      val userTokens = {
        def getUserTokens(userId: Column[java.util.UUID]) =
          for {
            t <- OAuthTokens if (t.userId === userId)
          } yield (
            t.accessToken,
            t.userId,
            t.refreshToken,
            t.macKey,
            t.uA,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.accessRights,
            t.activeAccessRightId)

        Compiled(getUserTokens _)
      }

/*      val client = {
        def getClient(id: Column[String], secret: Column[String]) =
          for {
            c <- OAuthClients if (c.id === id) && (c.secret === secret)
          } yield (
            c.id,
            c.secret,
            c.redirectUri)

        Compiled(getClient _)
      }*/

      val session = {
        def getUserSession(bearerToken: Column[String], userAgent: Column[String]) =
          for {
            (u, t) <- Users leftJoin OAuthTokens on (_.id === _.userId)
            if /*(t.userId === uuid(userId)) &&
                         */ (t.accessToken === bearerToken) &&
              (t.uA === userAgent)
          } yield (u, (
            t.accessToken,
            t.refreshToken,
            t.macKey,
            t.uA,
            t.expiresIn,
            t.refreshExpiresIn,
            t.createdAt,
            t.lastAccessTime,
            t.accessRights,
            t.activeAccessRightId))

        Compiled(getUserSession _)
      }

      val auth = {
        def getUser(username: Column[String]) =
          for {
            u <- Users if !u._deleted && (u.primaryEmail === username)
          } yield (u.id, u.password)

        Compiled(getUser _)
      }

      val forLastLoginTime = {
        def getLastLoginTime(id: Column[java.util.UUID]) =
          Users filter (_.id === id) map (_.lastLoginTime)

        Compiled(getLastLoginTime _)
      }

      val userRights = {
        def getUserAccessRights(id: Column[java.util.UUID]) =
          for {
            (userAccessRight, accessRight) <- UsersAccessRights leftJoin AccessRights on (_.accessRightId === _.id) if userAccessRight.userId === id
          } yield accessRight

        Compiled(getUserAccessRights _)
      }

      val setUserAccessRight = {
        def getTokenActiveAccessRight(accessToken: Column[String]) = 
          OAuthTokens filter(_.accessToken === accessToken) map (_.activeAccessRightId)

        Compiled(getTokenActiveAccessRight _)
      }

      val accessRight = {
        def getAccessRight(accessRightId: Column[java.util.UUID]) = 
          AccessRights filter(_.id === accessRightId)

        Compiled(getAccessRight _)
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
        case (accessToken, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, sAccessRights, sActiveAccessRight) =>

          def isExpired = refreshExpires exists (expiration => createdAt.plusSeconds(expiration.getSeconds) isBefore java.time.LocalDateTime.now) // refreshExpires exists (_ * 1000 + createdAt < java.time.LocalDateTime.now)

          if (isExpired) {
            db withTransaction { implicit session =>
              if (oq.bearerToken(accessToken).delete != 1)
                throw new Exception("getRefreshToken: can't delete expired refresh token")
              None
            }
          } else Some(OAuthToken(accessToken, userId, sRefreshToken, macKey, uA, expires, refreshExpires, createdAt, lastAccessTime, accessRights = sAccessRights, activeAccessRight = sActiveAccessRight))
      }
    }

    def exchangeRefreshToken(refreshToken: String) = db.withTransaction { implicit session =>

      val token = oq.forExchange(refreshToken)

      token.firstOption flatMap {
        case (aAccessToken, userId, uA, Some(aRefreshToken), issuedTime, expiresIn, refreshExpiresIn, aAccessRights, sActiveAccessRight) if refreshExpiresIn map (expiration => issuedTime.plusSeconds(expiration.getSeconds) isAfter java.time.LocalDateTime.now)/*(issuedTime + 1000 * _ > java.time.LocalDateTime.now)*/ getOrElse true => //aRefreshToken exists

          def generateToken = utils.Crypto.generateSecureToken
          def generateRefreshToken = utils.Crypto.generateSecureToken
          def generateMacKey = utils.Crypto.genMacKey(s"$userId:${System.nanoTime}")

          try {

            val currentTimestamp = java.time.LocalDateTime.now

            Some {

              OAuthTokens insert (
                generateToken,
                userId,
                Some(generateRefreshToken),
                generateMacKey,
                uA,
                expiresIn,
                refreshExpiresIn,
                currentTimestamp,
                currentTimestamp,
                "mac",
                accessRights = aAccessRights,
                activeAccessRightId = sActiveAccessRight)
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
        case (sAccessToken, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, sAccessRights, sActiveAccessRight) =>
          OAuthToken(sAccessToken, sUserId, sRefreshToken, sMacKey, sUA, sExpires, sRefreshExpires, sCreatedAt, sLastAccessTime, accessRights = sAccessRights, activeAccessRight = sActiveAccessRight)
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
        case (sUser, (sAccessToken, sRefreshToken, sMacKey, sUA, sExpiresIn, sRefreshExpiresIn, sCreatedAt, sLastAccessTime, sAccessRights, sActiveAccessRight)) =>

          utils.tryo {
            db.withTransaction {
              implicit s =>
                oq.lastAccessTime(sAccessToken) update java.time.LocalDateTime.now
            }
          } // Touch session

          def getAccessRight(accessRightId: java.util.UUID) = {
            val accessRight = oq.accessRight(accessRightId)

            db.withSession {
              implicit session =>
                accessRight.firstOption
            }
          }          

          Session(
            sAccessToken,
            sMacKey,
            java.time.Instant.from(sCreatedAt),
            sExpiresIn,
            sRefreshExpiresIn,
            sRefreshToken,
            sLastAccessTime,
            superUser = sUser.id == U.SuperUser.id,
            suspended = sUser.suspended,
            changePasswordAtNextLogin = sUser.changePasswordAtNextLogin,
            user = Profile(sUser.id.get, sUser.cin, sUser.primaryEmail, sUser.givenName, sUser.familyName, sUser.createdAt, sUser.createdBy, sUser.lastModifiedAt, sUser.lastModifiedBy, sUser.gender, sUser.homeAddress, sUser.workAddress, sUser.contacts),
            userAgent = sUA,
            accessRights = sAccessRights,
            activeAccessRight = sActiveAccessRight flatMap getAccessRight)
      }
    }

/*    def getClient(id: String, secret: String) = {
      import Database.dynamicSession

      val client = oq.client(id, secret)

      val result = db.withDynSession {
        client.firstOption
      }

      result map {
        case (cId, cSecret, redirectUri) =>
          OAuthClient(cId, cSecret, redirectUri)
      }
    }*/

    def authUser(username: String, password: String) = {
      val user = oq.auth(username)

      val result = db.withSession { implicit session =>
        user.firstOption
      }

      result collect {
        case (id, sPasswd) if passwords verify (password, sPasswd) =>

          db.withTransaction { implicit session =>
            oq.forLastLoginTime(id) update Some(java.time.LocalDateTime.now)
          }

          id.toString
      }
    }

    def saveToken(
      accessToken: String, 
      refreshToken: Option[String], 
      macKey: String, 
      uA: String, 
      userId: String, 
      expiresIn: Option[java.time.Duration], 
      refreshExpiresIn: Option[java.time.Duration], 
      accessRights: Set[domain.AccessRight],
      activeAccessRight: Option[String]) =
      db.withTransaction {
        implicit session =>
          val currentTimestamp = java.time.LocalDateTime.now

          OAuthTokens insert (
            accessToken,
            uuid(userId),
            refreshToken,
            macKey,
            uA,
            expiresIn,
            refreshExpiresIn,
            currentTimestamp,
            currentTimestamp,
            "mac",
            accessRights,
            activeAccessRight map uuid)
      }

    def setUserAccessRight(accessToken: String, accessRightId: String) {
      val token = oq.setUserAccessRight(accessToken)

      db.withTransaction{ 
        implicit session =>
          token update Some(uuid(accessRightId))
      }
    }

    def getUserAccessRights(userId: String) = db.withSession { implicit session => oq.userRights(uuid(userId)).list }
  }
}
