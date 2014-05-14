package ma.epsilon.schola
package schema

object `package` {

  import jdbc.Q._

  import domain._
  import conversions.jdbc._

  import java.time.{LocalDateTime, Duration}

  import scala.slick.model.ForeignKeyAction

  class Users(tag: Tag) extends Table[User](tag, "users") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val cin = column[String]("cin", O.NotNull)

    val primaryEmail = column[String]("primary_email", O.NotNull)

    val password = column[String]("password", O.NotNull, O.DBType("text"))

    val givenName = column[String]("given_name", O.NotNull)

    val familyName = column[String]("family_name", O.NotNull)

    val createdAt = column[LocalDateTime]("created_at", O.NotNull)

    val createdBy = column[Option[java.util.UUID]]("created_by", O.DBType("uuid"))

    val lastLoginTime = column[Option[LocalDateTime]]("last_login_time")

    val lastModifiedAt = column[Option[LocalDateTime]]("last_modified_at")

    val lastModifiedBy = column[Option[java.util.UUID]]("last_modified_by", O.DBType("uuid"))

    val stars = column[Int]("stars", O.Default(0))

    val gender = column[Gender]("gender", O.NotNull, O.Default(Gender.Male))

    val homeAddress = column[Option[AddressInfo]]("home_address", O.DBType("json"))

    val workAddress = column[Option[AddressInfo]]("work_address", O.DBType("json"))

    val contacts = column[Option[Contacts]]("contacts", O.DBType("json"))

    val activationKey = column[Option[String]]("user_activation_key", O.DBType("text"))

    val _deleted = column[Boolean]("_deleted", O.NotNull, O.Default(false))

    val suspended = column[Boolean]("suspended", O.NotNull, O.Default(false))

    val changePasswordAtNextLogin = column[Boolean]("change_password_at_next_login", O.NotNull, O.Default(false))

    def * = (cin, primaryEmail, password?, givenName, familyName, createdAt, createdBy, lastLoginTime, lastModifiedAt, lastModifiedBy, stars, gender, homeAddress, workAddress, contacts, activationKey, _deleted, suspended, changePasswordAtNextLogin, id?) <> ({ t: (String, String, Option[String], String, String, LocalDateTime, Option[java.util.UUID], Option[LocalDateTime], Option[LocalDateTime], Option[java.util.UUID], Int, Gender, Option[AddressInfo], Option[AddressInfo], Option[Contacts], Option[String], Boolean, Boolean, Boolean, Option[java.util.UUID]) =>
      t match {
        case (
          cin,
          primaryEmail,
          password,
          givenName,
          familyName,
          createdAt,
          createdBy,
          lastLoginTime,
          lastModifiedAt,
          lastModifiedBy,
          stars,
          gender,
          homeAddress,
          workAddress,
          contacts,
          activationKey,
          _deleted,
          suspended,
          changePasswordAtNextLogin,
          id) =>
          User(
            cin,
            primaryEmail,
            password,
            givenName,
            familyName,
            createdAt,
            createdBy,
            lastLoginTime,
            lastModifiedAt,
            lastModifiedBy,
            stars,
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
    }, (user: User) => Some(user.cin, user.primaryEmail, user.password, user.givenName, user.familyName, user.createdAt, user.createdBy, user.lastLoginTime, user.lastModifiedAt, user.lastModifiedBy, user.stars, user.gender, user.homeAddress, user.workAddress, user.contacts, user.activationKey, user._deleted, user.suspended, user.changePasswordAtNextLogin, user.id))

    lazy val _createdBy = foreignKey("USER_CREATOR_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    lazy val _lastModifiedBy = foreignKey("USER_MODIFIER_FK", createdBy, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.SetNull)

    val indexPrimaryEmail = index("USER_USERNAME_INDEX", primaryEmail, unique = true)

    val indexCIN = index("USER_CIN_INDEX", cin, unique = true)

    val pk = primaryKey("USER_PK", id)
  }

  val Users = new TableQuery(new Users(_)) {

    private val insertInvoker = {
      val usersInserts =
        this returning this.map(_.id) into {
          case (newUser, id) =>
            User(
              newUser.cin,
              newUser.primaryEmail,
              newUser.password,
              newUser.givenName,
              newUser.familyName,
              newUser.createdAt,
              newUser.createdBy,
              None,
              newUser.lastModifiedAt,
              newUser.lastModifiedBy,
              newUser.stars,
              newUser.gender,
              newUser.homeAddress,
              newUser.workAddress,
              newUser.contacts,
              suspended = newUser.suspended,
              changePasswordAtNextLogin = newUser.changePasswordAtNextLogin, id = Some(id))
        }

      usersInserts.insertInvoker
    }

    private val forDeletion = {
      def getDeleted(id: Column[java.util.UUID]) =
        this
          .filter(user => (user.id =!= U.SuperUser.id) && (user.id === id))
          .map(_._deleted)

      Compiled(getDeleted _)
    }

    def insert(cin: String, email: String, password: String, givenName: String, familyName: String, createdAt: LocalDateTime = LocalDateTime.now, createdBy: Option[java.util.UUID] = None, lastModifiedAt: Option[LocalDateTime] = None, lastModifiedBy: Option[java.util.UUID] = None, gender: Gender = Gender.Male, homeAddress: Option[domain.AddressInfo] = None, workAddress: Option[domain.AddressInfo] = None, contacts: Option[domain.Contacts] = None, suspended: Boolean = false, changePasswordAtNextLogin: Boolean = false)(implicit session: jdbc.Q.Session): User =
      insertInvoker insert User(cin, email, Some(password), givenName, familyName, createdAt, createdBy, lastModifiedAt, lastModifiedBy = lastModifiedBy, gender = gender, homeAddress = homeAddress, workAddress = workAddress, contacts = contacts, suspended = suspended, changePasswordAtNextLogin = changePasswordAtNextLogin)

    def delete(id: String)(implicit session: jdbc.Q.Session) =
      forDeletion(uuid(id)).update(true) == 1
  }

/*  class OAuthClients(tag: Tag) extends Table[OAuthClient](tag, "oauth_clients") {

    val id = column[String]("client_id")

    val secret = column[String]("client_secret")

    val redirectUri = column[String]("redirect_uri", O.NotNull)

    def * = (id, secret, redirectUri) <> (OAuthClient.tupled, OAuthClient.unapply)

    val indexId = index("CLIENT_CLIENT_ID_INDEX", id, unique = true)

    val pk = primaryKey("CLIENT_PK", (id, secret))
  }

  val OAuthClients = TableQuery[OAuthClients]*/

  class OAuthTokens(tag: Tag) extends Table[OAuthToken](tag, "oauth_tokens") {

    val accessToken = column[String]("access_token", O.NotNull)

    // val clientId = column[String]("client_id", O.NotNull)

    // val redirectUri = column[String]("redirect_uri", O.NotNull)

    val userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    val refreshToken = column[Option[String]]("refresh_token")

    val macKey = column[String]("secret", O.NotNull)

    val uA = column[String]("user_agent", O.NotNull, O.DBType("text"))

    val expiresIn = column[Option[Duration]]("expires_in")

    val refreshExpiresIn = column[Option[Duration]]("refresh_expires_in")

    val createdAt = column[LocalDateTime]("created_at")

    val lastAccessTime = column[LocalDateTime]("last_access_time")

    val tokenType = column[String]("token_type", O.NotNull, O.Default("mac"))

    val accessRights = column[Set[AccessRight]]("access_rights", O.NotNull, O.Default(Set()), O.DBType("json"))
    
    val activeAccessRightId = column[Option[java.util.UUID]]("active_access_right_id", O.DBType("uuid"))

    def * = (accessToken, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, createdAt, lastAccessTime, tokenType, accessRights, activeAccessRightId) <> (OAuthToken.tupled, OAuthToken.unapply)

    val user = foreignKey("TOKEN_USER_FK", userId, Users)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    // val client = foreignKey("TOKEN_CLIENT_FK", clientId, OAuthClients)(_.id, ForeignKeyAction.Cascade)

    val indexRefreshToken = index("TOKEN_REFRESH_TOKEN_INDEX", refreshToken)

    val pk = primaryKey("TOKEN_PK", accessToken)
  }

  val OAuthTokens = new TableQuery(new OAuthTokens(_)) {

    private val insertInvoker = {
      val oauthTokensInserts =
        this returning this.map(_.accessToken) into {
          case (token, _) =>
            OAuthToken(
              token.accessToken,
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

    def insert(accessToken: String, userId: java.util.UUID, refreshToken: Option[String], macKey: String, uA: String, expiresIn: Option[Duration], refreshExpiresIn: Option[Duration], createdAt: LocalDateTime, lastAccessTime: LocalDateTime, tokenType: String, accessRights: Set[AccessRight], activeAccessRightId: Option[java.util.UUID])(implicit session: jdbc.Q.Session): OAuthToken =
      insertInvoker insert OAuthToken(accessToken, userId, refreshToken, macKey, uA, expiresIn, refreshExpiresIn, createdAt, lastAccessTime, tokenType, accessRights, activeAccessRightId)
  }

  class Apps(tag: Tag) extends Table[App](tag, "apps") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val name = column[String]("name", O.NotNull)

    val scopes = column[List[String]]("scopes", O.NotNull, O.Default(List()))

    def * = (name, scopes, id?) <> ({ t: (String, List[String], Option[java.util.UUID]) => t match { case (name, scopes, id) => App(name, scopes, id = id) } }, (app: App) => Option((app.name, app.scopes, app.id)))

    val indexName = index("APP_NAME_INDEX", name, unique = true)

    val pk = primaryKey("APP_PK", id)
  }

  val Apps = new TableQuery(new Apps(_)) {

    private val insertInvoker = {
      val appsInserts =
        this returning this.map(_.id) into { case (app, id) => App(app.name, app.scopes, app.accessRights, id = Some(id)) }

      appsInserts.insertInvoker
    }

    def insert(name: String, scopes: List[String])(implicit session: jdbc.Q.Session): App = insertInvoker insert App(name, scopes)

    def delete(id: String)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === uuid(id))
        .delete
  }

  class AccessRights(tag: Tag) extends Table[AccessRight](tag, "access_rights") {

    val id = column[java.util.UUID]("id", O.DBType("uuid"), O.AutoInc, O.NotNull)

    val alias = column[String]("alias", O.NotNull)
    
    val displayName = column[String]("display_name", O.NotNull)

    val redirectUri = column[String]("redirect_uri", O.NotNull)

    val appId = column[java.util.UUID]("app_id", O.NotNull, O.DBType("uuid"))

    val scopes = column[List[Scope]]("scopes", O.DBType("json"), O.NotNull, O.Default(List()))
    
    val grantOptions = column[List[java.util.UUID]]("grant_options", O.NotNull, O.Default(List()))

    def * = (alias, displayName, redirectUri, appId, scopes, grantOptions, id?) <> (AccessRight.tupled, AccessRight.unapply)

    val app = foreignKey("ACCESS_RIGHT_APP_FK", appId, Apps)(_.id, ForeignKeyAction.Cascade, ForeignKeyAction.Cascade)

    val indexAlias = index("ACCESS_RIGHT_ALIAS_INDEX", alias, unique = true)

    val pk = primaryKey("ACCESS_RIGHT_PK", id)
  }

  val AccessRights = new TableQuery(new AccessRights(_)) {

    private val insertInvoker = {
      val accessRightsInserts =
        this returning this.map(_.id) into { case (accessRight, id) => AccessRight(accessRight.alias, accessRight.displayName, accessRight.redirectUri, accessRight.appId, accessRight.scopes, accessRight.grantOptions, id = Some(id)) }

      accessRightsInserts.insertInvoker
    }

    def insert(alias: String, displayName: String, redirectUri: String, appId: String, scopes: List[Scope], grantOptions: List[String] = Nil)(implicit session: jdbc.Q.Session): AccessRight = 
      insertInvoker insert AccessRight(alias, displayName, redirectUri, uuid(appId), scopes, grantOptions map uuid)

    def delete(id: String)(implicit session: jdbc.Q.Session) =
      this
        .filter(_.id === uuid(id))
        .delete
  }

  class UsersAccessRights(tag: Tag) extends Table[UserAccessRight](tag, "users_access_rights") {

    val userId = column[java.util.UUID]("user_id", O.NotNull, O.DBType("uuid"))

    val accessRightId = column[java.util.UUID]("access_right_id", O.NotNull, O.DBType("uuid"))

    val grantedAt = column[LocalDateTime]("granted_at", O.NotNull)

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

  val Labels = new TableQuery(new Labels(_)) {

    private val insertInvoker = {
      val labelsInserts =
        this returning this.map(_.name) into { case (label, name) => Label(name, label.color) }

      labelsInserts.insertInvoker
    }

    def insert(name: String, color: String)(implicit session: jdbc.Q.Session): Label = insertInvoker insert Label(name, color)
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