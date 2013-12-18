package schola
package oadmin

package domain {

  case class Session(
    key: String,
    secret: String,
    clientId: String,
    issuedTime: Long,
    expiresIn: Option[Long],
    refreshExpiresIn: Option[Long],
    refresh: Option[String],
    lastAccessTime: Long,
    user: User,
    userAgent: String,
    roles: Set[String] = Set(),
    permissions: Map[String, Boolean] = Map(),
    scopes: Set[String] = Set()
  )

  case class OAuthToken(
     accessToken: String,
     clientId: String,
     redirectUri: String,
     userId: java.util.UUID,
     refreshToken: Option[String],
     macKey: String,
     uA: String,
     expiresIn: Option[Long],
     refreshExpiresIn: Option[Long],
     createdAt: Long = System.currentTimeMillis,
     lastAccessTime: Long = System.currentTimeMillis,
     tokenType: String = "mac",
     scopes: Set[String] = Set())

  case class OAuthClient(id: String, secret: String, redirectUri: String)

  object Clients {
    val OADMIN = "oadmin"
  }

//  @SerialVersionUID(8648206719264612402L)
  object Gender extends conversions.jdbc.DBEnum {
    val Male = Value
    val Female = Value
  }

  sealed trait ContactValue

  case class Email(email: String) extends ContactValue
  case class PhoneNumber(number: String) extends ContactValue
  case class Fax(fax: String) extends ContactValue

  sealed trait ContactInfo

  case class HomeContactInfo(home: ContactValue) extends ContactInfo
  case class WorkContactInfo(work: ContactValue) extends ContactInfo
  case class MobileContactInfo(mobile: PhoneNumber) extends ContactInfo

  case class AddressInfo(city: String, country: String, zipCode: String, addressLine: String)

  case class AvatarInfo(contentType: String, created: Long = System.currentTimeMillis)

  case class User(
     id: Option[java.util.UUID],
     email: String,
     password: Option[String],
     firstname: String,
     lastname: String,
     createdAt: Long = System.currentTimeMillis,
     createdBy: Option[java.util.UUID],
     lastModifiedAt: Option[Long] = None,
     lastModifiedBy: Option[java.util.UUID] = None,
     gender: Gender.Value = Gender.Male,
     homeAddress: Option[AddressInfo] = None,
     workAddress: Option[AddressInfo] = None,
     contacts: Set[ContactInfo] = Set(),
     avatar: Option[AvatarInfo] = None,
     _deleted: Boolean = false,
     passwordValid: Boolean = false)

  case class Role(name: String, parent: Option[String], createdAt: Long = System.currentTimeMillis, createdBy: Option[java.util.UUID], public: Boolean = true)

  case class Permission(name: String, clientId: String)

  object P {
    val ViewUser = Permission("user.view", Clients.OADMIN)
    val ChangeUser = Permission("user.change", Clients.OADMIN)
    val DeleteUser = Permission("user.remove", Clients.OADMIN)
    val PurgeUser = Permission("user.purge", Clients.OADMIN)

    val ViewRole = Permission("role.view", Clients.OADMIN)
    val ChangeRole = Permission("role.change", Clients.OADMIN)
    val PurgeRole = Permission("role.purge", Clients.OADMIN)

    val GrantRole = Permission("role.grant", Clients.OADMIN)
    val RevokeRole = Permission("role.revoke", Clients.OADMIN)

    val GrantPermission = Permission("permission.grant", Clients.OADMIN)
    val RevokePermission = Permission("permission.revoke", Clients.OADMIN)

    val all = Set(
      ViewUser,
      ChangeUser,
      DeleteUser,
      PurgeUser,

      ViewRole,
      ChangeRole,
      PurgeRole,

      GrantRole,
      RevokeRole,

      GrantPermission,
      RevokePermission
     )
  }

  object R {
    val SuperUserR = Role(config.getString("super-user-role-name"), None, createdAt = 0L, createdBy = None, public = false)

    val AdministratorR = Role(config.getString("administrator-role-name"), Some(SuperUserR.name), createdAt = 0L, createdBy = None, public = false)

    val all = Set(SuperUserR, AdministratorR)
  }

  object U {
    val SuperUser =
      User(
        Some(java.util.UUID.fromString(config.getString("super-user-id"))),
        config.getString("super-user-email"),
        Some(config.getString("super-user-password")),
        config.getString("super-user-firstname"),
        config.getString("super-user-lastname"), createdAt = 0L, createdBy = None,
        passwordValid = true)

    val all = Set(SuperUser)
  }

  case class RolePermission(role: String, permission: String, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID])

  case class UserRole(userId: java.util.UUID, role: String, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID])
}