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
    hasRole: Map[String, Boolean] = Map(),
    hasPermission: Map[String, Boolean] = Map(),
    scopes: Set[String] = Set())

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

  object Gender extends conversions.jdbc.DBEnum {
    val Male = Value
    val Female = Value
  }

  case class Email(email: String)
  case class PhoneNumber(phoneNumber: String)
  case class Fax(fax: String)

  case class HomeContactInfo(email: Option[Email], phoneNumer: Option[PhoneNumber], fax: Option[Fax])
  case class WorkContactInfo(email: Option[Email], phoneNumer: Option[PhoneNumber], fax: Option[Fax])

  case class MobileNumbers(mobile1: Option[PhoneNumber], mobile2: Option[PhoneNumber])

  case class Contacts(home: Option[HomeContactInfo], work: Option[WorkContactInfo], mobiles: MobileNumbers)

  case class AddressInfo(city: String, country: String, postalCode: String, streetAddress: String)

  case class AvatarInfo(mimeType: String, created: Long = System.currentTimeMillis)

  case class User(
     primaryEmail: String,
     password: Option[String],
     givenName: String,
     familyName: String,
     createdAt: Long = System.currentTimeMillis,
     createdBy: Option[java.util.UUID],
     lastLoginTime: Option[Long] = None,
     lastModifiedAt: Option[Long] = None,
     lastModifiedBy: Option[java.util.UUID] = None,
     gender: Gender.Value = Gender.Male,
     homeAddress: Option[AddressInfo] = None,
     workAddress: Option[AddressInfo] = None,
     contacts: Contacts = Contacts(None, None, MobileNumbers(None, None)),
     avatar: Option[AvatarInfo] = None,
     _deleted: Boolean = false,
     suspended: Boolean = false,
     changePasswordAtNextLogin: Boolean = false,
     id: Option[java.util.UUID] = None)

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
    val SuperUserR = Role(config.getString("oauth2.super-user-role-name"), None, createdAt = 0L, createdBy = None, public = false)

    val AdministratorR = Role(config.getString("oauth2.administrator-role-name"), Some(SuperUserR.name), createdAt = 0L, createdBy = None, public = false)

    val all = Set(SuperUserR, AdministratorR)
  }

  object U {
    val SuperUser =
      User(
        config.getString("root.primaryEmail"),
        Some(config.getString("root.password")),
        config.getString("root.givenName"),
        config.getString("root.familyName"), createdAt = 0L, createdBy = None,
        id = Some(java.util.UUID.fromString(config.getString("root.id"))))

    val all = Set(SuperUser)
  }

  case class RolePermission(role: String, permission: String, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID])

  case class UserRole(userId: java.util.UUID, role: String, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID])
}