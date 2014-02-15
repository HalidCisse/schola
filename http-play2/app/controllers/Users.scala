package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsError

import com.typesafe.plugin._

import schola.oadmin._, domain._, conversions.json._, utils._

import schola.oadmin.http.{ ResourceOwner, Façade, Secured }
import play.api.libs.iteratee.{ Enumerator, Iteratee }

object Users extends Controller with Secured with Helpers  {

  def getUsers(page: Int) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[User]](use[Façade].userService.getUsers(page))
          }
    }

  def getUser(id: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              use[Façade].userService.getUser(id) match {
                case Some(user) => json[User](user)
                case _          => NotFound
              }
          }
    }

  def addUser =
    withAuth(parse.json) {
      resourceOwner =>
        implicit request =>

          request.body.validate[domain.User].map {
            case domain.User(primaryEmail, password, givenName, familyName, _, _, _, _, _, gender, homeAddress, workAddress, contacts, _, _, _, _, changePasswordAtNextLogin, _, _) =>

              use[Façade].userService.saveUser(primaryEmail, password getOrElse randomString(4), givenName, familyName, Some(resourceOwner.id), gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) match {
                case Some(user) =>

                  render {
                    case Accepts.Json() =>
                      json[User](user)

                    case _ => Ok
                  }

                case _ => BadRequest
              }

          }.recoverTotal {
            errors => BadRequest(JsError.toFlatJson(errors))
          }
    }

  def updateUser(id: String) =
    withAuth(parse.json) {
      resourceOwner =>
        implicit request =>

          request.body.validate[domain.User].map {
            case domain.User(sPrimaryEmail, sPassword, sGivenName, sFamilyName, _, _, _, _, _, sGender, sHomeAddress, sWorkAddress, sContacts, _, _, _, _, _, _, _) =>

              if (use[Façade].userService.updateUser(id, new DefaultUserSpec {

                override lazy val contacts =
                  UpdateSpecImpl[ContactsSpec](
                    set = sContacts collect {
                      case Contacts(mobiles, home, work) =>
                        Some(ContactsSpec(

                          UpdateSpecImpl[MobileNumbersSpec](set = mobiles collect {
                            case MobileNumbers(mobile1, mobile2) =>
                              Some(MobileNumbersSpec(
                                UpdateSpecImpl[String](set = mobile1 map Option[String]),
                                UpdateSpecImpl[String](set = mobile2 map Option[String])))
                          }),

                          UpdateSpecImpl[ContactInfoSpec](
                            set = home collect {
                              case ContactInfo(email, phoneNumber, fax) => Some(ContactInfoSpec(
                                UpdateSpecImpl[String](set = email map Option[String]),
                                UpdateSpecImpl[String](set = fax map Option[String]),
                                UpdateSpecImpl[String](set = phoneNumber map Option[String])))
                            }),

                          UpdateSpecImpl[ContactInfoSpec](
                            set = work collect {
                              case ContactInfo(email, phoneNumber, fax) => Some(ContactInfoSpec(
                                UpdateSpecImpl[String](set = email map Option[String]),
                                UpdateSpecImpl[String](set = fax map Option[String]),
                                UpdateSpecImpl[String](set = phoneNumber map Option[String])))
                            })))
                    })

                override lazy val homeAddress = UpdateSpecImpl[AddressInfo](set = sHomeAddress map Option[AddressInfo])

                override lazy val workAddress = UpdateSpecImpl[AddressInfo](set = sWorkAddress map Option[AddressInfo])

                override lazy val primaryEmail = Some(sPrimaryEmail)

                override lazy val givenName = Some(sGivenName)

                override lazy val familyName = Some(sFamilyName)

                override lazy val gender = Some(sGender)

                override lazy val password = sPassword

                override lazy val oldPassword = (request.body \ "old_password").asOpt[String]
              }))
                render {
                  case Accepts.Json() =>
                    json[Option[User]](use[Façade].userService.getUser(id))

                  case _ => Ok
                }

              else BadRequest

          }.recoverTotal {
            errors => BadRequest(JsError.toFlatJson(errors))
          }
    }

  def deleteUsers(users: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.removeUsers(users))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def purgeUsers(users: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.purgeUsers(users))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def getUsersStats =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[UsersStats](use[Façade].userService.getUsersStats)
          }
    }

  def getPurgedUsers =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[User]](use[Façade].userService.getPurgedUsers)
          }
    }

  def undeleteUsers(users: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.undeleteUsers(users))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def userExists(username: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = use[Façade].userService.primaryEmailExists(username)

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def getUserRoles(id: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[UserRole]](use[Façade].accessControlService.getUserRoles(id))
          }
    }

  def grantUserRoles(id: String, roles: List[String]) =
    withAuth {
      user: ResourceOwner =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].accessControlService.grantUserRoles(id, roles, Some(user.id)))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def revokeUserRoles(id: String, roles: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].accessControlService.revokeUserRoles(id, roles))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def getUserTags =
    withAuth {
      user: ResourceOwner =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[UserLabel]](use[Façade].userService.getUserLabels(user.id))
          }
    }

  def addUserTags(labels: List[String]) =
    withAuth {
      user: ResourceOwner =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.labelUser(user.id, labels))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def purgeUserTags(labels: List[String]) =
    withAuth {
      user: ResourceOwner =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.unLabelUser(user.id, labels))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def downloadAvatar(avatarId: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          import scala.concurrent.Await
          import scala.concurrent.duration.Duration

          Await.result(
            use[Façade].avatarServices.getAvatar(avatarId) map {
              case (filename, contentType, bytes) =>
                render {
                  case Accepts.Json() =>
                    json[AvatarInfo](AvatarInfo(filename, contentType.getOrElse("application/octet-stream; charset=UTF-8"), com.owtelse.codec.Base64.encode(bytes), base64 = true))

                  case _ =>

                    Ok.chunked(Enumerator(bytes))
                      .as(contentType.getOrElse("application/octet-stream; charset=UTF-8"))
                }
            } recover {
              case _: Throwable => BadRequest
            }, Duration.Inf)
    }

  def uploadAvatar(id: String, name: String) =
    withAuth(parse.temporaryFile) {
      _ =>
        implicit request =>

          import scala.concurrent.Await
          import scala.concurrent.duration.Duration

          Await.result(
            {
              Enumerator.fromFile(request.body.file) |>>> Iteratee.consume[Array[Byte]]()
            } flatMap {
              bytes =>

                use[Façade].avatarServices.uploadAvatar(id, name, request.contentType, bytes) map {
                  success =>

                    render {
                      case Accepts.Json() => json[Response](Response(success))
                      case _              => Ok
                    }

                } recover {
                  case _: Throwable =>

                    render {
                      case Accepts.Json() => json[Response](Response(success = false))
                      case _              => BadRequest
                    }
                }
            }, Duration.Inf)
    }

  def purgeAvatar(userId: String, avatarId: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          import scala.concurrent.Await
          import scala.concurrent.duration.Duration

          Await.result(
            use[Façade].avatarServices.purgeAvatar(userId, avatarId) map {
              success =>

                render {
                  case Accepts.Json() => json[Response](Response(success))
                  case _              => Ok
                }

            } recover {
              case _: Throwable =>

                render {
                  case Accepts.Json() => json[Response](Response(success = false))
                  case _              => BadRequest
                }

            }, Duration.Inf)
    }

  def checkActivationReq(username: String, key: String) =
    Action.async {
      implicit request =>
        scala.concurrent.Future {
          if (use[Façade].userService.checkActivationReq(username, key))
            render {
              case Accepts.Json() => json[Response](Response(success = true))
              case _              => Ok
            }
          else BadRequest
        } recover {
          case _: Throwable => render {
            case Accepts.Json() => json[Response](Response(success = false))
            case _              => NotFound
          }
        }
    }

  def lostPasswd(username: String) =
    Action.async {
      implicit request =>
        scala.concurrent.Future {
          if (tryo(use[Façade].userService.createPasswdResetReq(username)))
            render {
              case Accepts.Json() => json[Response](Response(success = true))
              case _              => Ok
            }
          else BadRequest
        } recover {
          case _: Throwable => render {
            case Accepts.Json() => json[Response](Response(success = false))
            case _              => Ok
          }
        }
    }

  def resetPasswd(username: String, key: String, newPassword: String) =
    Action.async {
      implicit request =>
        scala.concurrent.Future {
          if (use[Façade].userService.resetPasswd(username, key, newPassword))
            render {
              case Accepts.Json() => json[Response](Response(success = true))
              case _              => Ok
            }
          else BadRequest
        } recover {
          case _: Throwable => render {
            case Accepts.Json() => json[Response](Response(success = false))
            case _              => Ok
          }
        }
    }

  def logout(token: String) =
    Action {

      tryo { use[Façade].oauthService.revokeToken(token) }

      Ok
    }
}