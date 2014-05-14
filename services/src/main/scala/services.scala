package ma.epsilon.schola

trait Apps {
  val appService: AppServices

  trait AppServices {

    def getApps: List[domain.App]

    def addApp(name: String, scopes: List[String]): domain.App

    def removeApp(id: String)
  }
}

trait AppsRepo {
  protected val appsServiceRepo: AppServicesRepo

  trait AppServicesRepo {

    def getApps: List[domain.App]

    def addApp(name: String, scopes: List[String]): domain.App

    def removeApp(id: String)
  }
}

trait UserServicesComponent {

  val userService: UserServices

  trait UserServices {

    def getUsersStats: domain.UsersStats

    def getUsers(page: Int): List[domain.User]

    def getUser(id: String): Option[domain.User]

    def getUserByCIN(cin: String): Option[domain.User]

    def removeUser(id: String): Boolean

    def removeUsers(users: Set[String])

    def getPurgedUsers: List[domain.User]

    def purgeUsers(users: Set[String])

    def undeleteUsers(users: Set[String])

    def suspendUsers(users: Set[String])

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
      accessRights: List[String]): domain.User

    def updateUser(id: String, spec: domain.UserSpec): Boolean

    def primaryEmailExists(email: String): Boolean

    def labelUser(userId: String, labels: Set[String])

    def unLabelUser(userId: String, labels: Set[String])

    def getUserLabels(userId: String): List[String]

    def createPasswdResetReq(username: String): Unit

    def checkActivationReq(username: String, ky: String): Boolean

    def resetPasswd(username: String, ky: String, newPasswd: String): Boolean

    def getPage(userId: String): Int
  }
}

trait UserServicesRepoComponent {

  protected val userServiceRepo: UserServicesRepo

  trait UserServicesRepo {

    def getUsersStats: domain.UsersStats

    def getUsers(page: Int): List[domain.User]

    def getUser(id: String): Option[domain.User]

    def getUserByCIN(cin: String): Option[domain.User]

    def removeUser(id: String): Boolean

    def removeUsers(users: Set[String])

    def getPurgedUsers: List[domain.User]

    def purgeUsers(users: Set[String])

    def undeleteUsers(users: Set[String])

    def suspendUsers(users: Set[String])

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
      accessRights: List[String]): domain.User

    def updateUser(id: String, spec: domain.UserSpec): Boolean

    def primaryEmailExists(email: String): Boolean

    def labelUser(userId: String, labels: Set[String])

    def unLabelUser(userId: String, labels: Set[String])

    def getUserLabels(userId: String): List[String]

    def createPasswdResetReq(username: String): Unit

    def checkActivationReq(username: String, ky: String): Boolean

    def resetPasswd(username: String, ky: String, newPasswd: String): Boolean

    def getPage(userId: String): Int
  }
}

trait OAuthServicesComponent {

  val oauthService: OAuthServices

  trait OAuthServices {

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[domain.OAuthToken]

    def exchangeRefreshToken(refreshToken: String): Option[domain.OAuthToken]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[domain.OAuthToken]

    def getUserSession(params: Map[String, String]): Option[domain.Session]

    def getClient(id: String, secret: String): Option[domain.OAuthClient]

    def authUser(username: String, password: String): Option[String]

    def saveToken(
      accessToken: String, 
      refreshToken: Option[String], 
      macKey: String, 
      uA: String, 
      userId: String, 
      expiresIn: Option[java.time.Duration], 
      refreshExpiresIn: Option[java.time.Duration], 
      accessRights: Set[domain.AccessRight],
      activeAccessRight: Option[String]): domain.OAuthToken

    def setUserAccessRight(accessToken: String, accessRightId: String)

    def getUserAccessRights(userId: String): List[domain.AccessRight]
  }
}

trait OAuthServicesRepoComponent {

  protected val oauthServiceRepo: OAuthServicesRepo

  trait OAuthServicesRepo {

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[domain.OAuthToken]

    def exchangeRefreshToken(refreshToken: String): Option[domain.OAuthToken]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[domain.OAuthToken]

    def getUserSession(params: Map[String, String]): Option[domain.Session]

    // def getClient(id: String, secret: String): Option[OAuthClient]

    def authUser(username: String, password: String): Option[String]

    def saveToken(
      accessToken: String, 
      refreshToken: Option[String], 
      macKey: String, 
      uA: String, 
      userId: String, 
      expiresIn: Option[java.time.Duration], 
      refreshExpiresIn: Option[java.time.Duration], 
      accessRights: Set[domain.AccessRight],
      activeAccessRight: Option[String]): domain.OAuthToken

    def setUserAccessRight(accessToken: String, accessRightId: String)

    def getUserAccessRights(userId: String): List[domain.AccessRight]
  }
}

trait UploadServicesComponent {

  val uploadServices: UploadServices

  trait UploadServices {

    def getUpload(id: String): scala.concurrent.Future[domain.Upload]

    def upload(id: String, filename: String, contentType: Option[String], bytes: Array[Byte], attributes: (String, String)*)

    def purgeUpload(id: String)
  }
}

trait UploadServicesRepoComponent {

  protected val uploadServicesRepo: UploadServicesRepo

  trait UploadServicesRepo {

    def getUpload(id: String): scala.concurrent.Future[domain.Upload]

    def upload(id: String, filename: String, contentType: Option[String], bytes: Array[Byte], attributes: (String, String)*)

    def purgeUpload(id: String)
  }
}

trait LabelServicesComponent {

  val labelService: LabelServices

  trait LabelServices {

    def getLabels: List[domain.Label]

    def updateLabel(label: String, newName: String): Boolean

    def findOrNew(label: String, color: Option[String]): Option[domain.Label]

    def remove(labels: Set[String])
  }
}

trait LabelServicesRepoComponent {

  protected val labelServiceRepo: LabelServicesRepo

  trait LabelServicesRepo {

    def getLabels: List[domain.Label]

    def updateLabel(label: String, newName: String): Boolean

    def findOrNew(label: String, color: Option[String]): Option[domain.Label]

    def remove(labels: Set[String])
  }
}

trait CachingServicesComponent {

  protected val cachingServices: CachingServices

  trait CachingServices {

    type Params = {
      val cacheKey: String
    }

    def get[T: scala.reflect.ClassTag](params: Params)(default: => T): Option[T]

    def evict(params: Params)
  }
}

trait AkkaSystemProvider {
  protected def system: akka.actor.ActorSystem
}
