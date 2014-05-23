package controllers.admin

import play.api.mvc._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.typesafe.plugin._

import ma.epsilon.schola._, http.HttpHelpers, jdbc._, domain._, conversions.json._, utils._

import ma.epsilon.schola.http.{ Façade, Secured }
import play.api.libs.iteratee.{ Enumerator, Iteratee }
import play.api.Logger

import scala.util.control.NonFatal

object Users extends Controller with Secured with HttpHelpers {

  def getUsers =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[User]](use[Façade].userService.getUsers)
          }
    }

  def getUser(id: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              use[Façade].userService.getUser(Uuid(id)) match {
                case Some(user) => json[User](user)
                case _          => NotFound
              }
          }
    }

  def getUserByCIN(cin: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              use[Façade].userService.getUserByCIN(cin) match {
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

          implicit object BooleanReads extends Reads[Boolean] {
            def reads(json: JsValue) = json match {
              case JsBoolean(b) => JsSuccess(b)
              case _            => JsSuccess(true)
            }
          }

          case class UserIn(
            cin: String,
            primaryEmail: String,
            // password: Option[String],
            givenName: String,
            familyName: String,
            jobTitle: String,
            gender: Gender,
            homeAddress: Option[AddressInfo],
            workAddress: Option[AddressInfo],
            contacts: Option[Contacts],
            active: Boolean,
            changePasswordAtNextLogin: Boolean,
            accessRights: Option[List[Uuid]])

          implicit val addressInfoReads = (
            (__ \ "city").readNullable[String] ~
            (__ \ "country").readNullable[String] ~
            (__ \ "postalCode").readNullable[String] ~
            (__ \ "streetAddress").readNullable[String])(AddressInfo)

          implicit val userInReads = (
            (__ \ "cin").read[String] ~
            (__ \ "primaryEmail").read[String](email) ~
            // (__ \ "password").readNullable(minLength[String](ma.epsilon.schola.PasswordMinLength)) ~
            (__ \ "givenName").read[String] ~
            (__ \ "familyName").read[String] ~
            (__ \ "jobTitle").read[String] ~
            (__ \ "gender").read[Gender] ~
            (__ \ "homeAddress").readNullable[AddressInfo] ~
            (__ \ "workAddress").readNullable[AddressInfo] ~
            (__ \ "contacts").readNullable[Contacts] ~
            (__ \ "active").read[Boolean] ~
            (__ \ "changePasswordAtNextLogin").read[Boolean] ~
            (__ \ "accessRights").readNullable[List[Uuid]])(UserIn)

          request.body.validate[UserIn].map {
            case UserIn(cin, primaryEmail /*, password*/ , givenName, familyName, jobTitle, gender, homeAddress, workAddress, contacts, active, changePasswordAtNextLogin, accessRights) =>

              try

                render {
                  case Accepts.Json() =>
                    json[User](
                      use[Façade].userService.saveUser(cin, primaryEmail /*, password getOrElse randomString(4)*/ , givenName, familyName, jobTitle, Some(Uuid(resourceOwner.id)), gender, homeAddress, workAddress, contacts, suspended = !active, changePasswordAtNextLogin, accessRights getOrElse Nil))
                }
              catch {

                case NonFatal(ex) =>
                  Logger.debug(s"[addUser failed $ex]")
                  BadRequest
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
            cin: Option[String],
            primaryEmail: Option[String],
            // oldPassword: Option[String],
            // password: Option[String],
            givenName: Option[String],
            familyName: Option[String],
            jobTitle: Option[String],
            gender: Option[Gender],
            homeAddress: Option[AddressInfo],
            workAddress: Option[AddressInfo],
            contacts: Option[Contacts],
            active: Option[Boolean],
            changePasswordAtNextLogin: Option[Boolean],
            accessRights: Option[List[Uuid]])

          implicit val addressInfoReads = (
            (__ \ "city").readNullable[String] ~
            (__ \ "country").readNullable[String] ~
            (__ \ "postalCode").readNullable[String] ~
            (__ \ "streetAddress").readNullable[String])(AddressInfo)

          implicit val userInReads = (
            (__ \ "cin").readNullable[String] ~
            (__ \ "primaryEmail").readNullable[String](email) ~
            // (__ \ "oldPassword").readNullable[String] ~
            // (__ \ "password").readNullable(minLength[String](ma.epsilon.schola.PasswordMinLength)) ~
            (__ \ "givenName").readNullable[String] ~
            (__ \ "familyName").readNullable[String] ~
            (__ \ "jobTitle").readNullable[String] ~
            (__ \ "gender").readNullable[Gender] ~
            (__ \ "homeAddress").readNullable[AddressInfo] ~
            (__ \ "workAddress").readNullable[AddressInfo] ~
            (__ \ "contacts").readNullable[Contacts] ~
            (__ \ "active").readNullable[Boolean] ~
            (__ \ "changePasswordAtNextLogin").readNullable[Boolean] ~
            (__ \ "accessRights").readNullable[List[Uuid]])(UserIn)

          request.body.validate[UserIn].map {
            case UserIn(sCIN, sPrimaryEmail /*, sOldPassword, sPassword*/ , sGivenName, sFamilyName, sJobTitle, sGender, sHomeAddress, sWorkAddress, sContacts, sActive, changePasswordAtNextLogin, sAccessRights) =>

              if (use[Façade].userService.updateUser(Uuid(id), new DefaultUserSpec {

                override lazy val contacts =
                  UpdateSpecImpl[ContactsSpec](
                    set = sContacts collect {
                      case Contacts(mobiles, home, work, site) =>
                        Some(ContactsSpec(

                          UpdateSpecImpl[MobileNumbersSpec](
                            set = mobiles collect {
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
                            }),

                          site = UpdateSpecImpl[String](set = None)))
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

                override lazy val cin = sCIN

                override lazy val givenName = sGivenName

                override lazy val familyName = sFamilyName

                override lazy val jobTitle = sJobTitle

                override lazy val gender = sGender

                override lazy val suspended = sActive map (!_)

                // override lazy val password = sPassword

                // override lazy val oldPassword = sOldPassword

                override lazy val updatedBy = Some(Uuid(resourceOwner.id))

                override lazy val accessRights = sAccessRights map (accessRights => SetSpec.only(Set(accessRights: _*))) getOrElse SetSpec.empty[Uuid]

              }))
                render {
                  case Accepts.Json() =>
                    json[Option[User]](use[Façade].userService.getUser(Uuid(id)))

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

          def result = tryo(use[Façade].userService.removeUsers(users map Uuid.apply))

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

          def result = tryo(use[Façade].userService.purgeUsers(users map Uuid.apply))

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

  def suspendUsers(users: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.suspendUsers(users map Uuid.apply))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def undeleteUsers(users: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.undeleteUsers(users map Uuid.apply))

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

  def getUserAccessRights(id: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[AccessRight]](use[Façade].oauthService.getUserAccessRights(Uuid(id)))
          }
    }

  /*  def getUserRoles(id: String) =
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
    }*/

  def getUserTags(id: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[String]](use[Façade].userService.getUserLabels(Uuid(id)))
          }
    }

  def addUserTags(id: String, labels: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].userService.labelUser(Uuid(id), labels))

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

          def result = tryo(use[Façade].userService.unLabelUser(Uuid(id), labels))

          render {
            case Accepts.Json() =>
              json[Response](Response(result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def downloadAvatar(userId: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          import scala.concurrent.Await
          import scala.concurrent.duration.Duration

          val file = use[Façade].uploadServices.getUpload(userId) map {
            case domain.Upload(filename, contentType, bytes, _) =>
              render {
                case Accepts.Json() =>
                  json[AvatarInfo](AvatarInfo(filename, contentType.getOrElse("application/octet-stream; charset=UTF-8"), com.owtelse.codec.Base64.encode(bytes), base64 = true))

                case _ =>

                  Ok.chunked(Enumerator(bytes))
                    .as(contentType.getOrElse("application/octet-stream; charset=UTF-8"))
              }
          } recover {
            case scala.util.control.NonFatal(_) => NotFound
          }

          Await.result(file, Duration.Inf)
    }

  def uploadAvatar(userId: String, filename: String) =
    withAuth(parse.temporaryFile) {
      _ =>
        implicit request =>

          import scala.concurrent.Await
          import scala.concurrent.duration.Duration

          val enumerator = Enumerator.fromFile(request.body.file)

          val result = enumerator.run(Iteratee.consume()) map {
            bytes =>

              try {
                use[Façade].uploadServices.upload(userId, filename, request.contentType, bytes)

                render {
                  case Accepts.Json() => json[Response](Response(success = true))
                  case _              => Ok
                }
              } catch {
                case scala.util.control.NonFatal(_) =>

                  render {
                    case Accepts.Json() => json[Response](Response(success = false))
                    case _              => BadRequest
                  }
              }
          }

          Await.result(result, Duration.Inf)
    }

  def purgeAvatar(userId: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          try {
            use[Façade].uploadServices.purgeUpload(userId)

            render {
              case Accepts.Json() => json[Response](Response(success = true))
              case _              => Ok
            }
          } catch {
            case scala.util.control.NonFatal(_) =>

              render {
                case Accepts.Json() => json[Response](Response(success = false))
                case _              => BadRequest
              }
          }
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
          case scala.util.control.NonFatal(_) => render {
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
          case scala.util.control.NonFatal(_) => render {
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
          case scala.util.control.NonFatal(_) => render {
            case Accepts.Json() => json[Response](Response(success = false))
            case _              => Ok
          }
        }
    }
}