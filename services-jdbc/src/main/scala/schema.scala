package schola
package oadmin
package schema

object `package` {
  val Q = scala.slick.driver.PostgresDriver.simple

  import Q._

  import domain._
  import conversions.jdbc._

  import scala.slick.model.ForeignKeyAction

  class OAuthTokens(tag: Tag) extends Table[OAuthToken](tag, "oauth_tokens") {
    def accessToken = column[String]("access_token", O.PrimaryKey)

    def clientId = column[String]("client_id", O.NotNull)

    def redirectUri = column[String]("redirect_uri", O.NotNull)

    def userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    def refreshToken = column[Option[String]]("refresh_token")

    def macKey = column[String]("secret", O.NotNull)

    def uA = column[String]("user_agent", O.NotNull, O.DBType("text"))

    def expiresIn = column[Option[Long]]("expires_in")

    def refreshExpiresIn = column[Option[Long]]("refresh_expires_in")

    def createdAt = column[Long]("created_at")

    def lastAccessTime = column[Long]("last_access_time")

    def tokenType = column[String]("token_type", O.NotNull, O.Default("mac"))

    def scopes = column[Set[String]]("scopes", O.NotNull, O.Default(Set()))

    def * = (accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, createdAt, lastAccessTime, tokenType, scopes) <> (OAuthToken.tupled, OAuthToken.unapply)

    def user = foreignKey("TOKEN_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    def client = foreignKey("TOKEN_CLIENT_FK", clientId, OAuthClients)(_.id, ForeignKeyAction.Cascade)
  }

  val OAuthTokens = TableQuery[OAuthTokens]

  implicit class OAuthTokensExtensions(OAuthTokens: Query[OAuthTokens, OAuthToken]) {
    val forInsert =
      OAuthTokens.map { t => (t.accessToken, t.clientId, t.redirectUri, t.userId, t.refreshToken, t.macKey, t.uA, t.expiresIn, t.refreshExpiresIn, t.createdAt, t.lastAccessTime, t.tokenType, t.scopes) }
  }

  class OAuthClients(tag: Tag) extends Table[OAuthClient](tag, "oauth_clients") {
    def id = column[String]("client_id")

    def secret = column[String]("client_secret")

    def redirectUri = column[String]("redirect_uri", O.NotNull)

    def * = (id, secret, redirectUri) <> (OAuthClient.tupled, OAuthClient.unapply)

    def pk = primaryKey("CLIENT_PK", (id, secret))

    def idx = index("CLIENT_CLIENT_ID_INDEX", id, unique = true)
  }

  val OAuthClients = TableQuery[OAuthClients]

  private[this] val user = (
    primaryEmail: String,
    password: Option[String],
    givenName: String,
    familyName: String,
    createdAt: Long,
    createdBy: Option[java.util.UUID],
    lastLoginTime: Option[Long],
    lastModifiedAt: Option[Long],
    lastModifiedBy: Option[java.util.UUID],
    gender: Gender,
    homeAddress: Option[AddressInfo],
    workAddress: Option[AddressInfo],
    contacts: Contacts,
    avatar: Option[String],
    activationKey: Option[String],
    _deleted: Boolean,
    suspended: Boolean,
    changePasswordAtNextLogin: Boolean,
    id: Option[java.util.UUID]) =>
    User(primaryEmail,
      password,
      givenName,
      familyName,
      createdAt,
      createdBy,
      lastLoginTime,
      lastModifiedAt,
      lastModifiedBy,
      gender,
      homeAddress,
      workAddress,
      contacts,
      avatar,
      activationKey,
      _deleted,
      suspended,
      changePasswordAtNextLogin,
      id)

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[java.util.UUID]("id", O.DBType("uuid"), O.PrimaryKey)

    def primaryEmail = column[String]("primary_email", O.NotNull)

    def password = column[String]("password", O.NotNull, O.DBType("text"))

    def givenName = column[String]("given_name", O.NotNull)

    def familyName = column[String]("family_name", O.NotNull)

    def createdAt = column[Long]("created_at", O.NotNull)

    def createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def lastLoginTime = column[Option[Long]]("last_login_time")

    def lastModifiedAt = column[Option[Long]]("last_modified_at")

    def lastModifiedBy = column[Option[java.util.UUID]]("last_modified_by", O.DBType("uuid"))

    def gender = column[Gender.Value]("gender", O.NotNull, O.Default(Gender.Male))

    def homeAddress = column[Option[AddressInfo]]("home_address", O.DBType("text"))

    def workAddress = column[Option[AddressInfo]]("work_address", O.DBType("text"))

    def contacts = column[Contacts]("contacts", O.DBType("text"))

    def avatar = column[Option[String]]("avatar")

    def activationKey = column[Option[String]]("user_activation_key", O.DBType("text"))

    def _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    def suspended = column[Boolean]("suspended", O.NotNull, O.Default(false))

    def changePasswordAtNextLogin = column[Boolean]("change_password_at_next_login", O.NotNull, O.Default(false))

    def * = (primaryEmail, password?, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, activationKey, _deleted, suspended, changePasswordAtNextLogin, id?) <> (user.tupled, (user: User) => Some(user.primaryEmail, user.password, user.givenName, user.familyName, user.createdAt, user.createdBy, user.lastLoginTime, user.lastModifiedAt, user.lastModifiedBy, user.gender, user.homeAddress, user.workAddress, user.contacts, user.avatar, user.activationKey, user._deleted, user.suspended, user.changePasswordAtNextLogin, user.id))

    def idx1 = index("USER_USERNAME_INDEX", primaryEmail, unique = true)

    def idx2 = index("USER_USERNAME_PASSWORD_INDEX", (primaryEmail, password))

    def _createdBy = foreignKey("USER_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    def _lastModifiedBy = foreignKey("USER_MODIFIER_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)
  }

  val Users = TableQuery[Users]

  private val usersAutoGenId =
    Users.map(
      u => (u.primaryEmail, u.password, u.givenName, u.familyName, u.createdAt, u.createdBy, u.lastModifiedAt, u.lastModifiedBy, u.gender, u.homeAddress, u.workAddress, u.contacts, u.changePasswordAtNextLogin)) returning Users.map(_.id) into { case (u, id) => User(u._1, Some(u._2), u._3, u._4, u._5, u._6, None, u._7, u._8, u._9, u._10, u._11, u._12, changePasswordAtNextLogin = u._13, id = Some(id)) }

  private val cForDeletion = {
    def getDeleted(id: Column[java.util.UUID]) = Users where (u => (u.id isNot U.SuperUser.id) && (u.id is id)) map { _._deleted }

    Compiled(getDeleted _)
  }

  implicit class UsersExtensions(val users: Query[Users, User]) extends AnyVal {

    def insert(email: String, password: String, givenName: String, familyName: String, createdAt: Long = System.currentTimeMillis, createdBy: Option[java.util.UUID] = None, lastModifiedAt: Option[Long] = None, lastModifiedBy: Option[java.util.UUID] = None, gender: Gender = Gender.Male, homeAddress: Option[domain.AddressInfo] = None, workAddress: Option[domain.AddressInfo] = None, contacts: domain.Contacts = domain.Contacts(MobileNumbers(None, None), None, None), changePasswordAtNextLogin: Boolean = false)(implicit session: Q.Session): User =
      usersAutoGenId.insert(email, password, givenName, familyName, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin)

    def forDeletion = cForDeletion
  }

  class Roles(tag: Tag) extends Table[Role](tag, "roles") {
    def name = column[String]("name", O.PrimaryKey)

    def parent = column[Option[String]]("parent")

    def createdAt = column[Long]("created_at", O.NotNull)

    def createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def publiq = column[Boolean]("public", O.NotNull, O.Default(true))

    def * = (name, parent, createdAt, createdBy, publiq) <> (Role.tupled, Role.unapply)

    def idx = index("ROLE_NAME_INDEX", name, unique = true)

    def _createdBy = foreignKey("ROLE_USER_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    def _parent = foreignKey("ROLE_PARENT_ROLE_FK", parent, Roles)(_.name, ForeignKeyAction.Cascade)
  }

  val Roles = TableQuery[Roles]

  class Permissions(tag: Tag) extends Table[Permission](tag, "permissions") {
    def name = column[String]("name", O.PrimaryKey)

    def clientId = column[String]("client_id", O.NotNull)

    def client = foreignKey("PERMISSION_CLIENT_FK", clientId, OAuthClients)(_.id, ForeignKeyAction.Cascade)

    def * = (name, clientId) <> (Permission.tupled, Permission.unapply)

    def idx = index("PERMISSION_NAME_INDEX", name, unique = true)
  }

  val Permissions = TableQuery[Permissions]

  class RolesPermissions(tag: Tag) extends Table[RolePermission](tag, "roles_permissions") {
    def role = column[String]("role", O.NotNull)

    def permission = column[String]("permission", O.NotNull)

    def grantedAt = column[Long]("granted_at", O.NotNull)

    def grantedBy = column[Option[java.util.UUID]]("granted_by", O.DBType("uuid"))

    def * = (role, permission, grantedAt, grantedBy) <> (RolePermission.tupled, RolePermission.unapply)

    def _role = foreignKey("ROLE_PERMISSION_ROLE_FK", role, Roles)(_.name, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    def user = foreignKey("ROLE_PERMISSION_GRANTOR_FK", grantedBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    def _permission = foreignKey("ROLE_PERMISSION_PERMISSION_FK", permission, Permissions)(_.name)

    def pk = primaryKey("ROLE_PERMISSION_PK", (role, permission))
  }

  val RolesPermissions = TableQuery[RolesPermissions]

  private[this] val userRole = (userId: java.util.UUID, role: String, grantedAt: Long, grantedBy: Option[java.util.UUID]) => UserRole(userId, role, grantedAt, grantedBy)

  class UsersRoles(tag: Tag) extends Table[UserRole](tag, "users_roles") {
    def userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    def role = column[String]("role", O.NotNull)

    def grantedAt = column[Long]("granted_at", O.NotNull)

    def grantedBy = column[Option[java.util.UUID]]("granted_by", O.DBType("uuid"))

    def * = (userId, role, grantedAt, grantedBy) <> (userRole.tupled, (ur: UserRole) => Some(ur.userId, ur.role, ur.grantedAt, ur.grantedBy))

    def user = foreignKey("USER_ROLE_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Restrict)

    def _user = foreignKey("USER_ROLE_USER_GRANTOR_FK", grantedBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    def _role = foreignKey("USER_ROLE_ROLE_FK", role, Roles)(_.name, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    def pk = primaryKey("USER_ROLE_PK", (userId, role))
  }

  val UsersRoles = TableQuery[UsersRoles]

  class Labels(tag: Tag) extends Table[Label](tag, "labels") {
    def name = column[String]("name", O.NotNull)

    def color = column[String]("color", O.NotNull)

    def * = (name, color) <> (Label.tupled, Label.unapply)

    def pk = primaryKey("LABEL_PK", name)
  }

  val Labels = TableQuery[Labels]

  private val labelsInsert =
    Labels returning Labels.map(_.name) into { case (l, label) => Label(label, l.color) }

  implicit class LabelsExtensions(val labels: Query[Labels, Label]) extends AnyVal {
    def insert(label: String, color: String)(implicit session: Q.Session): Label = labelsInsert.insert(Label(label, color))
  }  

  class UsersLabels(tag: Tag) extends Table[UserLabel](tag, "users_labels") {
    def userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    def label = column[String]("label", O.NotNull)

    def * = (userId, label) <> (UserLabel.tupled, UserLabel.unapply)

    def pk = primaryKey("USER_LABEL_PK", (userId, label))

    def user = foreignKey("USER_LABEL_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    def _label = foreignKey("USER_LABEL_LABEL_FK", label, Labels)(_.name, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)
  }

  val UsersLabels = TableQuery[UsersLabels]
}