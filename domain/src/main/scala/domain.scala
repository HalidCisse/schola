package schola
package oadmin
package domain

object `package` {

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

  object Gender extends Enumeration {
    val Male = Value
    val Female = Value
  }

  type Gender = Gender.Value

  case class ContactInfo(email: Option[String], phoneNumber: Option[String], fax: Option[String])

  case class MobileNumbers(mobile1: Option[String], mobile2: Option[String])

  case class Contacts(mobiles: MobileNumbers, home: Option[ContactInfo], work: Option[ContactInfo])

  case class AddressInfo(city: String, country: String, postalCode: String, streetAddress: String)

  case class UsersStats(count: Int)

  case class AvatarInfo(filename: String, contentType: String, data: String, base64: Boolean)

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
      gender: Gender = Gender.Male,
      homeAddress: Option[AddressInfo] = None,
      workAddress: Option[AddressInfo] = None,
      contacts: Contacts = Contacts(MobileNumbers(None, None), None, None),
      avatar: Option[String] = None,
      activationKey: Option[String] = None,
      _deleted: Boolean = false,
      suspended: Boolean = false,
      changePasswordAtNextLogin: Boolean = false,
      id: Option[java.util.UUID] = None,
      labels: List[String] = Nil) {

    @inline def isSuperuser = id == U.SuperUser.id
  }

  case class Response(success: Boolean)

  case class Role(name: String, parent: Option[String] = None, createdAt: Long = System.currentTimeMillis, createdBy: Option[java.util.UUID] = None, publiq: Boolean = true)

  case class Permission(name: String, clientId: String)

  object P {
    val ChangeUser = Permission("user.modify", Clients.OADMIN)
    val ViewUser = Permission("user.view", Clients.OADMIN)
    val DeleteUser = Permission("user.remove", Clients.OADMIN)
    val PurgeUser = Permission("user.purge", Clients.OADMIN)
    val UnPurgeUser = Permission("user.undelete", Clients.OADMIN)

    val ChangeRole = Permission("role.modify", Clients.OADMIN)
    val ViewRole = Permission("role.view", Clients.OADMIN)
    val PurgeRole = Permission("role.purge", Clients.OADMIN)

    val GrantRole = Permission("role.grant", Clients.OADMIN)
    val RevokeRole = Permission("role.revoke", Clients.OADMIN)

    val GrantPermission = Permission("permission.grant", Clients.OADMIN)
    val RevokePermission = Permission("permission.revoke", Clients.OADMIN)

    val all = Set(
      ChangeUser,
      ViewUser,
      DeleteUser,
      PurgeUser,
      UnPurgeUser,

      ChangeRole,
      ViewRole,
      PurgeRole,

      GrantRole,
      RevokeRole,

      GrantPermission,
      RevokePermission)
  }

  object R {
    val SuperUserR = Role(config.getString("oauth2.super-user-role-name"), None, createdAt = 0L, createdBy = None, publiq = false)

    val AdministratorR = Role(config.getString("oauth2.administrator-role-name"), None, createdAt = 0L, createdBy = None, publiq = false)

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

    val Params = new {
      val DParams = List(
        "primaryEmail",
        "givenName",
        "familyName",
        "gender",
        "password",
        "new_password",
        "password_confirmation")

      val HomeAddressParams = List(
        "homeAddress[country]" -> "country",
        "homeAddress[city]" -> "city",
        "homeAddress[postalCode]" -> "postalCode",
        "homeAddress[streetAddress]" -> "streetAddress")

      val WorkAddressParams = List(
        "workAddress[country]" -> "country",
        "workAddress[city]" -> "city",
        "workAddress[postalCode]" -> "postalCode",
        "workAddress[streetAddress]" -> "streetAddress")

      val WorkContactParams = List(
        "contacts[work][phoneNumber]" -> "phoneNumber",
        "contacts[work][fax]" -> "fax",
        "contacts[work][email]" -> "email")

      val HomeContactParams = List(
        "contacts[home][phoneNumber]" -> "phoneNumber",
        "contacts[home][fax]" -> "fax",
        "contacts[home][email]" -> "email")

      val Mobile1ContactParams = List(
        "contacts[mobiles][mobile1]" -> "phoneNumber")

      val Mobile2ContactParams = List(
        "contacts[mobiles][mobile2]" -> "phoneNumber")
    }
  }

  case class RolePermission(role: String, permission: String, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID] = None)

  case class UserRole(userId: java.util.UUID, role: String, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID] = None, delegated: Boolean = false)

  case class Label(name: String, color: String)

  case class UserLabel(userId: java.util.UUID, label: String)
}