package schola
package oadmin

import Types._

trait UserServicesComponent {

  val userService: UserServices

  trait UserServices {

    def getUsersStats: StatsLike

    def getUsers(page: Int): List[UserLike]

    def getUser(id: String): Option[UserLike]

    def removeUser(id: String): Boolean

    def removeUsers(users: Set[String])

    def getPurgedUsers: List[UserLike]

    def purgeUsers(users: Set[String])

    def undeleteUsers(users: Set[String])

    def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Option[domain.Contacts], changePasswordAtNextLogin: Boolean): Option[UserLike]

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

    def getUsersStats: StatsLike

    def getUsers(page: Int): List[UserLike]

    def getUser(id: String): Option[UserLike]

    def removeUser(id: String): Boolean

    def removeUsers(users: Set[String])

    def getPurgedUsers: List[UserLike]

    def purgeUsers(users: Set[String])

    def undeleteUsers(users: Set[String])

    def saveUser(username: String, password: String, givenName: String, familyName: String, createdBy: Option[String], gender: domain.Gender, homeAddress: Option[domain.AddressInfo], workAddress: Option[domain.AddressInfo], contacts: Option[domain.Contacts], changePasswordAtNextLogin: Boolean): Option[UserLike]

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

    def getRefreshToken(refreshToken: String): Option[TokenLike]

    def exchangeRefreshToken(refreshToken: String): Option[TokenLike]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[TokenLike]

    def getUserSession(params: Map[String, String]): Option[SessionLike]

    def getClient(id: String, secret: String): Option[ClientLike]

    def authUser(username: String, password: String): Option[String]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): Option[TokenLike]
  }
}

trait OAuthServicesRepoComponent {

  protected val oauthServiceRepo: OAuthServicesRepo

  trait OAuthServicesRepo {

    def getTokenSecret(accessToken: String): Option[String]

    def getRefreshToken(refreshToken: String): Option[TokenLike]

    def exchangeRefreshToken(refreshToken: String): Option[TokenLike]

    def revokeToken(accessToken: String)

    def getUserTokens(userId: String): List[TokenLike]

    def getUserSession(params: Map[String, String]): Option[SessionLike]

    def getClient(id: String, secret: String): Option[ClientLike]

    def authUser(username: String, password: String): Option[String]

    def saveToken(accessToken: String, refreshToken: Option[String], macKey: String, uA: String, clientId: String, redirectUri: String, userId: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], scopes: Set[String]): Option[TokenLike]
  }
}

trait AvatarServicesComponent {

  val avatarServices: AvatarServices

  trait AvatarServices {

    def getAvatar(id: String): scala.concurrent.Future[(String, Option[String], Array[Byte])]

    def uploadAvatar(userId: String, filename: String, contentType: Option[String], bytes: Array[Byte]): scala.concurrent.Future[Boolean]

    def purgeAvatar(userId: String, avatarId: String): scala.concurrent.Future[Boolean]

    def purgeAvatarForUser(userId: String): Unit
  }
}

trait AvatarServicesRepoComponent {

  protected val avatarServicesRepo: AvatarServicesRepo

  trait AvatarServicesRepo {

    def getAvatar(id: String): scala.concurrent.Future[(String, Option[String], Array[Byte])]

    def uploadAvatar(userId: String, filename: String, contentType: Option[String], bytes: Array[Byte]): scala.concurrent.Future[Boolean]

    def purgeAvatar(userId: String, avatarId: String): scala.concurrent.Future[Boolean]

    def purgeAvatarForUser(userId: String): Unit
  }
}

trait AccessControlServicesComponent {

  val accessControlService: AccessControlServices

  trait AccessControlServices {

    def getRoles: List[RoleLike]

    def getRole(name: String): Option[RoleLike]

    def getPermissions: List[PermissionLike]

    def getRolePermissions(role: String): List[RolePermissionLike]

    def getClientPermissions(clientId: String): List[PermissionLike]

    def getUserRoles(userId: String): List[UserRoleLike]

    def getUserPermissions(userId: String): Set[String]

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String])

    def revokeUserRoles(userId: String, roles: Set[String])

    def userHasRole(userId: String, role: String): Boolean

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]): Option[RoleLike]

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String])

    def revokeRolePermission(role: String, permissions: Set[String])

    def purgeRoles(roles: Set[String])

    def roleHasPermission(role: String, permissions: Set[String]): Boolean

    def roleExists(role: String): Boolean

    def updateRole(name: String, newName: String, parent: Option[String]): Boolean
  }
}

trait AccessControlServicesRepoComponent {

  protected val accessControlServiceRepo: AccessControlServiceRepo

  trait AccessControlServiceRepo {

    def getRoles: List[RoleLike]

    def getRole(name: String): Option[RoleLike]

    def getPermissions: List[PermissionLike]

    def getRolePermissions(role: String): List[RolePermissionLike]

    def getClientPermissions(clientId: String): List[PermissionLike]

    def getUserRoles(userId: String): List[UserRoleLike]

    def getUserPermissions(userId: String): Set[String]

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String])

    def revokeUserRoles(userId: String, roles: Set[String])

    def userHasRole(userId: String, role: String): Boolean

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]): Option[RoleLike]

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String])

    def revokeRolePermission(role: String, permissions: Set[String])

    def purgeRoles(roles: Set[String])

    def roleHasPermission(role: String, permissions: Set[String]): Boolean

    def roleExists(name: String): Boolean

    def updateRole(name: String, newName: String, parent: Option[String]): Boolean
  }
}

trait LabelServicesComponent {

  val labelService: LabelServices

  trait LabelServices {

    def getLabels: List[LabelLike]

    def updateLabel(label: String, newName: String): Boolean

    def findOrNew(label: String, color: Option[String]): Option[LabelLike]

    def remove(labels: Set[String])
  }
}

trait LabelServicesRepoComponent {

  protected val labelServiceRepo: LabelServicesRepo

  trait LabelServicesRepo {

    def getLabels: List[LabelLike]

    def updateLabel(label: String, newName: String): Boolean

    def findOrNew(label: String, color: Option[String]): Option[LabelLike]

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
