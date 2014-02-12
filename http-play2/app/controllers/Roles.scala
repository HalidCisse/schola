package controllers

import play.api.mvc._
import play.api.Play.current

import schola.oadmin._, domain._, http.{ResourceOwner, Façade, Secured}, utils._, conversions.json._
import com.typesafe.plugin._

object Roles extends Controller with Secured with Helpers {

  def getRoles =
    withAuth {
      _ => implicit request: RequestHeader =>

        render {
          case Accepts.Json() =>
            json[List[Role]](use[Façade].accessControlService.getRoles)
        }
    }

  def getRole(name: String) =
    withAuth {
      _ => implicit request: RequestHeader =>

        render {
          case Accepts.Json() =>
            use[Façade].accessControlService.getRole(name) match {
              case Some(role) => json[Role](role)
              case _ => NotFound
            }
        }
    }

  def addRole(name: String, parent: Option[String]) =
    withAuth {
      resourceOwner: ResourceOwner => implicit request: RequestHeader =>

        use[Façade].accessControlService.saveRole(name, parent, Some(resourceOwner.id)) match {
          case Some(role) =>

            render {
              case Accepts.Json() =>
                json[Role](role)
            }

          case _ => BadRequest
        }
    }

  def updateRole(name: String, newName: String, parent: Option[String]) =
    withAuth {
      _ => implicit request: RequestHeader =>

        if (use[Façade].accessControlService.updateRole(name, newName, parent))

          render {
            case Accepts.Json() =>
              json[Role](use[Façade].accessControlService.getRole(name))
          }

        else BadRequest
    }

  def purgeRoles(roles: List[String]) =
    withAuth {
      _ => implicit request: RequestHeader =>

        def result = tryo(use[Façade].accessControlService.purgeRoles(roles))

        render {
          case Accepts.Json() =>
            json[Response](Response(result))

          case _ =>

            if (result) Ok
            else BadRequest
        }
    }

  def getPermissions =
    withAuth {
      _ => implicit request: RequestHeader =>

        render {
          case Accepts.Json() =>
            json[List[Permission]](use[Façade].accessControlService.getPermissions)
        }
    }

  def getRolePermissions(name: String) =
    withAuth {
      _ => implicit request: RequestHeader =>

        render {
          case Accepts.Json() =>
            json[List[RolePermission]](use[Façade].accessControlService.getRolePermissions(name))
        }
    }

  def grantPermissions(name: String, permissions: List[String]) =
    withAuth {
      user: ResourceOwner => implicit request: RequestHeader =>

        def result = tryo(use[Façade].accessControlService.grantRolePermissions(name, permissions, Some(user.id)))

        render {
          case Accepts.Json() =>
            json[Response](Response(result))

          case _ =>

            if (result) Ok
            else BadRequest
        }
    }

  def revokePermissions(name: String, permissions: List[String]) =
    withAuth {
      _ => implicit request: RequestHeader =>

        def result = tryo(use[Façade].accessControlService.revokeRolePermission(name, permissions))

        render {
          case Accepts.Json() =>
            json[Response](Response(result))

          case _ =>

            if (result) Ok
            else BadRequest
        }
    }

  def roleExists(name: String) =
    withAuth {
      _ => implicit request: RequestHeader =>

        def result = use[Façade].accessControlService.roleExists(name)

        render {
          case Accepts.Json() =>
            json[Response](Response(result))

          case _ =>

            if (result) Ok
            else BadRequest
        }
    }
}