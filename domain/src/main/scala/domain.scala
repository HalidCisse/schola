package ma.epsilon.schola
package domain

object `package` {

  import java.time.{ LocalDateTime, Duration, Month, Instant }

  case class Upload(filename: String, contentType: Option[String], data: Array[Byte], attributes: Traversable[(String, String)])

  case class Session(
    key: String,
    secret: String,
    issuedTime: Instant,
    expiresIn: Option[Duration],
    refreshExpiresIn: Option[Duration],
    refresh: Option[String],
    lastAccessTime: LocalDateTime,
    superUser: Boolean,
    suspended: Boolean,
    changePasswordAtNextLogin: Boolean,
    user: Profile,
    userAgent: String,
    accessRights: Set[AccessRight] = Set(),
    activeAccessRight: Option[AccessRight] = None)

  case class OAuthToken(
    accessToken: String,
    userId: Uuid,
    refreshToken: Option[String],
    macKey: String,
    uA: String,
    expiresIn: Option[Duration],
    refreshExpiresIn: Option[Duration],
    createdAt: LocalDateTime = now,
    lastAccessTime: LocalDateTime = now,
    tokenType: String = "mac",
    accessRights: Set[AccessRight] = Set(),
    activeAccessRight: Option[Uuid] = None)

  case class OAuthClient(id: String, secret: String, redirectUri: String)

  object Clients {
    val OAdmin = "schola"
  }

  object Gender extends Enumeration {
    val Male, Female = Value
  }

  type Gender = Gender.Value

  case class ContactInfo(
    email: Option[String] = None,
    phoneNumber: Option[String] = None,
    fax: Option[String] = None)

  case class MobileNumbers(
    mobile1: Option[String] = None,
    mobile2: Option[String] = None)

  case class Contacts(
    mobiles: Option[MobileNumbers] = None,
    home: Option[ContactInfo] = None,
    work: Option[ContactInfo] = None,
    site: Option[String] = None)

  case class AddressInfo(
    city: Option[String] = None,
    country: Option[String] = None,
    postalCode: Option[String] = None,
    streetAddress: Option[String] = None)

  case class UsersStats(count: Int)

  case class AvatarInfo(filename: String, contentType: String, data: String, base64: Boolean)

  case class Profile(
    id: Uuid,
    cin: String,
    primaryEmail: String,
    givenName: String,
    familyName: String,
    jobTitle: String,
    createdAt: LocalDateTime,
    createdBy: Option[Uuid],
    lastModifiedAt: Option[LocalDateTime],
    lastModifiedBy: Option[Uuid],
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Option[Contacts])

  case class User(
    cin: String,
    primaryEmail: String,
    password: Option[String],
    givenName: String,
    familyName: String,
    jobTitle: String,
    createdAt: LocalDateTime = now,
    createdBy: Option[Uuid],
    lastLoginTime: Option[LocalDateTime] = None,
    lastModifiedAt: Option[LocalDateTime] = None,
    lastModifiedBy: Option[Uuid] = None,
    stars: Int = 0,
    gender: Gender = Gender.Male,
    homeAddress: Option[AddressInfo] = None,
    workAddress: Option[AddressInfo] = None,
    contacts: Option[Contacts] = None,
    activationKey: Option[String] = None,
    _deleted: Boolean = false,
    suspended: Boolean = false,
    changePasswordAtNextLogin: Boolean = false,
    id: Option[Uuid] = None /*,
    labels: List[String] = Nil,
    accessRights: List[AccessRight] = Nil*/ )

  object U {
    val SuperUser =
      User(
        "ROOT",
        config.getString("root.primaryEmail"),
        Some(config.getString("root.password")),
        config.getString("root.givenName"),
        config.getString("root.familyName"), "Super user", createdAt = LocalDateTime.of(1970, Month.JANUARY, 1, 0, 0, 0), createdBy = None,
        id = Some(Uuid(config.getString("root.id"))))
  }

  case class Response(success: Boolean)

  case class Label(name: String, color: String)

  case class UserLabel(userId: Uuid, label: String)

  // Modules

  case class App(name: String, scopes: List[String], accessRights: List[AccessRight] = List(), id: Option[Uuid] = None)

  case class Scope(name: String, write: Boolean = true, trash: Boolean = true, purge: Boolean = false /*, owners: Boolean = false*/ )

  case class AccessRight(alias: String, displayName: String, redirectUri: String, appId: Uuid, scopes: List[Scope], grantOptions: List[Uuid] = Nil, id: Option[Uuid] = None)

  case class UserAccessRight(userId: Uuid, accessRightId: Uuid, grantedAt: LocalDateTime = now, grantedBy: Option[Uuid] = None)
}