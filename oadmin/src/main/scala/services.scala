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
      val email: String
      val password: String
      val firstname: String
      val lastname: String
      val createdAt: Long
      val createdBy: Option[java.util.UUID]
      val lastModifiedAt: Option[Long]
      val lastModifiedBy: Option[java.util.UUID]
      val gender: domain.Gender.Value
      val homeAddress: Option[domain.AddressInfo]
      val workAddress: Option[domain.AddressInfo]
      val contacts: Set[domain.ContactInfo]
      val avatar: Option[domain.AvatarInfo]
      val _deleted: Boolean
      val passwordValid: Boolean
    }

    type SessionLike = {
      val key: String
      val secret: String
      val clientId: String
      val issuedTime: Long
      val expiresIn: Option[Long]
      val refresh: Option[String]
      val lastAccessTime: Long
      val user: UserLike
<<<<<<< HEAD
      val userAgent: String
=======
>>>>>>> 32f31336eeccbeb06cb34896c3a9378f152ed90c
      val roles: Set[String]
      val permissions: Map[String, Boolean]
      val scopes: Set[String]
    }

    def getUsers: List[UserLike]

    def getUser(id: String): Option[UserLike]

    def removeUser(id: String): Boolean

    def getPurgedUsers: List[UserLike]

    def purgeUsers(users: Set[String])

    def getToken(bearerToken: String): Option[TokenLike]

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[TokenLike]

    def exchangeRefreshToken(refreshToken: String): Option[TokenLike]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[TokenLike]

    def getUserSession(params: Map[String, String]): Option[SessionLike]

    def getClient(id: String, secret: String): Option[ClientLike]

    def authUser(username: String, password: String): Option[String]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): Option[TokenLike]

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean): Option[UserLike]

    def updateUser(id: String, spec: utils.UserSpec): Option[UserLike]

    def getAvatar(id: String): Option[(domain.AvatarInfo, Array[Byte])]

    def emailExists(email: String): Boolean
  }

  trait OAuthServicesDelegate extends OAuthServices { self =>
    protected val delegate: OAuthServices

    def getUsers = delegate.getUsers

    def getUser(id: String) = delegate.getUser(id)

    def removeUser(id: String) = delegate.removeUser(id)

    def getPurgedUsers = delegate.getPurgedUsers

    def purgeUsers(users: Set[String]) = delegate.purgeUsers(users)

    def getToken(bearerToken: String) = delegate.getToken(bearerToken)

    def getTokenSecret(accessToken: String) = delegate.getTokenSecret(accessToken)

    def getRefreshToken(refreshToken: String) = delegate.getRefreshToken(refreshToken)

    def exchangeRefreshToken(refreshToken: String) = delegate.exchangeRefreshToken(refreshToken)

    def revokeToken(accessToken: String) = delegate.revokeToken(accessToken)

    def getUserTokens(userId: String) = delegate.getUserTokens(userId)

    def getUserSession(params: Map[String, String]) = delegate.getUserSession(params)

    def getClient(id: String, secret: String) = delegate.getClient(id, secret)

    def authUser(username: String, password: String) = delegate.authUser(username, password)

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      delegate.saveToken(accessToken, refreshToken, macKey, uA, clientId, redirectUri, userId, expiresIn, refreshExpiresIn, scopes)

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean) = delegate.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts, passwordValid)

    def updateUser(id: String, spec: utils.UserSpec) = delegate.updateUser(id, spec)

    def getAvatar(id: String) = delegate.getAvatar(id)

    def emailExists(email: String) = delegate.emailExists(email)
  }

}

trait OAuthServicesRepoComponent {
  self: OAuthServicesComponent =>
  protected val oauthServiceRepo: OAuthServicesRepo    

  trait OAuthServicesRepo { 
    import oauthService._   

    def getUsers: List[UserLike]

    def getUser(id: String): Option[UserLike]

    def removeUser(id: String): Boolean

    def getPurgedUsers: List[UserLike]

    def purgeUsers(users: Set[String])

    def getToken(bearerToken: String): Option[TokenLike]

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[TokenLike]

    def exchangeRefreshToken(refreshToken: String): Option[TokenLike]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[TokenLike]

    def getUserSession(params: Map[String, String]): Option[SessionLike]

    def getClient(id: String, secret: String): Option[ClientLike]

    def authUser(username: String, password: String): Option[String]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): Option[TokenLike]

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo], passwordValid: Boolean): Option[UserLike]

    def updateUser(id: String, spec: utils.UserSpec): Option[UserLike]

    def getAvatar(id: String): Option[(domain.AvatarInfo, Array[Byte])]

    def emailExists(email: String): Boolean
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

  trait AccessControlServicesDelegate extends AccessControlServices {
    val delegate: AccessControlServices

    def getRoles = delegate.getRoles

    def getRole(name: String) = delegate.getRole(name)

    def getUserRoles(userId: String) = delegate.getUserRoles(userId)

    def getPermissions = delegate.getPermissions

    def getRolePermissions(role: String) = delegate.getRolePermissions(role)

    def getUserPermissions(userId: String) = delegate.getUserPermissions(userId)

    def getClientPermissions(clientId: String) = delegate.getClientPermissions(clientId)

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]) = delegate.saveRole(name, parent, createdBy)

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String]) = delegate.grantRolePermissions(role, permissions, grantedBy)

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String]) = delegate.grantUserRoles(userId, roles, grantedBy)

    def revokeUserRole(userId: String, roles: Set[String]) = delegate.revokeUserRole(userId, roles)

    def revokeRolePermission(role: String, permissions: Set[String]) = delegate.revokeRolePermission(role, permissions)

    def purgeRoles(roles: Set[String]) = delegate.purgeRoles(roles)

    def userHasRole(userId: String, role: String) = delegate.userHasRole(userId, role)

    def roleHasPermission(role: String, permissions: Set[String]) = delegate.roleHasPermission(role, permissions)

    def roleExists(role: String) = delegate.roleExists(role)

    def updateRole(name: String, newName: String, parent: Option[String]) = delegate.updateRole(name, newName, parent)
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