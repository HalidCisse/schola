package ma.epsilon.schola
package http

import play.api.{ Plugin, Application }

import com.typesafe.plugin._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.jolbox.bonecp.BoneCPDataSource

import caching.impl.{ CacheSystem, CacheSystemProvider }

trait DB extends Plugin {
  def db: schema.Q.Database
}

class C3P0(app: Application) extends DB {
  val db = schema.Q.Database.forDataSource(new ComboPooledDataSource)
}

class BoneCP(app: Application) extends DB {
  val db = schema.Q.Database.forDataSource(new BoneCPDataSource)
}

trait Façade extends MainFaçade
  with AdminFaçade
  with Plugin

trait MainFaçade extends impl.OAuthServicesRepoComponentImpl
  with impl.OAuthServicesComponentImpl
  with impl.AppsRepoImpl
  with impl.AppsImpl
  with MailingComponent
  with MailingComponentImpl
  with AkkaSystemProvider
  with CachingServicesComponent
  with CacheSystemProvider

trait AdminFaçade extends impl.UserServicesComponentImpl
    with impl.UserServicesRepoComponentImpl
    with impl.LabelServicesRepoComponentImpl
    with impl.LabelServicesComponentImpl
    with impl.AvatarServicesRepoComponentImpl
    with impl.AvatarServicesComponentImpl
    with impl.CachingUserServicesComponentImpl
    with impl.CachingServicesComponentImpl {
  this: MailingComponent with AkkaSystemProvider with CachingServicesComponent with CacheSystemProvider =>
}

class DefaultFaçade(app: Application) extends Façade {

  implicit lazy val system = play.libs.Akka.system

  lazy val cacheSystem = new CacheSystem(config.getInt("cache.ttl"))

  lazy val avatarServices = new AvatarServicesImpl

  lazy val oauthService = new OAuthServicesImpl

  lazy val userService = new UserServicesImpl with CachingUserServicesImpl {}

  lazy val labelService = new LabelServicesImpl

  lazy val appService = new AppServicesImpl

  protected lazy val db = use[DB].db

  lazy val mailer = new MailerImpl

  // ----------------------------------------------------------------------------------------------------------

  import schema._
  import domain._
  import Q._

  private val log = Logger("oadmin.Façade")

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

      T.updateNA(
        """
          | alter table if exists "users_labels" drop constraint "USER_LABEL_USER_FK";
          | alter table if exists "users_labels" drop constraint "USER_LABEL_LABEL_FK";
          | alter table if exists "labels" drop constraint "LABEL_PK";
          | alter table if exists "users_labels" drop constraint "USER_LABEL_PK";        
          | alter table if exists "users" drop constraint "USER_MODIFIER_FK";
          | alter table if exists "users" drop constraint "USER_CREATOR_FK";
          | alter table if exists "oauth_tokens" drop constraint "TOKEN_USER_FK";
          | alter table if exists "oauth_tokens" drop constraint "TOKEN_CLIENT_FK";
          | alter table if exists "access_rights" drop constraint "ACCESS_RIGHT_APP_FK";
          | alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_ACCESS_RIGHT_FK";
          | alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_USER_GRANTOR_FK";
          | alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_USER_FK";
          | alter table if exists "users" drop constraint "USER_PK";
          | drop table if exists "users";
          | alter table if exists "oauth_tokens" drop constraint "TOKEN_PK";
          | drop table if exists "oauth_tokens";
          | alter table if exists "oauth_clients" drop constraint "CLIENT_PK";
          | drop table if exists "oauth_clients";
          | alter table if exists "apps" drop constraint "APP_PK";
          | drop table if exists "apps";
          | alter table if exists "access_rights" drop constraint "ACCESS_RIGHT_PK";
          | drop table if exists "access_rights";
          | alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_PK";
          | drop table if exists "users_access_rights";
          | drop table if exists "labels";
          | drop table if exists "users_labels";
          | DROP EXTENSION if exists "uuid-ossp";
        """.stripMargin).execute()
  }

  def init(userId: java.util.UUID) = db withTransaction {
    implicit session =>

      import scala.slick.jdbc.{ StaticQuery => T }

      try {

        T.updateNA(
          """
            | CREATE EXTENSION "uuid-ossp";
            | create table "users" ("primary_email" VARCHAR(254) NOT NULL,"password" text NOT NULL,"given_name" VARCHAR(254) NOT NULL,"family_name" VARCHAR(254) NOT NULL,"created_at" BIGINT NOT NULL,"created_by" uuid,"last_login_time" BIGINT,"last_modified_at" BIGINT,"last_modified_by" uuid,"gender" VARCHAR(254) DEFAULT 'Male' NOT NULL,"home_address" text,"work_address" text,"contacts" text,"user_activation_key" text,"_deleted" BOOLEAN DEFAULT false NOT NULL,"suspended" BOOLEAN DEFAULT false NOT NULL,"change_password_at_next_login" BOOLEAN DEFAULT false NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4());
            | alter table "users" add constraint "USER_PK" primary key("id");
            | create unique index "USER_USERNAME_INDEX" on "users" ("primary_email");
            | create table "oauth_tokens" ("access_token" VARCHAR(254) NOT NULL,"client_id" VARCHAR(254) NOT NULL,"redirect_uri" VARCHAR(254) NOT NULL,"user_id" uuid NOT NULL,"refresh_token" VARCHAR(254),"secret" VARCHAR(254) NOT NULL,"user_agent" text NOT NULL,"expires_in" BIGINT,"refresh_expires_in" BIGINT,"created_at" BIGINT NOT NULL,"last_access_time" BIGINT NOT NULL,"token_type" VARCHAR(254) DEFAULT 'mac' NOT NULL,"access_rights" text DEFAULT '[]' NOT NULL);
            | alter table "oauth_tokens" add constraint "TOKEN_PK" primary key("access_token");
            | create index "TOKEN_REFRESH_TOKEN_INDEX" on "oauth_tokens" ("refresh_token");
            | create table "oauth_clients" ("client_id" VARCHAR(254) NOT NULL,"client_secret" VARCHAR(254) NOT NULL,"redirect_uri" VARCHAR(254) NOT NULL);
            | alter table "oauth_clients" add constraint "CLIENT_PK" primary key("client_id","client_secret");
            | create unique index "CLIENT_CLIENT_ID_INDEX" on "oauth_clients" ("client_id");
            | create table "apps" ("name" VARCHAR(254) NOT NULL,"scopes" text DEFAULT '[]' NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4());
            | alter table "apps" add constraint "APP_PK" primary key("id");
            | create unique index "APP_NAME_INDEX" on "apps" ("name");
            | create table "access_rights" ("name" VARCHAR(254) NOT NULL,"app_id" uuid NOT NULL,"scopes" text DEFAULT '[]' NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4());
            | alter table "access_rights" add constraint "ACCESS_RIGHT_PK" primary key("id");
            | create unique index "ACCESS_RIGHT_NAME_INDEX" on "access_rights" ("name");
            | create table "users_access_rights" ("user_id" uuid NOT NULL,"access_right_id" uuid NOT NULL,"granted_at" BIGINT NOT NULL,"granted_by" uuid);
            | alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_PK" primary key("user_id","access_right_id");
            | alter table "users" add constraint "USER_MODIFIER_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "users" add constraint "USER_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "oauth_tokens" add constraint "TOKEN_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE;
            | alter table "oauth_tokens" add constraint "TOKEN_CLIENT_FK" foreign key("client_id") references "oauth_clients"("client_id") on update CASCADE on delete NO ACTION;
            | alter table "access_rights" add constraint "ACCESS_RIGHT_APP_FK" foreign key("app_id") references "apps"("id") on update CASCADE on delete CASCADE;
            | alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_ACCESS_RIGHT_FK" foreign key("access_right_id") references "access_rights"("id") on update CASCADE on delete CASCADE;
            | alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_USER_GRANTOR_FK" foreign key("granted_by") references "users"("id") on update CASCADE on delete SET NULL;
            | alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE;
          """.stripMargin).execute()

        // Add a client - oadmin:oadmin
        val _1 = (OAuthClients += OAuthClient("schola", "schola", "http://localhost:3000/schola")) == 1

        //Add a user
        val _2 = Users.forceInsert(U.SuperUser copy (password = U.SuperUser.password map passwords.crypt)) == 1

        /*        val _3 = (Roles ++= List(
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
          UserRole(userId, "Role Two"))) == Some(3)*/

        val adminApp = Apps insert ("admin", List("users", "stats.admin", "settings.*"))

        val adminReadonlyRight = AccessRights insert ("admin.readonly", adminApp.id.get.toString, List(
          domain.Scope("users", write = false, trash = false),
          domain.Scope("stats.admin"),
          domain.Scope("settings.*", write = false, trash = false)))

        val adminRight = AccessRights insert ("admin", adminApp.id.get.toString, List(
          domain.Scope("users"),
          domain.Scope("stats.admin")))

        val adminSettingsRight = AccessRights insert ("admin.settings", adminApp.id.get.toString, List(
          domain.Scope("users"),
          domain.Scope("stats.admin"),
          domain.Scope("settings.*")))

        val accessRights = List(
          adminReadonlyRight, adminRight, adminSettingsRight)

        // UsersAccessRights insert UserAccessRight(U.SuperUser.id.get, adminSettingsRight.id.get)

        Cache.clearAll()

        _1 && _2 /*&& _3 && _4 && _5 && _6*/
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

    /*    def rndRole =
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
      Permission(s"${rndString(4).toLowerCase}.${rndString(6).toLowerCase}", "oadmin") */

    val amadouEpsilon =      
      User(
        "amadou.cisse@epsilon.ma",
        Some("amsayk"),
        "Amadou",
        "Cisse",
        createdBy = U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),
        workAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),

        contacts = Some(Contacts(Some(MobileNumbers(Some(rndPhone), None)), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))))),
      
        changePasswordAtNextLogin = false,
        id = Some(java.util.UUID.randomUUID))

      db.withTransaction {
        implicit session => Users.forceInsert(amadouEpsilon copy (password = amadouEpsilon.password map passwords.crypt))
      }

    val amadouGmail =
      User(
        "cisse.amadou.9@gmail.com",
        Some("amsayk"),
        "Ousman",
        "Cisse",
        createdBy = U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),
        workAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),

        contacts = Some(Contacts(Some(MobileNumbers(Some(rndPhone), None)), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))))),
      
        changePasswordAtNextLogin = false,
        id = Some(java.util.UUID.randomUUID))

      db.withTransaction {
        implicit session => Users.forceInsert(amadouGmail copy (password = amadouGmail.password map passwords.crypt))
      }      

    def rndUser =
      User(
        rndEmail.toLowerCase,
        Some(rndString(4)),
        rndString(5),
        rndString(9),
        createdBy = if(scala.util.Random.nextBoolean) if(scala.util.Random.nextBoolean) amadouGmail.id else amadouEpsilon.id else U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),
        workAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),

        contacts = Some(Contacts(Some(MobileNumbers(Some(rndPhone), None)), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))))),

        changePasswordAtNextLogin = false)

    val rndUsers = (0 to 600).par map (_ => rndUser)
    //    val rndRoles = (((0 to 5) map (_ => rndRole)) ++ createRndRoles).par
    //    val rndPermissions = (0 to 50).par map (_ => rndPermission)
    val rndLabels = (0 to 6).par map (i => Label(s"${rndString(6)} $i", "#fff"))

    val users = {
      log.info("Creating users . . . ")

      (rndUsers map { u =>
        
        userService.saveUser(
          u.primaryEmail,
          u.password.get,
          u.givenName,
          u.familyName,
          u.createdBy map (_.toString),
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.changePasswordAtNextLogin,
          Nil)

      }) ++ Set(amadouEpsilon, amadouGmail)     
    }

    log.info("Deleting users . . .")
    userService.removeUsers(users.map(_.id.get.toString).seq.take(75).toSet)

    log.info("Suspending users . . .")

    users.seq.take(25) foreach{
      user =>

        db.withTransaction {
          implicit session => Users where(_.id is user.id) map(_.suspended) update(true)
        }
    }    

    users.seq.drop(75).take(250) foreach{
      user =>

        db.withTransaction {
          implicit session => Users where(_.id is user.id) map(_.suspended) update(true)
        }
    }


    //    log.info("Creating roles . . . ");
    //    val roles = rndRoles.seq map {r => accessControlService.saveRole(r.name, r.parent, r.createdBy map (_.toString)); r}

    //    val permissions = withTransaction { implicit s => log.info("Creating permissions . . . "); rndPermissions map (p => { Permissions.insert(p); p }) }

    /*{
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

    {
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
    }*/

    log.info("Creating labels . . .")
    val labels = rndLabels flatMap { label =>
      labelService.findOrNew(label.name, Some(label.color))
    }

    val labelledUsers = users.map(_.id.get).seq.drop(300).take(128)

    labelledUsers foreach { userId =>
      db.withTransaction {
        implicit session => labels foreach (label => UsersLabels += UserLabel(userId, label.name))
      }
    }

    /*

    log.info("Creating apps . . .")
    val archives = appService.addApp("archives", List("archives.scope.0", "archives.scope.1", "archives.scope.2"))
    val orphans = appService.addApp("orphans", List("orphans.scope.0", "orphans.scope.1", "orphans.scope.2"))
    val schools = appService.addApp("schools", List("schools.scope.0", "schools.scope.1", "schools.scope.2"))

    val apps = List(
       archives,  orphans, schools
    )

    log.info("Creating access rights . . .")
    val archivesRights =
      db.withTransaction {
        implicit session =>
          List(

            AccessRights insert ("archives.readonly", archives.id.get.toString, List(
              domain.Scope("archives.scope.0", write = false, del = false),
              domain.Scope("archives.scope.1", write = false, del = false),
              domain.Scope("archives.scope.2", write = false, del = false),
              domain.Scope("stats.archives"),
              domain.Scope("settings.archives", write = false, del = false))),

            AccessRights insert ("archives", archives.id.get.toString, List(
              domain.Scope("archives.scope.0"),
              domain.Scope("archives.scope.1"),
              domain.Scope("archives.scope.2"),
              domain.Scope("stats.archives"),
              domain.Scope("settings.archives")))
          )
      }

    val orphansRights =
      db.withTransaction {
        implicit session =>

          List(

            AccessRights insert ("orphans.readonly", archives.id.get.toString, List(
              domain.Scope("orphans.scope.0", write = false, del = false),
              domain.Scope("orphans.scope.1", write = false, del = false),
              domain.Scope("orphans.scope.2", write = false, del = false),
              domain.Scope("stats.orphans"),
              domain.Scope("settings.orphans", write = false, del = false))),

            AccessRights insert ("orphans", archives.id.get.toString, List(
              domain.Scope("orphans.scope.0"),
              domain.Scope("orphans.scope.1"),
              domain.Scope("orphans.scope.2"),
              domain.Scope("stats.orphans"),
              domain.Scope("settings.orphans")))
          )
      }

    val schoolsRights =
      db.withTransaction {
        implicit session =>

          List(

            AccessRights insert ("schools.readonly", archives.id.get.toString, List(
              domain.Scope("schools.scope.0", write = false, del = false),
              domain.Scope("schools.scope.1", write = false, del = false),
              domain.Scope("schools.scope.2", write = false, del = false),
              domain.Scope("stats.schools"),
              domain.Scope("settings.schools", write = false, del = false))),

            AccessRights insert ("schools", archives.id.get.toString, List(
              domain.Scope("schools.scope.0"),
              domain.Scope("schools.scope.1"),
              domain.Scope("schools.scope.2"),
              domain.Scope("stats.schools"),
              domain.Scope("settings.schools")))
          )
      }

     val allRights = archivesRights ::: orphansRights ::: schoolsRights ::: Nil

     val accessRightUsers = users.map(_.id.get).seq.drop(75).take(50)

     val userAccessRights = ((accessRightUsers flatMap {
       userId => archivesRights map(right => UserAccessRight(userId, right.id.get))
     }) :: (accessRightUsers flatMap {
       userId => orphansRights map(right => UserAccessRight(userId, right.id.get))
     }) :: (accessRightUsers flatMap {
       userId => schoolsRights map(right => UserAccessRight(userId, right.id.get))
     }) :: Nil) flatten

     db.withTransaction{
       implicit session => UsersAccessRights ++= userAccessRights
     } */

    Cache.clearAll()

    () => {

      /* userAccessRights foreach { userAccessRight =>
        db.withTransaction {
          implicit session =>
            UsersAccessRights
              .where(o => (o.userId is userAccessRight.userId) && (o.accessRightId is userAccessRight.accessRightId))
              .delete
        }
      }

      allRights foreach { accessRight =>
        db.withTransaction {
          implicit session =>
            AccessRights delete accessRight.id.get.toString
        }
      }

      apps foreach{ app =>
        db.withTransaction{
          implicit session =>
            appService removeApp app.id.get.toString
        }
      }*/

      //      users foreach { u => accessControlService.revokeUserRoles(u.id.get.toString, roles.seq.map(_.name).toSet) }
      //      roles foreach { r => accessControlService.revokeRolePermission(r.name, permissions.seq.map(_.name).toSet) }

      users foreach (u => userService.purgeUsers(Set(u.id.get.toString)))
      //      roles foreach (r => accessControlService.purgeRoles(Set(r.name)))

      //      withTransaction { implicit s =>
      //        Permissions where (_.name inSet permissions.seq.map(_.name)) delete
      //      }

      withTransaction { implicit s =>
        labelService.remove(labels.map(_.name).seq.toSet)
      }

      Cache.clearAll()
    }
  }
}

trait MailingComponentImpl extends MailingComponent {
  this: AkkaSystemProvider =>

  object MockMailer extends MailerImpl {
    override def sendPasswordResetEmail(username: String, key: String) {}
    override def sendPasswordChangedNotice(username: String) {}
    override def sendWelcomeEmail(username: String, password: String) {}
  }

  class MailerImpl extends Mailer {

    private[this] val log = Logger("http.MailerImpl")

    def sendPasswordResetEmail(username: String, key: String) {
      val subj = "[Schola] Password reset request"

      val msg = s"""
        | Someone requested that the password be reset for the following account:\r\n\r\n
        | Username: $username \r\n\r\n
        | If this was a mistake, just ignore this email and nothing will happen. \r\n\r\n
        | To reset your password, visit the following address:\r\n\r\n
        | < http://localhost/RstPasswd?key=$key&login=${java.net.URLEncoder.encode(username, "UTF-8")} >\r\n""".stripMargin

      sendEmail(subj, username, (Some(msg), None))
    }

    def sendPasswordChangedNotice(username: String) {
      val subj = "[Schola] Password change notice"

      val msg = s"""
        | Someone just changed the password for the following account:\r\n\r\n
        | Username: $username \r\n\r\n
        | If this was you, congratulation! the change was successfull. \r\n\r\n
        | Otherwise, contact your administrator immediately.\r\n""".stripMargin

      sendEmail(subj, username, (Some(msg), None))
    }

    def sendWelcomeEmail(username: String, password: String) {
      val subj = "[Schola] Welcome to Schola!"

      val msg = s"""
        | Congratulation, your account was successfully created.\r\n\r\n
        | Here are the details:\r\n\r\n
        | Username: $username \r\n\r\n
        | Password: $password \r\n\r\n
        | Sign in immediately at < http://localhost/Login > to reset your password and start using the service.\r\n\r\n
        | Thank you.\r\n""".stripMargin

      sendEmail(subj, username, (Some(msg), None))
    }

    lazy val FromAddress = implicitly[Application].configuration.getString("smtp.from").getOrElse(throw new RuntimeException("From addres is required."))

    private lazy val mailerRepo = use[com.typesafe.plugin.MailerPlugin].email

    private def sendEmail(subject: String, recipient: String, body: (Option[String], Option[String])) {
      import scala.concurrent.duration._

      if (log.isDebugEnabled) {
        log.debug("[Schola] sending email to %s".format(recipient))
        log.debug("[Schola] mail = [%s]".format(body))
      }

      system.scheduler.scheduleOnce(1000 microseconds) {

        mailerRepo.setSubject(subject)
        mailerRepo.setRecipient(recipient)
        mailerRepo.setFrom(FromAddress)

        mailerRepo.setReplyTo(FromAddress)

        // the mailer plugin handles null / empty string gracefully
        mailerRepo.send(body._1 getOrElse "", body._2 getOrElse "")
      }
    }
  }
}

/*

import ma.epsilon.schola._, schema._, domain._, http._

//import com.mchange.v2.c3p0.ComboPooledDataSource
import com.jolbox.bonecp.BoneCPDataSource

implicit val app: play.api.Application = null

object d extends DefaultFaçade(app) { 
  // override lazy val db = schema.Q.Database.forDataSource(new ComboPooledDataSource)
  override lazy val db = schema.Q.Database.forDataSource(new BoneCPDataSource { setDriverClass("org.postgresql.Driver") }) 
  override implicit lazy val system = akka.actor.ActorSystem()
  override lazy val mailer  = MockMailer.asInstanceOf[MailerImpl]
}

d.drop()
d.init(U.SuperUser.id.get)
d.genFixtures(d.system)
Cache.clearAll()

*/ 