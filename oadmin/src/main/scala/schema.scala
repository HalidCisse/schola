package schola
package oadmin

package object schema {
  import Q._
  import domain._
  import conversions.jdbc._

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

    def * = (accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, createdAt, lastAccessTime, tokenType, scopes) <>(OAuthToken.tupled, OAuthToken.unapply)

    def user = foreignKey("TOKEN_USER_FK", userId, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.Cascade)

    def client = foreignKey("TOKEN_CLIENT_FK", clientId, OAuthClients)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade)
  }

  val OAuthTokens = TableQuery[OAuthTokens]

  implicit class OAuthTokensExtensions(val OAuthTokens: Query[OAuthTokens, OAuthToken]) extends AnyVal{
    def forInsert = OAuthTokens.map { t => (t.accessToken, t.clientId, t.redirectUri, t.userId, t.refreshToken, t.macKey, t.uA, t.expiresIn, t.refreshExpiresIn, t.createdAt, t.lastAccessTime, t.tokenType, t.scopes) }
  }

  class OAuthClients(tag: Tag) extends Table[OAuthClient](tag, "oauth_clients") {
    def id = column[String]("client_id")

    def secret = column[String]("client_secret")

    def redirectUri = column[String]("redirect_uri", O.NotNull)

    def * = (id, secret, redirectUri) <>(OAuthClient.tupled, OAuthClient.unapply)

    def pk = primaryKey("CLIENT_PK", (id, secret))

    def idx = index("CLIENT_CLIENT_ID_INDEX", id, unique = true)
  }

  val OAuthClients = TableQuery[OAuthClients]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[java.util.UUID]("id", O.DBType("uuid"), O.PrimaryKey)

    def email = column[String]("email", O.NotNull)

    def password = column[String]("password", O.NotNull, O.DBType("text"))

    def firstname = column[String]("firstname", O.NotNull)

    def lastname = column[String]("lastname", O.NotNull)

    def createdAt = column[Long]("created_at", O.NotNull)

    def createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def lastModifiedAt = column[Option[Long]]("last_modified_at")

    def lastModifiedBy = column[Option[java.util.UUID]]("last_modified_by", O.DBType("uuid"))

    def gender = column[Gender.Value]("gender", O.NotNull, O.Default(Gender.Male))

    def homeAddress = column[Option[AddressInfo]]("home_address", O.DBType("text"))

    def workAddress = column[Option[AddressInfo]]("work_address", O.DBType("text"))

    def contacts = column[Set[ContactInfo]]("contacts", O.DBType("text"))

    def avatar = column[Option[AvatarInfo]]("avatar", O.DBType("text"))

    def _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    def passwordValid = column[Boolean]("password_valid", O.NotNull, O.Default(false))

    def * = (id ?, email, password ?, firstname, lastname, createdAt, createdBy, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, avatar, _deleted, passwordValid) <>(User.tupled, User.unapply)

    def idx1 = index("USER_USERNAME_INDEX", email, unique = true)

    def idx2 = index("USER_USERNAME_PASSWORD_INDEX", (email, password))

    def _createdBy = foreignKey("USER_CREATOR_FK", createdBy, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.SetNull)
    
    def _lastModifiedBy = foreignKey("USER_MODIFIER_FK", createdBy, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.SetNull)
  }

  val Users = TableQuery[Users]

  implicit class UsersExtensions(val users: Query[Users, User]) extends AnyVal{
    def forInsert = Users.map { u => (u.id, u.email, u.password, u.firstname, u.lastname, u.createdAt, u.createdBy, u.lastModifiedAt, u.lastModifiedBy, u.gender, u.homeAddress, u.workAddress, u.contacts, u.passwordValid) }
    def forDeletion(id: String) = Users where(_.id is java.util.UUID.fromString(id)) map { _._deleted }

    // val autoInc = fname ~ lname returning id into { case (c, i) => Person(c._1, c._2, i) }

    // def insert(person: Person)(implicit session: Session): Person = {
    //   autoInc.insert(person.fname, person.lname)
    // }
  }

  class Roles(tag: Tag) extends Table[Role](tag, "roles") {
    def name = column[String]("name", O.PrimaryKey)

    def parent = column[Option[String]]("parent")

    def createdAt = column[Long]("created_at", O.NotNull)

    def createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    def public = column[Boolean]("public", O.NotNull, O.Default(true))

    def * = (name, parent, createdAt, createdBy, public) <>(Role.tupled, Role.unapply)

    def idx = index("ROLE_NAME_INDEX", name, unique = true)

    def _createdBy = foreignKey("ROLE_USER_FK", createdBy, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.SetNull)

    def _parent = foreignKey("ROLE_PARENT_ROLE_FK", parent, Roles)(_.name, scala.slick.lifted.ForeignKeyAction.Cascade)
  }

  val Roles = TableQuery[Roles]

  class Permissions(tag: Tag) extends Table[Permission](tag, "permissions") {
    def name = column[String]("name", O.PrimaryKey)

    def clientId = column[String]("client_id", O.NotNull)

    def client = foreignKey("PERMISSION_CLIENT_FK", clientId, OAuthClients)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade)

    def * = (name, clientId) <>(Permission.tupled, Permission.unapply)

    def idx = index("PERMISSION_NAME_INDEX", name, unique = true)
  }

  val Permissions = TableQuery[Permissions]

  class RolesPermissions(tag: Tag) extends Table[RolePermission](tag, "roles_permissions") {
    def role = column[String]("role", O.NotNull)

    def permission = column[String]("permission", O.NotNull)

    def grantedAt = column[Long]("granted_at", O.NotNull)

    def grantedBy = column[Option[java.util.UUID]]("granted_by", O.DBType("uuid"))

    def * = (role, permission, grantedAt, grantedBy) <>(RolePermission.tupled, RolePermission.unapply)

    def _role = foreignKey("ROLE_PERMISSION_ROLE_FK", role, Roles)(_.name, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.Restrict)

    def user = foreignKey("ROLE_PERMISSION_GRANTOR_FK", grantedBy, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.SetNull)

    def _permission = foreignKey("ROLE_PERMISSION_PERMISSION_FK", permission, Permissions)(_.name)

    def pk = primaryKey("ROLE_PERMISSION_PK", (role, permission))
  }

  val RolesPermissions = TableQuery[RolesPermissions]

  class UsersRoles(tag: Tag) extends Table[UserRole](tag, "users_roles") {
    def userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    def role = column[String]("role", O.NotNull)

    def grantedAt = column[Long]("granted_at", O.NotNull)

    def grantedBy = column[Option[java.util.UUID]]("granted_by", O.DBType("uuid"))

    def * = (userId, role, grantedAt, grantedBy) <>(UserRole.tupled, UserRole.unapply)

    def user = foreignKey("USER_ROLE_USER_FK", userId, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.Restrict)

    def _user = foreignKey("USER_ROLE_USER_GRANTOR_FK", grantedBy, Users)(_.id, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.SetNull)

    def _role = foreignKey("USER_ROLE_ROLE_FK", role, Roles)(_.name, scala.slick.lifted.ForeignKeyAction.Cascade, scala.slick.lifted.ForeignKeyAction.Cascade)

    def pk = primaryKey("USER_ROLE_PK", (userId, role))
  }

  val UsersRoles = TableQuery[UsersRoles]
}