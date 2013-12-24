package schola
package oadmin

import javax.servlet.http.HttpServletResponse

object Façade extends Façade
with HandlerFactory {

  // --------------------------------------------------------------------------------------------------

  import javax.servlet.http.HttpServletRequest
  import unfiltered.request._
  import unfiltered.response._

  import scala.util.control.Exception.allCatch

  import unfiltered.request.Jsonp

  implicit class Ctx(val req: HttpRequest[_ <: HttpServletRequest]) {
    val Some(resourceOwner) = utils.ResourceOwner.unapply(req)

    val cb = Jsonp.Optional.unapply(req) getOrElse Jsonp.EmptyWrapper
  }

  implicit def req2async[T <: HttpServletRequest, B <: HttpServletResponse](req: HttpRequest[T]) = req.asInstanceOf[HttpRequest[T] with unfiltered.Async.Responder[B]]

  class MyRouteHandler(val ctx: Ctx) extends AnyVal with RouteHandler {

    import conversions.json.tojson
    import conversions.json.formats

    import ctx._

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
        override val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(None))
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
        override val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(Some(avatarInfo, bytes)))
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

        case Some(user) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
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

          req.respond {
            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
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

        case Some(user) =>

          req.respond {
            JsonContent ~>
              ResponseString(cb wrap tojson(user))
          }

        case _ => req.respond(BadRequest)
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

    def userExists(email: String) = {
      val resp = oauthService.emailExists(email)

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

  def apply(req: HttpRequest[_ <: HttpServletRequest]) = new MyRouteHandler(req)
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

    def addContacts(userId: String)

    def removeContacts(userId: String)

    def grantRoles(userId: String, roles: Set[String])

    def revokeRoles(userId: String, roles: Set[String])

    def getUserRoles(userId: String)

    def userExists(email: String)

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

  trait HandlerFactory extends (unfiltered.request.HttpRequest[_ <: javax.servlet.http.HttpServletRequest] => RouteHandler)

  class Façade extends impl.OAuthServicesRepoComponentImpl
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

    val cacheSystem = new impl.CacheSystem(60 * 30) // TODO: make `max-ttl` a config value

    protected val db = {
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
        import scala.slick.jdbc.{StaticQuery=>T}

        val ddl = OAuthTokens.ddl ++ OAuthClients.ddl ++ Users.ddl ++ Roles.ddl ++ Permissions.ddl ++ UsersRoles.ddl ++ RolesPermissions.ddl
        ddl.drop

        T.updateNA("DROP EXTENSION \"uuid-ossp\";")
    }

    def init(userId: java.util.UUID) = db withTransaction {
      implicit session =>

        import scala.slick.jdbc.{StaticQuery=>T}

        val ddl = OAuthTokens.ddl ++ OAuthClients.ddl ++ Users.ddl ++ Roles.ddl ++ Permissions.ddl ++ UsersRoles.ddl ++ RolesPermissions.ddl
        //    ddl.createStatements foreach (stmt => println(stmt+";"))
        ddl.create

        T.updateNA("CREATE EXTENSION \"uuid-ossp\";")

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
          UserRole(userId, "Role One", grantedBy = None),
          UserRole(userId, "Role Three", grantedBy = Some(userId)),
          UserRole(userId, "Role Two", grantedBy = None)
        )) == Some(3)

        _1 && _2 && _3 && _4 && _5 && _6
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