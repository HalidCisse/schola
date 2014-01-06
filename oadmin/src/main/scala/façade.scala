package schola
package oadmin

import javax.servlet.http.HttpServletResponse
import org.json4s.JsonAST.{JValue, JObject}

object Façade extends ServiceComponentFactory with HandlerFactory{
  import javax.servlet.http.HttpServletRequest
  import unfiltered.request._
  import unfiltered.response._

  import com.typesafe.config.Config

  import scala.util.control.Exception.allCatch

  class CacheConfig(config: Config) {
    val MaxTTL = config getInt "max-ttl"
  }
  
  class DbConfig(config: Config) {
    val Driver = config getString "driver-class"
    val URL = config getString "url"
    val User = config getString "user"
    val Password = config getString "password"
    val MaxPoolSize = config getInt "max-pool-size"
    val MinPoolSize = config getInt "min-pool-size"
  }

  object dbConfig extends DbConfig(config getConfig "db")

  object cacheConfig extends CacheConfig(config getConfig "cache")

  object simple extends Façade {

    val cacheSystem = new impl.CacheSystem(cacheConfig.MaxTTL)

    protected val db = {
      import dbConfig._
      import com.mchange.v2.c3p0.ComboPooledDataSource

      val ds = new ComboPooledDataSource
      ds.setDriverClass(Driver)
      ds.setJdbcUrl(URL)
      ds.setUser(User)
      ds.setPassword(Password)

      ds.setMaxPoolSize(MaxPoolSize)
      ds.setMinPoolSize(MinPoolSize)

      Q.Database.forDataSource(ds)
    }
  }

  def apply(req: HttpRequest[_ <: HttpServletRequest]) = new MyRouteHandler(req)

  // --------------------------------------------------------------------------------------------------

  implicit class Ctx(val req: HttpRequest[_ <: HttpServletRequest]) {
    val Some(resourceOwner) = utils.ResourceOwner.unapply(req)

    val cb = Jsonp.Optional.unapply(req) getOrElse Jsonp.EmptyWrapper
  }

  implicit def req2async[T <: HttpServletRequest, B <: HttpServletResponse](req: HttpRequest[T]) = req.asInstanceOf[HttpRequest[T] with unfiltered.Async.Responder[B]]

  class MyRouteHandler(val ctx: Ctx) extends AnyVal with RouteHandler {

    import conversions.json.tojson
    import conversions.json.formats

    import ctx._
    import S._

    // -------------------------------------------------------------------------------------------------

    def downloadAvatar(userId: String) =
      oauthService.getAvatar(userId) match {
        case Some((avatarInfo, data)) =>

          import org.json4s.JsonDSL._

          req.respond {

            JsonContent ~>
              ResponseString(
                cb wrap tojson(
                  ("avatarInfo" -> org.json4s.Extraction.decompose(avatarInfo)) ~
                    ("data" -> data)))
          }

        case _ =>

          req.respond(NotFound)
      }

    def purgeAvatar(userId: String) =
      oauthService.updateUser(userId, new utils.DefaultUserSpec {
        override lazy val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(None))
      }) match {
        case Some(user) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
      }

    def uploadAvatar(userId: String, avatarInfo: domain.AvatarInfo, bytes: Array[Byte]) =
      oauthService.updateUser(userId, new utils.DefaultUserSpec {
        override lazy val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(Some(avatarInfo, bytes)))
      }) match {
        case Some(user) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
      }

    // -------------------------------------------------------------------------------------------------

    def addUser() =
    // primaryEmail
    // givenName and familyName
    // gender
    // home and work addresses
    // contacts

      (for {
        json <- JsonBody(req)
        x <- allCatch.opt {
          json.extract[domain.User]
        }
        passwd <- x.password
        y <- oauthService.saveUser(
          x.primaryEmail, passwd, x.givenName, x.familyName, Some(resourceOwner.id), x.gender, x.homeAddress, x.workAddress, x.contacts, x.changePasswordAtNextLogin)
      } yield y) match {

        case Some(user) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
      }

    def updateUser(userId: String) =
    // primaryEmail
    // givenName and familyName
    // password
    // gender
    // home and work addresses
    // contacts

      (for {
        json <- JsonBody(req)
        x <- oauthService.updateUser(userId, new utils.DefaultUserSpec {

          val findField =
            utils.findFieldStr(json)_

          val findFieldObj =
            utils.findFieldJObj(json)_

          override lazy val primaryEmail = findField("primaryEmail")

          override lazy val password = findField("password")
          override lazy val oldPassword = findField("old_password")

          override lazy val givenName = findField("givenName")
          override lazy val familyName = findField("familyName")
          override lazy val gender = findField("gender") flatMap (x => allCatch.opt {
            domain.Gender.withName(x)
          })

          override lazy val homeAddress = new UpdateSpecImpl[domain.AddressInfo](
            set = findFieldObj("homeAddress") map (x => allCatch.opt {
              x.extract[domain.AddressInfo]
            })
          )

          override lazy val workAddress = new UpdateSpecImpl[domain.AddressInfo](
            set = findFieldObj("workAddress") map (x => allCatch.opt {
              x.extract[domain.AddressInfo]
            })
          )

          override lazy val contacts =
            utils.findFieldJObj(json)("contacts") map{
              contactsJson =>

                ContactsSpec(

                  mobiles = {
                    val mobilesJson = utils.findFieldJObj(contactsJson)("mobiles") getOrElse JObject(List())

                    MobileNumbersSpec(
                      mobile1 = UpdateSpecImpl[String](
                        set = utils.findFieldJObj(mobilesJson)("mobile1") flatMap(o=>utils.findFieldStr(o)("phoneNumber")) map(s=>utils.If(s.isEmpty, None, Some(s)))
                      ),

                      mobile2 = UpdateSpecImpl[String](
                        set = utils.findFieldJObj(mobilesJson)("mobile2") flatMap(o=>utils.findFieldStr(o)("phoneNumber")) map(s=>utils.If(s.isEmpty, None, Some(s)))
                      )
                    )
                  },

                  home = utils.findFieldJObj(contactsJson)("home") flatMap (x => allCatch.opt {
                      ContactInfoSpec[domain.HomeContactInfo](
                        email = UpdateSpecImpl[String](
                          set = utils.findFieldStr(x)("email") map(s=>utils.If(s.isEmpty, None, Some(s)))
                        ),

                        fax = UpdateSpecImpl[String](
                          set = utils.findFieldStr(x)("fax") map(s=>utils.If(s.isEmpty, None, Some(s)))
                        ),

                        phoneNumber = UpdateSpecImpl[String](
                          set = utils.findFieldStr(x)("phoneNumber") map(s=>utils.If(s.isEmpty, None, Some(s)))
                        )
                      )
                    }),

                  work = utils.findFieldJObj(contactsJson)("work") flatMap (x => allCatch.opt {
                      ContactInfoSpec[domain.WorkContactInfo](
                        email = UpdateSpecImpl[String](
                          set = utils.findFieldStr(x)("email") map(s=>utils.If(s.isEmpty, None, Some(s)))
                        ),

                        fax = UpdateSpecImpl[String](
                          set = utils.findFieldStr(x)("fax") map(s=>utils.If(s.isEmpty, None, Some(s)))
                        ),

                        phoneNumber = UpdateSpecImpl[String](
                          set = utils.findFieldStr(x)("phoneNumber") map(s=>utils.If(s.isEmpty, None, Some(s)))
                        )
                      )
                    })
                )
            }
        })
      } yield x) match {

        case Some(user) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
      }

    def removeUser(userId: String) = {

      val response = oauthService.removeUser(userId)

      req.respond {

        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $response}""")
      }
    }

    def getUser(userId: String) =
      oauthService.getUser(userId) match {
        case Some(user) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(NotFound)
      }

    def getUsers = {
      val resp = oauthService.getUsers

      req.respond {
        
        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def getTrash = {
      val resp = oauthService.getPurgedUsers

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def purgeUsers(id: Set[String]) = {
      val resp = allCatch.opt {
        oauthService.purgeUsers(id)
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $resp}""")
      }
    }

    def undeleteUsers(id: Set[String]) = {
      val resp = allCatch.opt {
        oauthService.undeleteUsers(id)
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $resp}""")
      }
    }

    def grantRoles(userId: String, roles: Set[String]) = {
      val resp = allCatch.opt {
        accessControlService.grantUserRoles(userId, roles, Some(resourceOwner.id))
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(
            cb wrap s"""{"success": $resp}""")
      }
    }

    def revokeRoles(userId: String, roles: Set[String]) = {
      val resp = allCatch.opt {
        accessControlService.revokeUserRole(userId, roles)
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(
            cb wrap s"""{"success": $resp""")
      }
    }

    def getUserRoles(userId: String) = {
      val resp = accessControlService.getUserRoles(userId)

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def userExists(primaryEmail: String) = {
      val resp = oauthService.primaryEmailExists(primaryEmail)

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $resp}""")
      }
    }

    // ---------------------------------------------------------------------------------------------------

    def getRoles = {
      val resp = accessControlService.getRoles

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def addRole() =
      (for {
        json <- JsonBody(req)
        x <- allCatch.opt {
          //          org.json4s.native.Serialization.read[domain.Role](json)
          json.extract[domain.Role]
        }
        y <- accessControlService.saveRole(x.name, x.parent, Some(resourceOwner.id))
      } yield y) match {

        case Some(role) =>

          req.respond {
            JsonContent ~>
              ResponseString(cb wrap tojson(role))
          }

        case _ => req.respond(BadRequest)
      }

    def roleExists(name: String) = {
      val resp = accessControlService.roleExists(name)

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $resp}""")
      }
    }

    def updateRole(name: String) =
      (for {
        json <- JsonBody(req)
        x <- allCatch.opt {
          //          org.json4s.native.Serialization.read[domain.Role](json)
          json.extract[domain.Role]
        } if accessControlService.updateRole(name, x.name, x.parent)
        y <- accessControlService.getRole(x.name)
      } yield y) match {

        case Some(role) =>

          req.respond {
            JsonContent ~>
              ResponseString(cb wrap tojson(role))
          }

        case _ => req.respond(BadRequest)
      }

    def getRole(name: String) =
      accessControlService.getRole(name) match {
        case Some(role) =>

          req.respond {
            JsonContent ~>
              ResponseString(cb wrap tojson(role))
          }

        case _ => req.respond(NotFound)
      }

    def getUserSession = {

      val params = Map(
        /* "userId" -> resourceOwner.id, // -- NOT USED YET */
        "bearerToken" -> oauth2.Token.unapply(req).get,
        "userAgent" -> UserAgent.unapply(req).get
      )

      oauthService.getUserSession(params) match {
        case Some(session) =>

          req.respond {
            JsonContent ~> ResponseString(
              cb wrap tojson(session))
          }

        case _ => req.respond(NotFound)
      }
    }

    def purgeRoles(roles: Set[String]) = {
      val resp = allCatch.opt {
        accessControlService.purgeRoles(roles)
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $resp}""")
      }
    }

    def grantPermissions(name: String, permissions: Set[String]) = {
      val resp = allCatch.opt {
        accessControlService.grantRolePermissions(name, permissions, Some(resourceOwner.id))
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(
            cb wrap s"""{"success": $resp}""")
      }
    }

    def revokePermissions(name: String, permissions: Set[String]) = {
      val resp = allCatch.opt {
        accessControlService.revokeRolePermission(name, permissions)
        true
      } getOrElse false

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap s"""{"success": $resp}""")
      }
    }

    def getPermissions = {
      val resp = accessControlService.getPermissions

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def getRolePermissions(name: String) = {
      val resp = accessControlService.getRolePermissions(name)

      req.respond {
        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def logout(token: String) = {
      val resp = allCatch.opt {
        oauthService.revokeToken(token)
        true
      } getOrElse false

      req.respond {
        JsonContent ~> ResponseString(
          cb wrap s"""{"success": $resp}"""
        )
      }
    }
  }
}

trait RouteHandler extends Any {

  // -------------------------------------------------------------------------------------------------

  def downloadAvatar(userId: String)

  def purgeAvatar(userId: String)

  def uploadAvatar(userId: String, avatarInfo: domain.AvatarInfo, bytes: Array[Byte])

  // -------------------------------------------------------------------------------------------------

  def addUser()

  def updateUser(userId: String)

  def removeUser(userId: String)

  def getUser(userId: String)

  def getUsers

  def getTrash

  def purgeUsers(id: Set[String])

  def undeleteUsers(id: Set[String])

  def grantRoles(userId: String, roles: Set[String])

  def revokeRoles(userId: String, roles: Set[String])

  def getUserRoles(userId: String)

  def userExists(primaryEmail: String)

  // ---------------------------------------------------------------------------------------------------

  def getRoles

  def addRole()

  def roleExists(name: String)

  def updateRole(name: String)

  def getRole(name: String)

  def getUserSession

  def purgeRoles(roles: Set[String])

  def grantPermissions(name: String, permissions: Set[String])

  def revokePermissions(name: String, permissions: Set[String])

  def getPermissions

  def getRolePermissions(name: String)

  def logout(token: String)
}

trait ServiceComponentFactory {
  val simple: OAuthServicesComponent with AccessControlServicesComponent
}

trait HandlerFactory extends (unfiltered.request.HttpRequest[_ <: javax.servlet.http.HttpServletRequest] => RouteHandler)

trait Façade extends impl.OAuthServicesRepoComponentImpl
with impl.CachingServicesComponentImpl
with impl.CachingOAuthServicesComponentImpl
with impl.CacheSystemProvider
//with impl.OAuthServicesComponentImpl
with CachingServicesComponent
with impl.AccessControlServicesRepoComponentImpl
with impl.AccessControlServicesComponentImpl {

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
      import scala.slick.jdbc.{StaticQuery=>T}

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
        """.stripMargin
      ).execute()
  }

  def init(userId: java.util.UUID) = db withTransaction {
    implicit session =>

      import scala.slick.jdbc.{StaticQuery=>T}

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
            | create table "users" ("primary_email" VARCHAR(254) NOT NULL,"password" text NOT NULL,"given_name" VARCHAR(254) NOT NULL,"family_name" VARCHAR(254) NOT NULL,"created_at" BIGINT NOT NULL,"created_by" uuid,"last_login_time" BIGINT,"last_modified_at" BIGINT,"last_modified_by" uuid,"gender" VARCHAR(254) DEFAULT 'Male' NOT NULL,"home_address" text,"work_address" text,"contacts" text NOT NULL,"avatar" text,"user_activation_key" text,"_deleted" BOOLEAN DEFAULT false NOT NULL,"suspended" BOOLEAN DEFAULT false NOT NULL,"change_password_at_next_login" BOOLEAN DEFAULT false NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4() PRIMARY KEY);
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
          """.stripMargin
        ).execute()

        // Add a client - oadmin:oadmin
        val _1 = (OAuthClients ++= List(
          OAuthClient("oadmin", "oadmin", "http://localhost:3880/admin"),
          OAuthClient("schola", "schola", "http://localhost:3880/schola")
        )) == Some(2)

        //Add a user
        val _2 = (Users += SuperUser copy (password = SuperUser.password map passwords.crypt)) == 1

        val _3 = (Roles ++= List(
          SuperUserR,
          AdministratorR,
          Role("Role One", Some(AdministratorR.name), System.currentTimeMillis, None),
          Role("Role Two", Some(AdministratorR.name), System.currentTimeMillis, None),
          Role("Role Three", Some(AdministratorR.name), System.currentTimeMillis, Some(userId)),
          Role("Role Four", Some(SuperUserR.name), System.currentTimeMillis, None),
          Role("Role X", Some("Role One"), System.currentTimeMillis, Some(userId))
        )) == Some(7)

        /*    val _31 = (Roles += Role("Role One", None, System.currentTimeMillis, None)) == 1
            val _32 = (Roles += Role("Role Two", None, System.currentTimeMillis, None)) == 1
            val _33 = (Roles += Role("Role Three", None, System.currentTimeMillis, None)) == 1
            val _34 = (Roles += Role("Role Four", None, System.currentTimeMillis, None)) == 1
            val _35 = (Roles += Role("Role X", Some("Role One"), System.currentTimeMillis, None)) == 1

            val _3 = _31 && _32 && _33 && _34 && _35*/

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
          Permission("P10", "schola")
        )) == Some(10)

        val _5 = (RolesPermissions ++= List(
          RolePermission("Role One", "P1", grantedBy = None),
          RolePermission("Role One", "P2", grantedBy = Some(userId)),
          RolePermission("Role One", "P3", grantedBy = None),
          RolePermission("Role One", "P4", grantedBy = None),
          RolePermission("Role One", "P5", grantedBy = Some(userId))
        )) == Some(5)

        val _6 = (UsersRoles ++= List(
          UserRole(userId, SuperUserR.name, grantedBy = None),
          UserRole(userId, AdministratorR.name, grantedBy = None),
//          UserRole(userId, "Role One", grantedBy = None),
          UserRole(userId, "Role Three", grantedBy = Some(userId)),
          UserRole(userId, "Role Two", grantedBy = None)
        )) == Some(3)

        _1 && _2 && _3 && _4 && _5 && _6
      }
      catch {
        case e: java.sql.SQLException =>
          var cur = e
          while(cur ne null){
            cur.printStackTrace()
            cur = cur.getNextException
          }
          false
      }
  }

  def test() {
    val o = accessControlService

    val userId = SuperUser.id.get

//      def initialize() = init(userId)

//      initialize()

    println(o.getRoles)
    println(o.getUserRoles(userId.toString))
    println(o.getPermissions)
    println(o.getRolePermissions("Role One"))
    println(o.getClientPermissions("oadmin"))
    println(o.getUserPermissions(userId.toString))
    println(o.saveRole("Role XI", None, None))
    println(o.grantUserRoles(userId.toString, Set("Role Four"), None))
    println(o.grantRolePermissions("Role X", Set("P7", "P8"), None))
    println(o.userHasRole(userId.toString, "Role One"))
    println(o.userHasRole(userId.toString, "Role X"))
    println(o.roleHasPermission("Role One", Set("P1")))
  }
}