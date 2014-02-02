package schola
package oadmin

import com.mchange.v2.c3p0.ComboPooledDataSource

abstract class FaçadeImpl(implicit system: akka.actor.ActorSystem) extends impl.HandlerComponent with oauth2.OAuth2Component {

  import javax.servlet.http.HttpServletRequest
  import unfiltered.request._
  import unfiltered.response._

  import schema._
  import Q._

  import com.typesafe.config.Config

  import scala.util.control.Exception.allCatch

  private val log = org.clapper.avsl.Logger("oadmin.Façade")

  private val ds = new ComboPooledDataSource

  system.registerOnTermination {
    log.info("Closing datasource . . . ")
    ds.close()
  }

  object simple extends Façade {

    val avatarService = system.actorOf(akka.actor.Props(new impl.Avatars(new impl.MongoDBSettings(config getConfig "mongodb"))), name = "avatars")

    val cacheSystem = new impl.CacheSystem(config.getInt("cache.ttl"))

    val accessControlService = new AccessControlServicesImpl with CachingAccessControlServicesImpl {}

    val oauthService = new OAuthServicesImpl with CachingOAuthServicesImpl {}

    protected val db = Database.forDataSource(ds)
  }

  def apply(req: HttpRequest[_ <: HttpServletRequest]) = new RouteHandlerImpl(req)

  // --------------------------------------------------------------------------------------------------
}

trait Façade extends impl.AccessControlServicesRepoComponentImpl
    with impl.AccessControlServicesComponentImpl
    with impl.OAuthServicesRepoComponentImpl
    with impl.OAuthServicesComponentImpl
    with CachingServicesComponent
    with impl.CachingAccessControlServicesComponentImpl
    with impl.CachingOAuthServicesComponentImpl
    with impl.CachingServicesComponentImpl
    with impl.CacheSystemProvider {

  import schema._
  import domain._
  import Q._

  def withTransaction[T](f: Q.Session => T) =
    db.withTransaction {
      f
    }

  def withSession[T](f: Q.Session => T) =
    db.withSession {
      f
    }

  def drop() = db withTransaction {
    implicit session =>
      import scala.slick.jdbc.{ StaticQuery => T }

      //        val ddl = OAuthTokens.ddl ++ OAuthClients.ddl ++ Users.ddl ++ Roles.ddl ++ Permissions.ddl ++ UsersRoles.ddl ++ RolesPermissions.ddl
      //        ddl.drop

      //        T.updateNA("DROP EXTENSION \"uuid-ossp\";")

      T.updateNA(
        """
          | alter table "oauth_tokens" drop constraint "TOKEN_CLIENT_FK";
          | alter table "oauth_tokens" drop constraint "TOKEN_USER_FK";
          | alter table "users" drop constraint "USER_CREATOR_FK";
          | alter table "users" drop constraint "USER_MODIFIER_FK";
          | alter table "roles" drop constraint "ROLE_PARENT_ROLE_FK";
          | alter table "roles" drop constraint "ROLE_USER_FK";
          | alter table "permissions" drop constraint "PERMISSION_CLIENT_FK";
          | alter table "users_roles" drop constraint "USER_ROLE_USER_FK";
          | alter table "users_roles" drop constraint "USER_ROLE_ROLE_FK";
          | alter table "users_roles" drop constraint "USER_ROLE_USER_GRANTOR_FK";
          | alter table "roles_permissions" drop constraint "ROLE_PERMISSION_GRANTOR_FK";
          | alter table "roles_permissions" drop constraint "ROLE_PERMISSION_ROLE_FK";
          | alter table "roles_permissions" drop constraint "ROLE_PERMISSION_PERMISSION_FK";
          | drop table "oauth_tokens";
          | alter table "oauth_clients" drop constraint "CLIENT_PK";
          | drop table "oauth_clients";
          | drop table "users";
          | drop table "roles";
          | drop table "permissions";
          | alter table "users_roles" drop constraint "USER_ROLE_PK";
          | drop table "users_roles";
          | alter table "roles_permissions" drop constraint "ROLE_PERMISSION_PK";
          | drop table "roles_permissions";
          | DROP EXTENSION "uuid-ossp";
        """.stripMargin).execute()
  }

  def init(userId: java.util.UUID) = db withTransaction {
    implicit session =>

      import scala.slick.jdbc.{ StaticQuery => T }

      //        val ddl = OAuthTokens.ddl ++ OAuthClients.ddl ++ Users.ddl ++ Roles.ddl ++ Permissions.ddl ++ UsersRoles.ddl ++ RolesPermissions.ddl
      //          ddl.createStatements foreach (stmt => println(stmt+";"))
      //        ddl.create

      //        T.updateNA("CREATE EXTENSION \"uuid-ossp\";")

      try {
        T.updateNA(
          """
            | CREATE EXTENSION "uuid-ossp";
            | create table "oauth_tokens" ("access_token" VARCHAR(254) NOT NULL PRIMARY KEY,"client_id" VARCHAR(254) NOT NULL,"redirect_uri" VARCHAR(254) NOT NULL,"user_id" uuid NOT NULL,"refresh_token" VARCHAR(254),"secret" VARCHAR(254) NOT NULL,"user_agent" text NOT NULL,"expires_in" BIGINT,"refresh_expires_in" BIGINT,"created_at" BIGINT NOT NULL,"last_access_time" BIGINT NOT NULL,"token_type" VARCHAR(254) DEFAULT 'mac' NOT NULL,"scopes" VARCHAR(254) DEFAULT '[]' NOT NULL);
            | create table "oauth_clients" ("client_id" VARCHAR(254) NOT NULL,"client_secret" VARCHAR(254) NOT NULL,"redirect_uri" VARCHAR(254) NOT NULL);
            | alter table "oauth_clients" add constraint "CLIENT_PK" primary key("client_id","client_secret");
            | create unique index "CLIENT_CLIENT_ID_INDEX" on "oauth_clients" ("client_id");
            | create table "users" ("primary_email" VARCHAR(254) NOT NULL,"password" text NOT NULL,"given_name" VARCHAR(254) NOT NULL,"family_name" VARCHAR(254) NOT NULL,"created_at" BIGINT NOT NULL,"created_by" uuid,"last_login_time" BIGINT,"last_modified_at" BIGINT,"last_modified_by" uuid,"gender" VARCHAR(254) DEFAULT 'Male' NOT NULL,"home_address" text,"work_address" text,"contacts" text NOT NULL,"avatar" VARCHAR(254),"user_activation_key" text,"_deleted" BOOLEAN DEFAULT false NOT NULL,"suspended" BOOLEAN DEFAULT false NOT NULL,"change_password_at_next_login" BOOLEAN DEFAULT false NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY);
            | create unique index "USER_USERNAME_INDEX" on "users" ("primary_email");
            | create index "USER_USERNAME_PASSWORD_INDEX" on "users" ("primary_email","password");
            | create table "roles" ("name" VARCHAR(254) NOT NULL PRIMARY KEY,"parent" VARCHAR(254),"created_at" BIGINT NOT NULL,"created_by" uuid,"public" BOOLEAN DEFAULT true NOT NULL);
            | create unique index "ROLE_NAME_INDEX" on "roles" ("name");
            | create table "permissions" ("name" VARCHAR(254) NOT NULL PRIMARY KEY,"client_id" VARCHAR(254) NOT NULL);
            | create unique index "PERMISSION_NAME_INDEX" on "permissions" ("name");
            | create table "users_roles" ("user_id" uuid NOT NULL,"role" VARCHAR(254) NOT NULL,"granted_at" BIGINT NOT NULL,"granted_by" uuid);
            | alter table "users_roles" add constraint "USER_ROLE_PK" primary key("user_id","role");
            | create table "roles_permissions" ("role" VARCHAR(254) NOT NULL,"permission" VARCHAR(254) NOT NULL,"granted_at" BIGINT NOT NULL,"granted_by" uuid);
            | alter table "roles_permissions" add constraint "ROLE_PERMISSION_PK" primary key("role","permission");
            | alter table "oauth_tokens" add constraint "TOKEN_CLIENT_FK" foreign key("client_id") references "oauth_clients"("client_id") on update CASCADE on delete NO ACTION;
            | alter table "oauth_tokens" add constraint "TOKEN_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE;
            | alter table "users" add constraint "USER_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "users" add constraint "USER_MODIFIER_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "roles" add constraint "ROLE_USER_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "roles" add constraint "ROLE_PARENT_ROLE_FK" foreign key("parent") references "roles"("name") on update CASCADE on delete NO ACTION;
            | alter table "permissions" add constraint "PERMISSION_CLIENT_FK" foreign key("client_id") references "oauth_clients"("client_id") on update CASCADE on delete NO ACTION;
            | alter table "users_roles" add constraint "USER_ROLE_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete RESTRICT;
            | alter table "users_roles" add constraint "USER_ROLE_ROLE_FK" foreign key("role") references "roles"("name") on update CASCADE on delete CASCADE;
            | alter table "users_roles" add constraint "USER_ROLE_USER_GRANTOR_FK" foreign key("granted_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "roles_permissions" add constraint "ROLE_PERMISSION_GRANTOR_FK" foreign key("granted_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "roles_permissions" add constraint "ROLE_PERMISSION_ROLE_FK" foreign key("role") references "roles"("name") on update CASCADE on delete RESTRICT;
            | alter table "roles_permissions" add constraint "ROLE_PERMISSION_PERMISSION_FK" foreign key("permission") references "permissions"("name") on update NO ACTION on delete NO ACTION;
          """.stripMargin).execute()

        // Add a client - oadmin:oadmin
        val _1 = (OAuthClients ++= List(
          OAuthClient("oadmin", "oadmin", "http://localhost:3000/admin"),
          OAuthClient("schola", "schola", "http://localhost:3000/schola"))) == Some(2)

        //Add a user
        val _2 = (Users += U.SuperUser copy (password = U.SuperUser.password map passwords.crypt)) == 1

        val _3 = (Roles ++= List(
          R.SuperUserR,
          R.AdministratorR,
          Role("Role One"),
          Role("Role Two"),
          Role("Role Three", createdBy = Some(userId)),
          Role("Role Four"),
          Role("Role X", Some("Role One"), createdBy = Some(userId)))) == Some(7)

        val _4 = (Permissions ++= List(
          Permission("P1", "oadmin"),
          Permission("P2", "oadmin"),
          Permission("P3", "oadmin"),
          Permission("P4", "oadmin"),

          Permission("P5", "schola"),
          Permission("P6", "schola"),
          Permission("P7", "schola"),
          Permission("P8", "schola"),
          Permission("P9", "schola"),
          Permission("P10", "schola"))) == Some(10)

        val _5 = (RolesPermissions ++= List(
          RolePermission("Role One", "P1"),
          RolePermission("Role One", "P2", grantedBy = Some(userId)),
          RolePermission("Role One", "P3"),
          RolePermission("Role One", "P4"),
          RolePermission("Role One", "P5", grantedBy = Some(userId)))) == Some(5)

        val _6 = (UsersRoles ++= List(
          UserRole(userId, R.SuperUserR.name),
          UserRole(userId, R.AdministratorR.name),
          UserRole(userId, "Role Three", grantedBy = Some(userId)),
          UserRole(userId, "Role Two"))) == Some(3)

        _1 && _2 && _3 && _4 && _5 && _6
      } catch {
        case e: java.sql.SQLException =>
          var cur = e
          while (cur ne null) {
            cur.printStackTrace()
            cur = cur.getNextException
          }
          false
      }
  }

  def genFixtures(implicit system: akka.actor.ActorSystem) = {
    import domain._
    import org.apache.commons.lang3.{ RandomStringUtils => Rnd }

    def rndEmail = s"${Rnd.randomAlphanumeric(7)}@${Rnd.randomAlphanumeric(5)}.${Rnd.randomAlphanumeric(3)}"
    def rndString(len: Int) = Rnd.randomAlphabetic(len).toLowerCase.capitalize
    def rndPhone = Rnd.randomNumeric(9)
    def rndPostalCode = Rnd.randomNumeric(5)
    def rndStreetAddress = s"${rndString(7)} ${rndString(2)} ${rndString(7)} ${rndString(4)}"

    def rndRole =
      Role(rndString(6), None, createdBy = U.SuperUser.id)

    def createRndRoles = {

      val rndName1 = rndString(6)
      val rndName2 = rndString(6)
      val rndName3 = rndString(6)
      val rndName4 = rndString(6)
      val rndName5 = rndString(6)
      val rndName6 = rndString(6)
      val rndName7 = rndString(6)
      val rndName8 = rndString(6)
      val rndName9 = rndString(6)
      val rndName10 = rndString(6)
      val rndName11 = rndString(6)

      Seq(
        Role(rndName1, Some("Role One"), createdBy = U.SuperUser.id),
        Role(rndName2, Some("Role Two"), createdBy = U.SuperUser.id),
        Role(rndName3, Some("Role X"), createdBy = U.SuperUser.id),
        Role(rndName4, Some(rndName2), createdBy = U.SuperUser.id),
        Role(rndName5, Some(rndName2), createdBy = U.SuperUser.id),
        Role(rndName6, Some("Role X"), createdBy = U.SuperUser.id),
        Role(rndName7, Some(rndName4), createdBy = U.SuperUser.id),
        Role(rndName8, Some("Administrator"), createdBy = U.SuperUser.id),
        Role(rndName9, Some("Administrator"), createdBy = U.SuperUser.id),
        Role(rndName10, Some("Role X"), createdBy = U.SuperUser.id),
        Role(rndName11, Some("Role X"), createdBy = U.SuperUser.id),
        Role(rndString(6), Some("Administrator"), createdBy = U.SuperUser.id),
        Role(rndString(6), Some("Administrator"), createdBy = U.SuperUser.id),
        Role(rndString(6), Some(rndName1), createdBy = U.SuperUser.id),
        Role(rndString(6), Some(rndName1), createdBy = U.SuperUser.id),
        Role(rndString(6), Some(rndName1), createdBy = U.SuperUser.id),
        Role(rndString(6), Some(rndName1), createdBy = U.SuperUser.id),
        Role(rndString(6), Some("Administrator"), createdBy = U.SuperUser.id),
        Role(rndString(6), Some("Role One"), createdBy = U.SuperUser.id),
        Role(rndString(6), Some("Role One"), createdBy = U.SuperUser.id),
        Role(rndString(6), Some("Role One"), createdBy = U.SuperUser.id))
    }

    def rndPermission =
      Permission(s"${rndString(4).toLowerCase}.${rndString(6).toLowerCase}", "oadmin")

    def rndUser =
      User(
        rndEmail.toLowerCase,
        Some(rndString(4)),
        rndString(5),
        rndString(9),
        createdBy = U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(rndString(10), rndString(10), rndPostalCode, rndStreetAddress)),
        workAddress = Some(AddressInfo(rndString(10), rndString(10), rndPostalCode, rndStreetAddress)),

        contacts = Contacts(MobileNumbers(Some(rndPhone), None), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone)))),

        changePasswordAtNextLogin = false,
        id = Some(java.util.UUID.randomUUID))

    val rndUsers = (0 to 600).par map (_ => rndUser)
    val rndRoles = (((0 to 5) map (_ => rndRole)) ++ createRndRoles).par
    val rndPermissions = (0 to 50).par map (_ => rndPermission)

    val users = withTransaction { implicit s =>
      log.info("Creating users . . . ")

      rndUsers map { u =>
        oauthService.saveUser(
          u.primaryEmail,
          u.password.get,
          u.givenName,
          u.familyName,
          u.createdBy map (_.toString),
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.changePasswordAtNextLogin).get
      }
    }

    log.info("Creating roles . . . ");
    val roles = rndRoles map (r => withTransaction { implicit s => accessControlService.saveRole(r.name, r.parent, r.createdBy map (_.toString)).get })

    val permissions = withTransaction { implicit s => log.info("Creating permissions . . . "); rndPermissions map (p => { Permissions.insert(p); p }) }

    withTransaction { implicit s =>
      log.info("Creating users grants . . . ")

      for (u <- users) try
        accessControlService.grantUserRoles(u.id.get.toString, roles.seq.map(_.name).toSet, None)
      catch {
        case e: java.sql.SQLException =>
          var cur = e
          while (cur ne null) {
            cur.printStackTrace()
            cur = cur.getNextException
          }

          throw e
      }
    }

    withTransaction { implicit s =>
      log.info("Creating roles grants . . . ")

      for (r <- roles) try
        accessControlService.grantRolePermissions(r.name, permissions.seq.map(_.name).toSet, None)
      catch {
        case e: java.sql.SQLException =>
          var cur = e
          while (cur ne null) {
            cur.printStackTrace()
            cur = cur.getNextException
          }

          throw e
      }
    }

    () => withTransaction { implicit s =>

      users foreach { u => accessControlService.revokeUserRole(u.id.get.toString, roles.seq.map(_.name).toSet) }
      roles foreach { r => accessControlService.revokeRolePermission(r.name, permissions.seq.map(_.name).toSet) }

      users foreach (u => oauthService.purgeUsers(Set(u.id.get.toString)))
      roles foreach (r => accessControlService.purgeRoles(Set(r.name)))

      Permissions where (_.name inSet permissions.seq.map(_.name)) delete
    }
  }
}