package schola
package oadmin

package impl

trait AccessControlServicesComponentImpl extends AccessControlServicesComponent{
  self: AccessControlServicesRepoComponent =>

  val accessControlService = new AccessControlServicesImpl

  class AccessControlServicesImpl extends AccessControlServices{

    def getRoles = accessControlServiceRepo.getRoles

    def getRole(name: String) = accessControlServiceRepo.getRole(name)

    def getUserRoles(userId: String) = accessControlServiceRepo.getUserRoles(userId)

    def getPermissions = accessControlServiceRepo.getPermissions

    def getRolePermissions(role: String) = accessControlServiceRepo.getRolePermissions(role)

    def getUserPermissions(userId: String) = accessControlServiceRepo.getUserPermissions(userId)

    def getClientPermissions(clientId: String) = accessControlServiceRepo.getClientPermissions(clientId)

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]) = accessControlServiceRepo.saveRole(name, parent, createdBy)

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String]) = accessControlServiceRepo.grantRolePermissions(role, permissions, grantedBy)

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String]) = accessControlServiceRepo.grantUserRoles(userId, roles, grantedBy)

    def revokeUserRole(userId: String, roles: Set[String]) = accessControlServiceRepo.revokeUserRole(userId, roles)

    def revokeRolePermission(role: String, permissions: Set[String]) = accessControlServiceRepo.revokeRolePermission(role, permissions)

    def purgeRoles(roles: Set[String]) = accessControlServiceRepo.purgeRoles(roles)

    def userHasRole(userId: String, role: String) = accessControlServiceRepo.userHasRole(userId, role)

    def roleHasPermission(role: String, permissions: Set[String]) = accessControlServiceRepo.roleHasPermission(role, permissions)

    def roleExists(role: String) = accessControlServiceRepo.roleExists(role)

    def updateRole(name: String, newName: String, parent: Option[String]) = accessControlServiceRepo.updateRole(name, newName, parent)
  }
}

trait AccessControlServicesRepoComponentImpl extends AccessControlServicesRepoComponent {
  self : AccessControlServicesComponent =>

  import schema._
  import domain._
  import Q._

  protected val db: Database

  protected val accessControlServiceRepo = new AccessControlServiceRepoImpl

  class AccessControlServiceRepoImpl extends AccessControlServiceRepo{

    def getRoles = {
      import Database.dynamicSession

      val q = for {
        r <- Roles
      } yield (
          r.name,
          r.parent,
          r.createdAt,
          r.createdBy,
          r.public)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (name, parent, createdAt, createdBy, public) => Role(name, parent, createdAt, createdBy, public)
      }
    }

    def getRole(name: String) = {
      import Database.dynamicSession

      val q = for {
        r <- Roles if r.name is name
      } yield (
          r.name,
          r.parent,
          r.createdAt,
          r.createdBy,
          r.public)

      val result = db.withDynSession {
        q.firstOption
      }

      result map {
        case (name, parent, createdAt, createdBy, public) => Role(name, parent, createdAt, createdBy, public)
      }
    }

    def getUserRoles(userId: String) = {
      import Database.dynamicSession

      val q = for{
        u <- UsersRoles if u.userId is java.util.UUID.fromString(userId)
      } yield (
          u.userId,
          u.role,
          u.grantedAt,
          u.grantedBy
          )

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (sUserId, role, grantedAt, grantedBy) => UserRole(sUserId, role, grantedAt, grantedBy)
      }
    }

    def getPermissions = {
      import Database.dynamicSession

      val q = for {
        p <- Permissions
      } yield (
          p.name,
          p.clientId)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (name, clientId) => Permission(name, clientId)
      }
    }

    def getRolePermissions(role: String) = {
      import Database.dynamicSession

      val q = for{
        r <- RolesPermissions if r.role is role
      } yield (
          r.role,
          r.permission,
          r.grantedAt,
          r.grantedBy
          )

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (sRole, permission, grantedAt, grantedBy) => RolePermission(sRole, permission, grantedAt, grantedBy)
      }
    }

    def getUserPermissions(userId: String) = {
      import Database.dynamicSession

      val q = for {
        rp <- RolesPermissions
        ur <- UsersRoles if ur.role is rp.role
        u  <- ur.user if u.id is java.util.UUID.fromString(userId)
      } yield rp.permission

      Set(
        db.withDynSession {
          q.list
        } : _*
      )
    }

    def getClientPermissions(clientId: String) = {
      import Database.dynamicSession

      val q = for {
        p <- Permissions if p.clientId is clientId
      } yield (
          p.name,
          p.clientId)

      val result = db.withDynSession {
        q.list
      }

      result map {
        case (name, sClientId) => Permission(name, sClientId)
      }
    }

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]) = db.withTransaction { implicit session =>
      Roles += Role(name, parent orElse Some(AdministratorR.name), System.currentTimeMillis, createdBy = createdBy map java.util.UUID.fromString)

      val q = for{
        r <- Roles if r.name is name
      } yield(
        r.name,
        r.parent,
        r.createdAt,
        r.createdBy,
        r.public)

      q.firstOption map {
        case (sName, sParent, createdAt, sCreatedBy, public) => Role(sName, sParent, createdAt, sCreatedBy, public)
      }
    }

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String]) = db.withTransaction { implicit session =>
      RolesPermissions ++= (permissions map (RolePermission(role, _, grantedBy = grantedBy map java.util.UUID.fromString)))
    }

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String]) = db.withTransaction { implicit session =>
      UsersRoles ++= (roles map (UserRole(java.util.UUID.fromString(userId), _, grantedBy = grantedBy map java.util.UUID.fromString)))
    }

    def revokeUserRole(userId: String, roles: Set[String]) = db.withTransaction { implicit session =>
      val q = for { arg <- UsersRoles if (arg.userId is java.util.UUID.fromString(userId)) && (arg.role inSet roles) } yield arg
      q.delete
    }

    def revokeRolePermission(role: String, permissions: Set[String]) = db.withTransaction { implicit session =>
      val q = for { arg <- RolesPermissions if (arg.role is role) && (arg.permission inSet permissions) } yield arg
      q.delete
    }

    def purgeRoles(roles: Set[String]) = db.withTransaction { implicit session =>
      val q = for { r <- Roles if !r.public && (r.name inSet roles) } yield r
      q.delete
    }

    def userHasRole(userId: String, role: String) = {
      import Database.dynamicSession

      val id = java.util.UUID.fromString(userId)

      @inline def getParent(role: String) =
        (for { r <- Roles  if r.name is role } yield r.parent).firstFlatten


      @scala.annotation.tailrec def userHasRoleAcc(role: String): Boolean = {

        val q = for {
          ur <- UsersRoles if (ur.userId is id) && (ur.role is role)
        } yield true

        (Query(q.exists).firstOption getOrElse false) || (getParent(role) match {
          case Some(parent) => userHasRoleAcc(parent)
          case _ => false
        })
      }

      db.withDynSession {
        userHasRoleAcc(role)
      }
    }

    def roleHasPermission(role: String, permissions: Set[String]) = db.withSession { implicit session =>
      val q = for {
        r <- RolesPermissions if (r.role is role) && (r.permission inSet permissions)
      } yield true

      Query(q.exists).firstOption getOrElse false
    }

    def roleExists(name: String) = db.withSession { implicit session =>
      val q = for {
        r <- Roles if r.name is name
      } yield true

      Query(q.exists).firstOption getOrElse false
    }

    def updateRole(name: String, newName: String, parent: Option[String]) =
    db.withTransaction{ implicit sesssion =>
      (Roles where(_.name is name) map(o => (o.name, o.parent)) update(newName, parent)) == 1
    }
  }
}