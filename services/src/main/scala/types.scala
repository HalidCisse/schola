package ma.epsilon.schola

/*import domain._

object Types {

  import java.time.{LocalDateTime, Duration, Instant}

  type UserLike = {
    val id: Option[Uuid]
    val cin: String
    val primaryEmail: String
    val password: Option[String]
    val givenName: String
    val familyName: String
    val createdAt: LocalDateTime
    val createdBy: Option[Uuid]
    val lastLoginTime: Option[LocalDateTime]
    val lastModifiedAt: Option[LocalDateTime]
    val lastModifiedBy: Option[Uuid]
    val gender: domain.Gender
    val homeAddress: Option[domain.AddressInfo]
    val workAddress: Option[domain.AddressInfo]
    val contacts: Option[domain.Contacts]
    val _deleted: Boolean
    val suspended: Boolean
    val changePasswordAtNextLogin: Boolean
    val labels: List[String]
    val accessRights: List[AccessRight]
  }

  type ProfileLike = {
    val id: Uuid
    val cin: String
    val primaryEmail: String
    val givenName: String
    val familyName: String
    val createdAt: LocalDateTime
    val createdBy: Option[Uuid]
    val lastModifiedAt: Option[LocalDateTime]
    val lastModifiedBy: Option[Uuid]
    val gender: domain.Gender
    val homeAddress: Option[domain.AddressInfo]
    val workAddress: Option[domain.AddressInfo]
    val contacts: Option[domain.Contacts]
  }

  type UserLabelLike = {
    val userId: Uuid
    val label: String
  }

  type StatsLike = {
    val count: Int
  }

  //

  type TokenLike = {
    val accessToken: String
    val userId: Uuid
    val refreshToken: Option[String]
    val macKey: String
    val uA: String
    val expiresIn: Option[Duration]
    val refreshExpiresIn: Option[Duration]
    val createdAt: LocalDateTime
    val lastAccessTime: LocalDateTime
    val tokenType: String
    val accessRights: Set[AccessRight]
    val activeAccessRight: Option[Uuid]
  }

  type ClientLike = {
    val id: String
    val secret: String
    val redirectUri: String
  }

  type SessionLike = {
    val key: String
    val secret: String
    val issuedTime: Instant
    val expiresIn: Option[Duration]
    val refreshExpiresIn: Option[Duration]
    val refresh: Option[String]
    val lastAccessTime: LocalDateTime
    val superUser: Boolean
    val suspended: Boolean
    val changePasswordAtNextLogin: Boolean
    val user: ProfileLike
    val userAgent: String
    val accessRights: Set[AccessRight]
    val activeAccessRight: Option[AccessRight]
  }

  // 

  type LabelLike = {
    val name: String
    val color: String
  }
}
*/ 