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

          import play.api.libs.json._, Reads._
          import play.api.libs.functional.syntax._

          case class UserIn(
            primaryEmail: String,
            password: Option[String],
            givenName: String,
            familyName: String,
            gender: Gender,
            homeAddress: Option[AddressInfo],
            workAddress: Option[AddressInfo],
            contacts: Option[Contacts],
            changePasswordAtNextLogin: Boolean)

          implicit val addressInfoReads = (
            (__ \ "city").readNullable[String] ~
            (__ \ "country").readNullable[String] ~
            (__ \ "postalCode").readNullable[String] ~
            (__ \ "streetAddress").readNullable[String]
          )(AddressInfo)
          
          implicit val userInReads = (
            (__ \ "primaryEmail").read[String](email) ~
            (__ \ "password").readNullable(minLength[String](schola.oadmin.PasswordMinLength)) ~
            (__ \ "givenName").read[String] ~
            (__ \ "familyName").read[String] ~
            (__ \ "gender").read[Gender] ~
            (__ \ "homeAddress").readNullable[AddressInfo] ~
            (__ \ "workAddress").readNullable[AddressInfo] ~
            (__ \ "contacts").readNullable[Contacts] ~
            (__ \ "changePasswordAtNextLogin").read[Boolean]
          )(UserIn)

          request.body.validate[UserIn].map {
            case UserIn(primaryEmail, password, givenName, familyName, gender, homeAddress, workAddress, contacts, changePasswordAtNextLogin) =>

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

          import play.api.libs.json._, Reads._
          import play.api.libs.functional.syntax._

          case class UserIn(
            primaryEmail: Option[String],
            oldPassword: Option[String],
            password: Option[String],
            givenName: Option[String],
            familyName: Option[String],
            gender: Option[Gender],
            homeAddress: Option[AddressInfo],
            workAddress: Option[AddressInfo],
            contacts: Option[Contacts],
            changePasswordAtNextLogin: Option[Boolean]) 

          implicit val addressInfoReads = (
            (__ \ "city").readNullable[String] ~
            (__ \ "country").readNullable[String] ~
            (__ \ "postalCode").readNullable[String] ~
            (__ \ "streetAddress").readNullable[String]
          )(AddressInfo)
          
          implicit val userInReads = (
            (__ \ "primaryEmail").readNullable[String](email) ~
            (__ \ "oldPassword").readNullable[String] ~
            (__ \ "password").readNullable(minLength[String](schola.oadmin.PasswordMinLength)) ~
            (__ \ "givenName").readNullable[String] ~
            (__ \ "familyName").readNullable[String] ~
            (__ \ "gender").readNullable[Gender] ~
            (__ \ "homeAddress").readNullable[AddressInfo] ~
            (__ \ "workAddress").readNullable[AddressInfo] ~
            (__ \ "contacts").readNullable[Contacts] ~
            (__ \ "changePasswordAtNextLogin").readNullable[Boolean]
          )(UserIn)              

          request.body.validate[UserIn].map {
            case UserIn(sPrimaryEmail, sOldPassword, sPassword, sGivenName, sFamilyName, sGender, sHomeAddress, sWorkAddress, sContacts, changePasswordAtNextLogin) =>

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

                override lazy val homeAddress =
                  UpdateSpecImpl[AddressInfoSpec](
                    set = sHomeAddress collect {
                      case AddressInfo(city, country, postalCode, streetAddress) =>
                          Some(AddressInfoSpec(
                            city = UpdateSpecImpl[String](set = city map Option[String]),
                            country = UpdateSpecImpl[String](set = country map Option[String]),
                            postalCode = UpdateSpecImpl[String](set = postalCode map Option[String]),
                            streetAddress = UpdateSpecImpl[String](set = streetAddress map Option[String])))
                    })

                override lazy val workAddress =
                  UpdateSpecImpl[AddressInfoSpec](
                    set = sWorkAddress collect {
                      case AddressInfo(city, country, postalCode, streetAddress) =>
                          Some(AddressInfoSpec(
                            city = UpdateSpecImpl[String](set = city map Option[String]),
                            country = UpdateSpecImpl[String](set = country map Option[String]),
                            postalCode = UpdateSpecImpl[String](set = postalCode map Option[String]),
                            streetAddress = UpdateSpecImpl[String](set = streetAddress map Option[String])))
                    })                

                override lazy val primaryEmail = sPrimaryEmail

                override lazy val givenName = sGivenName

                override lazy val familyName = sFamilyName

                override lazy val gender = sGender

                override lazy val password = sPassword

                override lazy val oldPassword = sOldPassword
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

  def getUserTags(id: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[String]](use[Façade].userService.getUserLabels(id))
          }
    }

  def addUserTags(id: String, labels: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.labelUser(id, labels))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def purgeUserTags(id: String, labels: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.unLabelUser(id, labels))

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
              case _: Throwable => NotFound
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