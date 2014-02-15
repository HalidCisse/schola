package schola
package oadmin

object Types {

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
    val gender: domain.Gender
    val homeAddress: Option[domain.AddressInfo]
    val workAddress: Option[domain.AddressInfo]
    val contacts: Option[domain.Contacts]
    val avatar: Option[String]
    val _deleted: Boolean
    val suspended: Boolean
    val changePasswordAtNextLogin: Boolean
  }

  type UserLabelLike = {
    val userId: java.util.UUID
    val label: String
  }

  type UserRoleLike = {
    val userId: java.util.UUID
    val role: String
    val grantedAt: Long
    val grantedBy: Option[java.util.UUID]
  }

  type StatsLike = {
    val count: Int
  }

  //

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

  // 

  type RoleLike = {
    val name: String
    val parent: Option[String]
    val createdAt: Long
    val createdBy: Option[java.util.UUID]
    val publiq: Boolean
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

  // 

  type LabelLike = {
    val name: String
    val color: String
  }
}