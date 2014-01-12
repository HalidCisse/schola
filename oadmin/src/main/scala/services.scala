package schola
package oadmin

trait OAuthServicesComponent {
  val oauthService: OAuthServices

  trait OAuthServices {

    type TokenLike = {
      val accessToken: String
      val clientId: String
      val redirectUri: String
      val userId: java.util.UUID
      val refreshToken: Option[String]
      val macKey: String
      val uA: String
      val expiresIn: Option[Long]
      val refreshExpiresIn: Option[Long]
      val createdAt: Long
      val lastAccessTime: Long
      val tokenType: String
      val scopes: Set[String]
    }

    type ClientLike = {
      val id: String
      val secret: String
      val redirectUri: String
    }

    type UserLike = {
      val id: Option[java.util.UUID]
      val primaryEmail: String
      val password: Option[String]
      val givenName: String
      val familyName: String
      val createdAt: Long
      val createdBy: Option[java.util.UUID]
      val lastModifiedAt: Option[Long]
      val lastModifiedBy: Option[java.util.UUID]
      val gender: domain.Gender.Value
      val homeAddress: Option[domain.AddressInfo]
      val workAddress: Option[domain.AddressInfo]
      //      val contacts: Option[ContactsLike]
      val contacts: domain.Contacts
      val avatar: Option[String]
      val _deleted: Boolean
      val suspended: Boolean
      val changePasswordAtNextLogin: Boolean
    }

    type SessionLike = {
      val key: String
      val secret: String
      val clientId: String
      val issuedTime: Long
      val expiresIn: Option[Long]
      val refreshExpiresIn: Option[Long]
      val refresh: Option[String]
      val lastAccessTime: Long
      val user: UserLike
      val userAgent: String
      val hasRole: Map[String, Boolean]
      val hasPermission: Map[String, Boolean]
      val scopes: Set[String]
    }

    def getUsers(page: Int): List[UserLike]

    def getUser(id: String): Option[UserLike]

    def removeUser(id: String): Boolean

    def getPurgedUsers: List[UserLike]

    def purgeUsers(users: Set[String])

    def undeleteUsers(users: Set[String])

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[TokenLike]

    def exchangeRefreshToken(refreshToken: String): Option[TokenLike]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[TokenLike]

    def getUserSession(params: Map[String, String]): Option[SessionLike]

    def getClient(id: String, secret: String): Option[ClientLike]

    def authUser(username: String, password: String): Option[String]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): Option[TokenLike]

    def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: domain.Contacts, changePasswordAtNextLogin: Boolean): Option[UserLike]

    def updateUser(id: String, spec: utils.UserSpec): Boolean

    def getAvatar(id: String): scala.concurrent.Future[(Option[String], String)]

    def primaryEmailExists(email: String): Boolean

    def createPasswdResetReq(username: String): Unit

    def checkActivationReq(username: String, ky: String): Boolean

    def changePasswd(username: String, ky: String, newPasswd: String): Boolean
  }
}

trait OAuthServicesRepoComponent {
  self: OAuthServicesComponent =>
  protected val oauthServiceRepo: OAuthServicesRepo

  trait OAuthServicesRepo {
    import oauthService._

    def getUsers(page: Int): List[UserLike]

    def getUser(id: String): Option[UserLike]

    def removeUser(id: String): Boolean

    def getPurgedUsers: List[UserLike]

    def purgeUsers(users: Set[String])

    def undeleteUsers(users: Set[String])

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[TokenLike]

    def exchangeRefreshToken(refreshToken: String): Option[TokenLike]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[TokenLike]

    def getUserSession(params: Map[String, String]): Option[SessionLike]

    def getClient(id: String, secret: String): Option[ClientLike]

    def authUser(username: String, password: String): Option[String]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): Option[TokenLike]

    def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: domain.Contacts, changePasswordAtNextLogin: Boolean): Option[UserLike]

    def updateUser(id: String, spec: utils.UserSpec): Boolean

    def getAvatar(id: String): scala.concurrent.Future[(Option[String], String)]

    def primaryEmailExists(email: String): Boolean

    def createPasswdResetReq(username: String): Unit

    def checkActivationReq(username: String, ky: String): Boolean

    def changePasswd(username: String, ky: String, newPasswd: String): Boolean
  }
}

trait AccessControlServicesComponent {
  val accessControlService: AccessControlServices

  trait AccessControlServices {

    type RoleLike = {
      val name: String
      val parent: Option[String]
      val createdAt: Long
      val createdBy: Option[java.util.UUID]
      val public: Boolean
    }

    type PermissionLike = {
      val name: String
      val clientId: String
    }

    type RolePermissionLike = {
      val role: String
      val permission: String
      val grantedAt: Long
      val grantedBy: Option[java.util.UUID]
    }

    type UserRoleLike = {
      val userId: java.util.UUID
      val role: String
      val grantedAt: Long
      val grantedBy: Option[java.util.UUID]
    }

    def getRoles: List[RoleLike]

    def getRole(name: String): Option[RoleLike]

    def getUserRoles(userId: String): List[UserRoleLike]

    def getPermissions: List[PermissionLike]

    def getRolePermissions(role: String): List[RolePermissionLike]

    def getUserPermissions(userId: String): Set[String]

    def getClientPermissions(clientId: String): List[PermissionLike]

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]): Option[RoleLike]

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String])

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String])

    def revokeUserRole(userId: String, roles: Set[String])

    def revokeRolePermission(role: String, permissions: Set[String])

    def purgeRoles(roles: Set[String])

    def userHasRole(userId: String, role: String): Boolean

    def roleHasPermission(role: String, permissions: Set[String]): Boolean

    def roleExists(role: String): Boolean

    def updateRole(name: String, newName: String, parent: Option[String]): Boolean
  }
}

trait AccessControlServicesRepoComponent {
  self: AccessControlServicesComponent =>

  protected val accessControlServiceRepo: AccessControlServiceRepo

  trait AccessControlServiceRepo {
    import accessControlService._

    def getRoles: List[RoleLike]

    def getRole(name: String): Option[RoleLike]

    def getUserRoles(userId: String): List[UserRoleLike]

    def getPermissions: List[PermissionLike]

    def getRolePermissions(role: String): List[RolePermissionLike]

    def getUserPermissions(userId: String): Set[String]

    def getClientPermissions(clientId: String): List[PermissionLike]

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]): Option[RoleLike]

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String])

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String])

    def revokeUserRole(userId: String, roles: Set[String])

    def revokeRolePermission(role: String, permissions: Set[String])

    def purgeRoles(roles: Set[String])

    def userHasRole(userId: String, role: String): Boolean

    def roleHasPermission(role: String, permissions: Set[String]): Boolean

    def roleExists(name: String): Boolean

    def updateRole(name: String, newName: String, parent: Option[String]): Boolean
  }

}

trait CachingServicesComponent {

  protected val cachingServices: CachingServices

  trait CachingServices {

    def get[T: scala.reflect.ClassTag](params: impl.CacheActor.Params)(default: => T): Option[T]

    def evict(params: impl.CacheActor.Params)
  }
}
