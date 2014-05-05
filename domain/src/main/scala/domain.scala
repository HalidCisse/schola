package ma.epsilon.schola
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
    superUser: Boolean,
    suspended: Boolean,
    changePasswordAtNextLogin: Boolean,
    user: Profile,
    userAgent: String,
    accessRights: Set[AccessRight] = Set())

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
    accessRights: Set[AccessRight] = Set())

  case class OAuthClient(id: String, secret: String, redirectUri: String)

  object Clients {
    val OAdmin = "schola"
  }

  object Gender extends Enumeration {
    val Male = Value
    val Female = Value
  }

  type Gender = Gender.Value

  case class ContactInfo(email: Option[String] = None, phoneNumber: Option[String] = None, fax: Option[String] = None)

  case class MobileNumbers(mobile1: Option[String] = None, mobile2: Option[String] = None)

  case class Contacts(mobiles: Option[MobileNumbers] = None, home: Option[ContactInfo] = None, work: Option[ContactInfo] = None)

  case class AddressInfo(city: Option[String] = None, country: Option[String] = None, postalCode: Option[String] = None, streetAddress: Option[String] = None)

  case class UsersStats(count: Int)

  case class AvatarInfo(filename: String, contentType: String, data: String, base64: Boolean)

  case class Profile(
    id: java.util.UUID,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    createdAt: Long,
    createdBy: Option[java.util.UUID],
    lastModifiedAt: Option[Long],
    lastModifiedBy: Option[java.util.UUID],
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts])

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
    stars: Int = 0,
    gender: Gender = Gender.Male,
    homeAddress: Option[AddressInfo] = None,
    workAddress: Option[AddressInfo] = None,
    contacts: Option[Contacts] = None,
    activationKey: Option[String] = None,
    _deleted: Boolean = false,
    suspended: Boolean = false,
    changePasswordAtNextLogin: Boolean = false,
    id: Option[java.util.UUID] = None,
    labels: List[String] = Nil,
    accessRights: List[AccessRight] = Nil)

  object U {
    val SuperUser =
      User(
        config.getString("root.primaryEmail"),
        Some(config.getString("root.password")),
        config.getString("root.givenName"),
        config.getString("root.familyName"), createdAt = 0L, createdBy = None,
        id = Some(java.util.UUID.fromString(config.getString("root.id"))))
  }

  case class Response(success: Boolean)

  case class Label(name: String, color: String)

  case class UserLabel(userId: java.util.UUID, label: String)

  // Modules

  case class App(name: String, scopes: Seq[String], accessRights: Seq[AccessRight] = Seq(), id: Option[java.util.UUID] = None)

  case class Scope(name: String, write: Boolean = true, trash: Boolean = true, purge: Boolean = false)

  case class AccessRight(name: String, appId: java.util.UUID, scopes: Seq[Scope], id: Option[java.util.UUID] = None)

  case class UserAccessRight(userId: java.util.UUID, accessRightId: java.util.UUID, grantedAt: Long = System.currentTimeMillis, grantedBy: Option[java.util.UUID] = None)
}