package schola
package oadmin

trait OAuthServicesComponent[M[_]] {
  val oauthService: OAuthServices

  trait OAuthServices {

    type TokenLike = {
      val accessToken: String
      val clientId: String
      val redirectUri: String
      val userId: java.util.UUID
      val refreshToken: Option[String]
      val macKey: String
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

/*    type HasId = {
      def id: Int
    }

    type ContactValueLike = {
      val _type: String
      val value: String
    }

    type ContactInfoLike = {
      val _type: HasId
      val value: ContactValueLike
      val primary: Boolean
    }

    type AddressInfoLike = {
      val city: String
      val country: String
      val zipCode: String
      val addressLine: String
    }*/

    type UserLike = {
      val id: java.util.UUID
      val username: String
      val password: String
      val firstname: String
      val lastname: String
      val createdAt: Long
      val createdBy: Option[java.util.UUID]
      val lastModifiedAt: Option[Long]
      val lastModifiedBy: Option[java.util.UUID]
//      val gender: HasId
//      val homeAddress: Option[AddressInfoLike]
//      val workAddress: Option[AddressInfoLike]
//      val contacts: Set[ContactInfoLike]
      val gender: domain.Gender.Value
      val homeAddress: Option[domain.AddressInfo]
      val workAddress: Option[domain.AddressInfo]
      val contacts: Set[domain.ContactInfo]
      val _deleted: Boolean
    }

    def getUsers: M[List[UserLike]]

    def getUser(id: String): M[Option[UserLike]]

    def removeUser(id: String): M[Boolean]

    def getPurgedUsers: M[List[UserLike]]

    def purgeUsers(users: Set[String])

    def getToken(bearerToken: String): M[Option[TokenLike]]

    def getTokenSecret(accessToken: String): M[Option[String]]

    def getRefreshToken(refreshToken: String): M[Option[TokenLike]]

    def exchangeRefreshToken(refreshToken: String): M[Option[TokenLike]]

    def revokeToken(accessToken: String)

    def getClient(id: String, secret: String): M[Option[ClientLike]]

    def authUser(username: String, password: String): M[Option[UserLike]]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): M[Option[TokenLike]]

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo]): M[Option[UserLike]]
  }

  trait OAuthServicesDelegate extends OAuthServices {
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

    def getClient(id: String, secret: String) = delegate.getClient(id, secret)

    def authUser(username: String, password: String) = delegate.authUser(username, password)

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]) =
      delegate.saveToken(accessToken, refreshToken, macKey, clientId, redirectUri, userId, expiresIn, refreshExpiresIn, scopes)

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo]) = delegate.saveUser(username, password, firstname, lastname, createdBy, gender, homeAddress, workAddress, contacts)
  }

}

trait OAuthServicesRepoComponent[M[_]] {
  self: OAuthServicesComponent[M] =>
  protected val oauthServiceRepo: OAuthServicesRepo  

  trait OAuthServicesRepo {

    import oauthService._

    def getUsers: M[List[UserLike]]

    def getUser(id: String): M[Option[UserLike]]

    def removeUser(id: String): M[Boolean]

    def getPurgedUsers: M[List[UserLike]]

    def purgeUsers(users: Set[String])

    def getToken(bearerToken: String): M[Option[TokenLike]]

    def getTokenSecret(accessToken: String): M[Option[String]]

    def getRefreshToken(refreshToken: String): M[Option[TokenLike]]

    def exchangeRefreshToken(refreshToken: String): M[Option[TokenLike]]

    def revokeToken(accessToken: String)

    def getClient(id: String, secret: String): M[Option[ClientLike]]

    def authUser(username: String, password: String): M[Option[UserLike]]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): M[Option[TokenLike]]

    def saveUser(username: String, password: String, firstname: String, lastname: String, createdBy: Option[String], gender: domain.Gender.Value, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Set[domain.ContactInfo]): M[Option[UserLike]]

    //    def updateUser(id: String, updateSpec: ...): Option[UserLike]
  }

}

trait AccessControlServicesComponent[M[_]] {
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

    def getRoles: M[List[RoleLike]]

    def getUserRoles(userId: String): M[List[UserRoleLike]]

    def getPermissions: M[List[PermissionLike]]

    def getRolePermissions(role: String): M[List[RolePermissionLike]]

    def getUserPermissions(userId: String): M[Set[String]]

    def getClientPermissions(clientId: String): M[List[PermissionLike]]

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]): M[Option[RoleLike]]

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String])

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String])

    def revokeUserRole(userId: String, roles: Set[String])

    def revokeRolePermission(role: String, permissions: Set[String])

    def purgeRoles(roles: Set[String])

    def userHasRole(userId: String, role: String): M[Boolean]

    def roleHasPermission(role: String, permissions: Set[String]): M[Boolean]
  }

  trait AccessControlServicesDelegate extends AccessControlServices {
    val delegate: AccessControlServices

    def getRoles = delegate.getRoles

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
  }

}

trait AccessControlServicesRepoComponent[M[_]] {
  self: AccessControlServicesComponent[M] =>
  protected val accessControlServiceRepo: AccessControlServiceRepo

  trait AccessControlServiceRepo {

    import accessControlService._

    def getRoles: M[List[RoleLike]]

    def getUserRoles(userId: String): M[List[UserRoleLike]]

    def getPermissions: M[List[PermissionLike]]

    def getRolePermissions(role: String): M[List[RolePermissionLike]]

    def getUserPermissions(userId: String): M[Set[String]]

    def getClientPermissions(clientId: String): M[List[PermissionLike]]

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]): M[Option[RoleLike]]

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String])

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String])

    def revokeUserRole(userId: String, roles: Set[String])

    def revokeRolePermission(role: String, permissions: Set[String])

    def purgeRoles(roles: Set[String])

    def userHasRole(userId: String, role: String): M[Boolean]

    def roleHasPermission(role: String, permissions: Set[String]): M[Boolean]
  }

}