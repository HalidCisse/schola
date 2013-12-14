package schola
package oadmin

import org.clapper.avsl.Logger

object façade extends Façade

trait RouteHandler {

  type Return = unfiltered.response.ResponseFunction[Any]

  // -------------------------------------------------------------------------------------------------

  def downloadAvatar(userId: String): Return

  def purgeAvatar(userId: String): Return

  def uploadAvatar(userId: String, avatarInfo: domain.AvatarInfo, bytes: Array[Byte]): Return

  // -------------------------------------------------------------------------------------------------

  def addUser(): Return

  def updateUser(userId: String): Return

  def removeUser(userId: String): Return

  def getUser(userId: String): Return

  def getUsers: Return

  def getTrash: Return

  def purgeUsers(id: Set[String]): Return

  def addContacts(userId: String): Return

  def removeContacts(userId: String): Return

  def grantRoles(userId: String, roles: Set[String]): Return

  def revokeRoles(userId: String, roles: Set[String]): Return

  def getUserRoles(userId: String): Return

  def userExists(email: String): Return

  // ---------------------------------------------------------------------------------------------------

  def getRoles: Return

  def addRole(): Return

  def roleExists(name: String): Return

  def updateRole(name: String): Return

  def getRole(name: String): Return

  def getUserSession: Return

  def purgeRoles(roles: Set[String]): Return

  def grantPermissions(name: String, permissions: Set[String]): Return

  def revokePermissions(name: String, permissions: Set[String]): Return

  def getPermissions: Return

  def getRolePermissions(name: String): Return

  def logout(token: String): Return
}

trait HandlerFactory extends (unfiltered.request.HttpRequest[_ <: javax.servlet.http.HttpServletRequest] => RouteHandler)

class Façade extends impl.OAuthServicesRepoComponentImpl
with impl.OAuthServicesComponentImpl
with impl.AccessControlServicesRepoComponentImpl
with impl.AccessControlServicesComponentImpl
with HandlerFactory{

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

  protected lazy val db = {
    import com.mchange.v2.c3p0.ComboPooledDataSource

    val ds = new ComboPooledDataSource
    ds.setDriverClass(Db.DriverClass)
    ds.setJdbcUrl(Db.DatabaseURL)
    ds.setUser(Db.Username)
    ds.setPassword(Db.Password)

    ds.setMaxPoolSize(Db.MaxPoolSize)
    ds.setMinPoolSize(Db.MinPoolSize)

    Q.Database.forDataSource(ds)
  }

  def drop() = db withTransaction {
    implicit session =>
      val ddl = OAuthTokens.ddl ++ OAuthClients.ddl ++ Users.ddl ++ Roles.ddl ++ Permissions.ddl ++ UsersRoles.ddl ++ RolesPermissions.ddl
      ddl.drop
  }

  def init(userId: java.util.UUID) = db withTransaction {
    implicit session =>
      val ddl = OAuthTokens.ddl ++ OAuthClients.ddl ++ Users.ddl ++ Roles.ddl ++ Permissions.ddl ++ UsersRoles.ddl ++ RolesPermissions.ddl
      //    ddl.createStatements foreach (stmt => println(stmt+";"))
      ddl.create

      // Add a client - oadmin:oadmin
      val _1 = (OAuthClients ++= List(
        OAuthClient("oadmin", "oadmin", "http://localhost:3880/admin"),
        OAuthClient("schola", "schola", "http://localhost:3880/schola")
      )) == Some(2)

      //Add a user
      val _2 = (Users += SuperUser copy(password = SuperUser.password map passwords.crypt)) == 1

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
        UserRole(userId, "Role One", grantedBy = None),
        UserRole(userId, "Role Three", grantedBy = Some(userId)),
        UserRole(userId, "Role Two", grantedBy = None)
      )) == Some(3)

      _1 && _2 && _3 && _4 && _5 && _6
  }

  def test() = {
    val o = accessControlService

    val userId = SuperUser.id

    def initialize() = init(userId.get)

    initialize()

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

  // --------------------------------------------------------------------------------------------------

  import unfiltered.request._
  import unfiltered.response._

  import scala.util.control.Exception.allCatch

  import unfiltered.request.Jsonp

  class MyRouteHandler(val req: HttpRequest[_ <: javax.servlet.http.HttpServletRequest]) extends RouteHandler{

    val log = Logger(getClass)

    import conversions.json.tojson
    import conversions.json.formats

    val Some(resourceOwner) = utils.ResourceOwner.unapply(req)

    val cb = Jsonp.Optional.unapply(req) getOrElse Jsonp.EmptyWrapper

    // -------------------------------------------------------------------------------------------------

    def downloadAvatar(userId: String) =
      oauthService.getAvatar(userId) match {
        case Some((avatarInfo, data)) =>

          import org.json4s.JsonDSL._

          JsonContent ~>
            ResponseString(
              cb wrap tojson(
                ("avatarInfo" -> org.json4s.Extraction.decompose(avatarInfo)) ~
                  ("data" -> com.owtelse.codec.Base64.encode(data))))

        case _ =>

          import org.json4s.JsonDSL._

          oauthService.getUser(userId) match {
            case Some(user) =>

              JsonContent ~>
                ResponseString(
                  cb wrap tojson(
                    ("avatarInfo" -> org.json4s.Extraction.decompose(domain.AvatarInfo("image/png"))) ~
                      ("data" -> org.json4s.JString(if(user.gender eq domain.Gender.Male) DefaultAvatars.Male else DefaultAvatars.Female))))

            case _ => NotFound
          }
      }

    def purgeAvatar(userId: String) =
      oauthService.updateUser(userId, new utils.DefaultUserSpec {
        override val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(None))
      }) match {
        case Some(user) =>

          JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => BadRequest
      }

    def uploadAvatar(userId: String, avatarInfo: domain.AvatarInfo, bytes: Array[Byte]) =
      oauthService.updateUser(userId, new utils.DefaultUserSpec {
        override val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(Some(avatarInfo, bytes)))
      }) match {
        case Some(user) => JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => BadRequest
      }

    // -------------------------------------------------------------------------------------------------

    def addUser() =
    // email
    // firstname and lastname
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
          x.email, passwd, x.firstname, x.lastname, Some(resourceOwner.id), x.gender, x.homeAddress, x.workAddress, x.contacts, x.passwordValid)
      } yield y) match {

        case Some(user) => JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => BadRequest
      }

    def updateUser(userId: String) =
    // email
    // firstname and lastname
    // gender
    // home and work addresses
    // contacts

      (for {
        json <- JsonBody(req)
        x <- oauthService.updateUser(userId, new utils.DefaultUserSpec {

          def findField(field: String) =
            json findField {
              case org.json4s.JField(`field`, _) => true
              case _ => false
            } collect {
              case org.json4s.JField(_, org.json4s.JString(s)) => s
            }

          def findFieldObj(field: String) =
            json findField {
              case org.json4s.JField(`field`, _) => true
              case _ => false
            } collect {
              case org.json4s.JField(_, o@org.json4s.JObject(_)) => o
            }

          override val email = findField("email")

          override val password = findField("password")
          override val oldPassword = findField("old_password")

          override val firstname = findField("firstname")
          override val lastname = findField("lastname")
          override val gender = findField("gender") flatMap (x => allCatch.opt {
            domain.Gender.withName(x)
          })

          override val homeAddress = new UpdateSpecImpl[domain.AddressInfo](
            set = findFieldObj("homeAddress") map (x => allCatch.opt {
                x.extract[domain.AddressInfo]
            })
          )

          override val workAddress = new UpdateSpecImpl[domain.AddressInfo](
            set = findFieldObj("workAddress") map (x => allCatch.opt {
              x.extract[domain.AddressInfo]
            })
          )
        })
      } yield x) match {

        case Some(user) => JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => BadRequest
      }

    def removeUser(userId: String) =
      JsonContent ~>
        ResponseString(cb wrap s"""{"success": ${oauthService.removeUser(userId)}}""")

    def getUser(userId: String) =
      oauthService.getUser(userId) match {
        case Some(user) => JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => NotFound
      }

    def getUsers = JsonContent ~> ResponseString(cb wrap tojson(oauthService.getUsers))

    def getTrash = JsonContent ~> ResponseString(cb wrap tojson(oauthService.getPurgedUsers))

    def purgeUsers(id: Set[String]) =
      JsonContent ~>
        ResponseString(cb wrap s"""{"success": ${
          allCatch.opt {
            oauthService.purgeUsers(id)
            true
          } getOrElse false
        }}""")

    def addContacts(userId: String) =
      (for {
        json <- JsonBody(req)
        x <- oauthService.updateUser(userId, new utils.DefaultUserSpec {

          def findField(field: String) =
            json findField {
              case org.json4s.JField(`field`, _) => true
              case _ => false
            } collect {
              case org.json4s.JField(_, x@org.json4s.JArray(_)) => x
            }

          override val contacts = Some(ContactInfoSpec(
            toAdd = findField("contacts") flatMap (x => allCatch.opt {
              x.extract[Set[domain.ContactInfo]]
            }) getOrElse Set()
          ))
        })
      } yield x) match {

        case Some(user) =>
          JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => BadRequest
      }

    def removeContacts(userId: String) =
      (for {
        json <- JsonBody(req)
        x <- oauthService.updateUser(userId, new utils.DefaultUserSpec {

          def findField(field: String) =
            json findField {
              case org.json4s.JField(`field`, _) => true
              case _ => false
            } collect {
              case org.json4s.JField(_, x@org.json4s.JArray(_)) => x
            }

          override val contacts = Some(ContactInfoSpec(
            toRem = findField("contacts") flatMap (x => allCatch.opt {
              x.extract[Set[domain.ContactInfo]]
            }) getOrElse Set()
          ))

        })
      } yield x) match {

        case Some(user) => JsonContent ~> ResponseString(cb wrap tojson(user))
        case _ => BadRequest
      }

    def grantRoles(userId: String, roles: Set[String]) =
      JsonContent ~>
        ResponseString(
          cb wrap s"""{"success": ${
            allCatch.opt {
              accessControlService.grantUserRoles(userId, roles, Some(resourceOwner.id))
              true
            } getOrElse false
          }}""")

    def revokeRoles(userId: String, roles: Set[String]) = 
      JsonContent ~>
        ResponseString(
          cb wrap s"""{"success": ${
            allCatch.opt {
              accessControlService.revokeUserRole(userId, roles)
              true
            } getOrElse false
          }}""")

    def getUserRoles(userId: String) =
      JsonContent ~>
        ResponseString(cb wrap tojson(accessControlService.getUserRoles(userId)))

    def userExists(email: String) =
      JsonContent ~> ResponseString(cb wrap s"""{"success": ${oauthService.emailExists(email)}}""")

    // ---------------------------------------------------------------------------------------------------

    def getRoles =
      JsonContent ~> ResponseString(cb wrap tojson(accessControlService.getRoles))

    def addRole() =
      (for {
        json <- JsonBody(req)
        x <- allCatch.opt {
//          org.json4s.native.Serialization.read[domain.Role](json)
            json.extract[domain.Role]
        }
        y <- accessControlService.saveRole(x.name, x.parent, Some(resourceOwner.id))
      } yield y) match {

        case Some(role) => JsonContent ~> ResponseString(cb wrap tojson(role))
        case _ => BadRequest
      }

    def roleExists(name: String) =
      JsonContent ~> ResponseString(cb wrap s"""{"success": ${accessControlService.roleExists(name)}""")

    def updateRole(name: String) =
      (for {
        json <- JsonBody(req)
        x <- allCatch.opt {
//          org.json4s.native.Serialization.read[domain.Role](json)
          json.extract[domain.Role]
        } if accessControlService.updateRole(name, x.name, x.parent)
        y <- accessControlService.getRole(x.name)
      } yield y) match {

        case Some(role) => JsonContent ~> ResponseString(cb wrap tojson(role))
        case _ => BadRequest
      }

    def getRole(name: String) =
      accessControlService.getRole(name) match {
        case Some(role) => JsonContent ~> ResponseString(cb wrap tojson(role))
        case _ => NotFound
      }

    def getUserSession = {

      val params = Map(
        /* "userId" -> resourceOwner.id, // -- NOT USED YET */
        "bearerToken" -> oauth2.Token.unapply(req).get,
        "userAgent" -> UserAgent.unapply(req).get
      )

      oauthService.getUserSession(params) match {
        case Some(session) =>

          JsonContent ~> ResponseString(
            cb wrap tojson(session))

        case _ => NotFound
      }
    }

    def purgeRoles(roles: Set[String]) =
      JsonContent ~> ResponseString(cb wrap s"""{"success": ${
        allCatch.opt {
          accessControlService.purgeRoles(roles)
          true
        } getOrElse false
      }""")

    def grantPermissions(name: String, permissions: Set[String]) =
      JsonContent ~>
        ResponseString(
          cb wrap s"""{"success": ${
            allCatch.opt {
              accessControlService.grantRolePermissions(name, permissions, Some(resourceOwner.id))
              true
            } getOrElse false
          }}""")

    def revokePermissions(name: String, permissions: Set[String]) =
      JsonContent ~>
        ResponseString(cb wrap s"""{"success": ${
          allCatch.opt {
            accessControlService.revokeRolePermission(name, permissions)
            true
          } getOrElse false
        }}""")

    def getPermissions =
      JsonContent ~>
        ResponseString(cb wrap tojson(accessControlService.getPermissions))

    def getRolePermissions(name: String) =
      JsonContent ~>
        ResponseString(cb wrap tojson(accessControlService.getRolePermissions(name)))

    def logout(token: String) =
      JsonContent ~> ResponseString(
        cb wrap s"""{"success": ${
          allCatch.opt {
            oauthService.revokeToken(token)
            true
          } getOrElse false
        }}"""
      )
  }

  def apply(req: HttpRequest[_ <: javax.servlet.http.HttpServletRequest]) = new MyRouteHandler(req)
}