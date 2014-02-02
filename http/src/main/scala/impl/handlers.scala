package schola
package oadmin

package impl

trait HandlerComponent extends ServiceComponentFactory with HandlerFactory {

  import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }
  import unfiltered.request._
  import unfiltered.response._

  import scala.util.control.Exception.allCatch

  implicit class RequestContext(val req: HttpRequest[_ <: HttpServletRequest]) {
    lazy val Some(resourceOwner) = ResourceOwner.unapply(req)

    val cb = Jsonp.Optional.unapply(req) getOrElse Jsonp.EmptyWrapper
  }

  implicit def async[T <: HttpServletRequest, B <: HttpServletResponse](req: HttpRequest[T]) = req.asInstanceOf[HttpRequest[T] with unfiltered.Async.Responder[B]]

  class RouteHandlerImpl(val context: RequestContext)(implicit system: akka.actor.ActorSystem) extends RouteHandler {

    import conversions.json.tojson
    import conversions.json.formats

    import context._

    import simple._

    import system.dispatcher

    // -------------------------------------------------------------------------------------------------

    def downloadAvatar(avatarId: String) =
      oauthService.getAvatar(avatarId) onComplete {
        case scala.util.Success((filename, contentType, data)) =>

          req.respond {

            JsonContent ~>
              ResponseString(
                cb wrap tojson(
                  domain.AvatarInfo(
                    filename,
                    contentType.getOrElse("application/octet-stream; charset=UTF-8"),
                    com.owtelse.codec.Base64.encode(data),
                    base64 = true)))
          }

        case _ =>

          req.respond(NotFound)
      }

    def purgeAvatar(userId: String, avatarId: String) =
      oauthService.purgeAvatar(userId, avatarId) onComplete {
        case scala.util.Success(ok) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap s"""{"success": $ok}""")
          }

        case _ =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap s"""{"success": false}""")
          }
      }

    def uploadAvatar(userId: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
      oauthService.uploadAvatar(userId, filename, contentType, bytes) onComplete {
        case scala.util.Success(ok) =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap s"""{"success": $ok}""")
          }

        case _ =>

          req.respond {

            JsonContent ~>
              ResponseString(cb wrap s"""{"success": false}""")
          }
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
        if oauthService.updateUser(userId, new domain.DefaultUserSpec {

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
            }))

          override lazy val workAddress = new UpdateSpecImpl[domain.AddressInfo](
            set = findFieldObj("workAddress") map (x => allCatch.opt {
              x.extract[domain.AddressInfo]
            }))

          override lazy val contacts =
            utils.findFieldJObj(json)("contacts") map {
              contactsJson =>

                ContactsSpec(

                  mobiles = {
                    val mobilesJson = utils.findFieldJObj(contactsJson)("mobiles") getOrElse org.json4s.JObject(List())

                    MobileNumbersSpec(
                      mobile1 = UpdateSpecImpl[String](
                        set = utils.findFieldJObj(mobilesJson)("mobile1") flatMap (o => utils.findFieldStr(o)("phoneNumber")) map (s => utils.If(s.isEmpty, None, Some(s)))),

                      mobile2 = UpdateSpecImpl[String](
                        set = utils.findFieldJObj(mobilesJson)("mobile2") flatMap (o => utils.findFieldStr(o)("phoneNumber")) map (s => utils.If(s.isEmpty, None, Some(s)))))
                  },

                  home = utils.findFieldJObj(contactsJson)("home") flatMap (x => allCatch.opt {
                    ContactInfoSpec[domain.ContactInfo](
                      email = UpdateSpecImpl[String](
                        set = utils.findFieldStr(x)("email") map (s => utils.If(s.isEmpty, None, Some(s)))),

                      fax = UpdateSpecImpl[String](
                        set = utils.findFieldStr(x)("fax") map (s => utils.If(s.isEmpty, None, Some(s)))),

                      phoneNumber = UpdateSpecImpl[String](
                        set = utils.findFieldStr(x)("phoneNumber") map (s => utils.If(s.isEmpty, None, Some(s)))))
                  }),

                  work = utils.findFieldJObj(contactsJson)("work") flatMap (x => allCatch.opt {
                    ContactInfoSpec[domain.ContactInfo](
                      email = UpdateSpecImpl[String](
                        set = utils.findFieldStr(x)("email") map (s => utils.If(s.isEmpty, None, Some(s)))),

                      fax = UpdateSpecImpl[String](
                        set = utils.findFieldStr(x)("fax") map (s => utils.If(s.isEmpty, None, Some(s)))),

                      phoneNumber = UpdateSpecImpl[String](
                        set = utils.findFieldStr(x)("phoneNumber") map (s => utils.If(s.isEmpty, None, Some(s)))))
                  }))
            }
        })
        x <- oauthService.getUser(userId)
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

    def getUserSession = {

      val params = Map(
        /* "userId" -> resourceOwner.id, // -- NOT USED YET */
        "bearerToken" -> oauth2.Token.unapply(req).get,
        "userAgent" -> UserAgent.unapply(req).get)

      oauthService.getUserSession(params) match {
        case Some(session) =>

          JsonContent ~> ResponseString(
            cb wrap tojson(session))

        case _ => NotFound
      }
    }

    def getUsersStats = {
      val resp = oauthService.getUsersStats

      req.respond {

        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def getUsers(page: Int) = {
      val resp = oauthService.getUsers(page)

      req.respond {

        JsonContent ~>
          ResponseString(cb wrap tojson(resp))
      }
    }

    def resetPasswd(username: String, activationKey: String, newPasswd: String) = {
      val resp = oauthService.resetPasswd(username, activationKey, newPasswd)

      JsonContent ~>
        ResponseString(cb wrap s"""{"success": $resp}""")
    }

    def checkActivationReq(username: String, key: String) = {
      val resp = oauthService.checkActivationReq(username, key)

      JsonContent ~>
        ResponseString(cb wrap s"""{"success": $resp}""")
    }    

    def createPasswdResetReq(username: String) = {
      val resp = allCatch.opt {
        oauthService.createPasswdResetReq(username)
        true
      } getOrElse false

      JsonContent ~>
        ResponseString(cb wrap s"""{"success": $resp}""")
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
          cb wrap s"""{"success": $resp}""")
      }
    }
  }
}

object ResourceOwner {

  import javax.servlet.http.HttpServletRequest
  import unfiltered.request._

  def unapply[T <: HttpServletRequest](request: HttpRequest[T]): Option[unfiltered.oauth2.ResourceOwner] =
    request.underlying.getAttribute(unfiltered.oauth2.OAuth2.XAuthorizedIdentity) match {
      case sId: String => Some(new unfiltered.oauth2.ResourceOwner { val id = sId; val password = None })
      case _           => None
    }
}