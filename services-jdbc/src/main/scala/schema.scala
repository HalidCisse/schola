package ma.epsilon.schola
package schema

object `package` {

  val Q = {
    Class.forName("org.postgresql.Driver")
    scala.slick.driver.PostgresDriver.simple
  }

  import Q._

  import domain._
  import conversions.jdbc._

  import scala.slick.model.ForeignKeyAction

  class Users(tag: Tag) extends Table[User](tag, "users") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val primaryEmail = column[String]("primary_email", O.NotNull)

    val password = column[String]("password", O.NotNull, O.DBType("text"))

    val givenName = column[String]("given_name", O.NotNull)

    val familyName = column[String]("family_name", O.NotNull)

    val createdAt = column[Long]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    val lastLoginTime = column[Option[Long]]("last_login_time")

    val lastModifiedAt = column[Option[Long]]("last_modified_at")

    val lastModifiedBy = column[Option[java.util.UUID]]("last_modified_by", O.DBType("uuid"))

    val gender = column[Gender]("gender", O.NotNull, O.Default(Gender.Male))

    val homeAddress = column[Option[AddressInfo]]("home_address", O.DBType("text"))

    val workAddress = column[Option[AddressInfo]]("work_address", O.DBType("text"))

    val contacts = column[Option[Contacts]]("contacts", O.DBType("text"))

    val activationKey = column[Option[String]]("user_activation_key", O.DBType("text"))

    val _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    val suspended = column[Boolean]("suspended", O.NotNull, O.Default(false))

    val changePasswordAtNextLogin = column[Boolean]("change_password_at_next_login", O.NotNull, O.Default(false))

    def * = (primaryEmail, password?, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, gender, homeAddress, workAddress, contacts, activationKey, _deleted, suspended, changePasswordAtNextLogin, id?) <> ({ t: (String, Option[String], String, String, Long, Option[java.util.UUID], Option[Long], Option[Long], Option[java.util.UUID], Gender, Option[AddressInfo], Option[AddressInfo], Option[Contacts], Option[String], Boolean, Boolean, Boolean, Option[java.util.UUID]) =>
      t match {
        case (
          primaryEmail,
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
          activationKey,
          _deleted,
          suspended,
          changePasswordAtNextLogin,
          id) =>
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
            activationKey,
            _deleted,
            suspended,
            changePasswordAtNextLogin,
            id)
      }
    }, (user: User) => Some(user.primaryEmail, user.password, user.givenName, user.familyName, user.createdAt, user.createdBy, user.lastLoginTime, user.lastModifiedAt, user.lastModifiedBy, user.gender, user.homeAddress, user.workAddress, user.contacts, user.activationKey, user._deleted, user.suspended, user.changePasswordAtNextLogin, user.id))

    lazy val _createdBy = foreignKey("USER_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    lazy val _lastModifiedBy = foreignKey("USER_MODIFIER_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val indexPrimaryEmail = index("USER_USERNAME_INDEX", primaryEmail, unique = true)

    val pk = primaryKey("USER_PK", id)
  }

  val Users = new TableQuery[Users](new Users(_)) {

    private val insertInvoker = {
      val usersInserts =
        this returning this.map(_.id) into {
          case (newUser, id) =>
            User(
              newUser.primaryEmail,
              newUser.password,
              newUser.givenName,
              newUser.familyName,
              newUser.createdAt,
              newUser.createdBy,
              None,
              newUser.lastModifiedAt,
              newUser.lastModifiedBy,
              newUser.gender,
              newUser.homeAddress,
              newUser.workAddress,
              newUser.contacts,
              changePasswordAtNextLogin = newUser.changePasswordAtNextLogin, id = Some(id))
        }

      usersInserts.insertInvoker
    }

    private val forDeletion = {
      def getDeleted(id: Column[java.util.UUID]) =
        this
          .where(user => (user.id isNot U.SuperUser.id) && (user.id is id))
          .map(_._deleted)

      Compiled(getDeleted _)
    }

    def insert(email: String, password: String, givenName: String, familyName: String, createdAt: Long = System.currentTimeMillis, createdBy: Option[java.util.UUID] = None, lastModifiedAt: Option[Long] = None, lastModifiedBy: Option[java.util.UUID] = None, gender: Gender = Gender.Male, homeAddress: Option[domain.AddressInfo] = None, workAddress: Option[domain.AddressInfo] = None, contacts: Option[domain.Contacts] = None, changePasswordAtNextLogin: Boolean = false)(implicit session: Q.Session): User =
      insertInvoker insert User(email, Some(password), givenName, familyName, createdAt, createdBy, lastModifiedAt, lastModifiedBy = lastModifiedBy, gender = gender, homeAddress = homeAddress, workAddress = workAddress, contacts = contacts, changePasswordAtNextLogin = changePasswordAtNextLogin)

    def delete(id: String)(implicit session: Q.Session) =
      forDeletion(uuid(id)).update(true) == 1
  }

  class OAuthClients(tag: Tag) extends Table[OAuthClient](tag, "oauth_clients") {

    val id = column[String]("client_id")

    val secret = column[String]("client_secret")

    val redirectUri = column[String]("redirect_uri", O.NotNull)

    def * = (id, secret, redirectUri) <> (OAuthClient.tupled, OAuthClient.unapply)

    val indexId = index("CLIENT_CLIENT_ID_INDEX", id, unique = true)

    val pk = primaryKey("CLIENT_PK", (id, secret))
  }

  val OAuthClients = TableQuery[OAuthClients]

  class OAuthTokens(tag: Tag) extends Table[OAuthToken](tag, "oauth_tokens") {

    val accessToken = column[String]("access_token", O.NotNull)

    val clientId = column[String]("client_id", O.NotNull)

    val redirectUri = column[String]("redirect_uri", O.NotNull)

    val userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    val refreshToken = column[Option[String]]("refresh_token")

    val macKey = column[String]("secret", O.NotNull)

    val uA = column[String]("user_agent", O.NotNull, O.DBType("text"))

    val expiresIn = column[Option[Long]]("expires_in")

    val refreshExpiresIn = column[Option[Long]]("refresh_expires_in")

    val createdAt = column[Long]("created_at")

    val lastAccessTime = column[Long]("last_access_time")

    val tokenType = column[String]("token_type", O.NotNull, O.Default("mac"))

    val accessRights = column[Set[AccessRight]]("access_rights", O.NotNull, O.Default(Set()), O.DBType("text"))

    def * = (accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, createdAt, lastAccessTime, tokenType, accessRights) <> (OAuthToken.tupled, OAuthToken.unapply)

    val user = foreignKey("TOKEN_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val client = foreignKey("TOKEN_CLIENT_FK", clientId, OAuthClients)(_.id, ForeignKeyAction.Cascade)

    val indexRefreshToken = index("TOKEN_REFRESH_TOKEN_INDEX", refreshToken)

    val pk = primaryKey("TOKEN_PK", accessToken)
  }

  val OAuthTokens = new TableQuery[OAuthTokens](new OAuthTokens(_)) {

    private val insertInvoker = {
      val oauthTokensInserts =
        this returning this.map(_.accessToken) into {
          case (token, _) =>
            OAuthToken(
              token.accessToken,
              token.clientId,
              token.redirectUri,
              token.userId,
              token.refreshToken,
              token.macKey,
              token.uA,
              token.expiresIn,
              token.refreshExpiresIn,
              token.createdAt,
              token.lastAccessTime,
              token.tokenType,
              token.accessRights)
        }

      oauthTokensInserts.insertInvoker
    }

    def insert(accessToken: String, clientId: String, redirectUri: String, userId: java.util.UUID, refreshToken: Option[String], macKey: String, uA: String, expiresIn: Option[Long], refreshExpiresIn: Option[Long], createdAt: Long, lastAccessTime: Long, tokenType: String, accessRights: Set[AccessRight])(implicit session: Q.Session): OAuthToken =
      insertInvoker insert OAuthToken(accessToken, clientId, redirectUri, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, createdAt, lastAccessTime, tokenType, accessRights)
  }

  /*  class Roles(tag: Tag) extends Table[Role](tag, "roles") {

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

  val UsersRoles = TableQuery[UsersRoles]*/

  class Apps(tag: Tag) extends Table[App](tag, "apps") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val scopes = column[Seq[String]]("scopes", O.DBType("text"), O.NotNull, O.Default(Seq()))

    def * = (name, scopes, id?) <> ({ t: (String, Seq[String], Option[java.util.UUID]) => t match { case (name, scopes, id) => App(name, scopes, id = id) } }, (app: App) => Option((app.name, app.scopes, app.id)))

    val indexName = index("APP_NAME_INDEX", name, unique = true)

    val pk = primaryKey("APP_PK", id)
  }

  val Apps = new TableQuery[Apps](new Apps(_)) {

    private val insertInvoker = {
      val appsInserts =
        this returning this.map(_.id) into { case (app, id) => App(app.name, app.scopes, app.accessRights, id = Some(id)) }

      appsInserts.insertInvoker
    }

    def insert(name: String, scopes: Seq[String])(implicit session: Q.Session): App = insertInvoker insert App(name, scopes)

    def delete(id: String)(implicit session: Q.Session) =
      this
        .where(_.id is uuid(id))
        .delete
  }

  class AccessRights(tag: Tag) extends Table[AccessRight](tag, "access_rights") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val appId = column[java.util.UUID]("app_id", O.NotNull, O.DBType("uuid"))

    val scopes = column[Seq[Scope]]("scopes", O.DBType("text"), O.NotNull, O.Default(Seq()))

    def * = (name, appId, scopes, id?) <> (AccessRight.tupled, AccessRight.unapply)

    val app = foreignKey("ACCESS_RIGHT_APP_FK", appId, Apps)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val indexName = index("ACCESS_RIGHT_NAME_INDEX", name, unique = true)

    val pk = primaryKey("ACCESS_RIGHT_PK", id)
  }

  val AccessRights = new TableQuery[AccessRights](new AccessRights(_)) {

    private val insertInvoker = {
      val accessRightsInserts =
        this returning this.map(_.id) into { case (accessRight, id) => AccessRight(accessRight.name, accessRight.appId, accessRight.scopes, id = Some(id)) }

      accessRightsInserts.insertInvoker
    }

    def insert(name: String, appId: String, scopes: Seq[Scope])(implicit session: Q.Session): AccessRight = insertInvoker insert AccessRight(name, uuid(appId), scopes)

    def delete(id: String)(implicit session: Q.Session) =
      this
        .where(_.id is uuid(id))
        .delete
  }

  class UsersAccessRights(tag: Tag) extends Table[UserAccessRight](tag, "users_access_rights") {

    val userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    val accessRightId = column[java.util.UUID]("access_right_id", O.NotNull, O.DBType("uuid"))

    val grantedAt = column[Long]("granted_at", O.NotNull)

    val grantedBy = column[Option[java.util.UUID]]("granted_by", O.DBType("uuid"))

    def * = (userId, accessRightId, grantedAt, grantedBy) <> (UserAccessRight.tupled, UserAccessRight.unapply)

    val user = foreignKey("USER_ACCESS_RIGHT_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val _user = foreignKey("USER_ACCESS_RIGHT_USER_GRANTOR_FK", grantedBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val accessRight = foreignKey("USER_ACCESS_RIGHT_ACCESS_RIGHT_FK", accessRightId, AccessRights)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val pk = primaryKey("USER_ACCESS_RIGHT_PK", (userId, accessRightId))
  }

  val UsersAccessRights = TableQuery[UsersAccessRights]

  class Labels(tag: Tag) extends Table[Label](tag, "labels") {

    val name = column[String]("name", O.NotNull)

    val color = column[String]("color", O.NotNull)

    def * = (name, color) <> (Label.tupled, Label.unapply)

    val pk = primaryKey("LABEL_PK", name)
  }

  val Labels = new TableQuery[Labels](new Labels(_)) {

    private val insertInvoker = {
      val labelsInserts =
        this returning this.map(_.name) into { case (label, name) => Label(name, label.color) }

      labelsInserts.insertInvoker
    }

    def insert(name: String, color: String)(implicit session: Q.Session): Label = insertInvoker insert Label(name, color)
  }

  class UsersLabels(tag: Tag) extends Table[UserLabel](tag, "users_labels") {

    val userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    val label = column[String]("label", O.NotNull)

    def * = (userId, label) <> (UserLabel.tupled, UserLabel.unapply)

    val user = foreignKey("USER_LABEL_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val _label = foreignKey("USER_LABEL_LABEL_FK", label, Labels)(_.name, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val pk = primaryKey("USER_LABEL_PK", (userId, label))
  }

  val UsersLabels = TableQuery[UsersLabels]
}