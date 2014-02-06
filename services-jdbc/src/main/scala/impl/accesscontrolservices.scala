package schola
package oadmin

package impl

trait AccessControlServicesComponentImpl extends AccessControlServicesComponent {
  this: AccessControlServicesRepoComponent =>

  class AccessControlServicesImpl extends AccessControlServices {

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
  this: AccessControlServicesComponent =>

  import schema._
  import domain._
  import Q._

  private[this] val log = Logger("oadmin.AccessControlServicesRepoComponentImpl")

  protected val db: Database

  protected val accessControlServiceRepo = new AccessControlServiceRepoImpl

  class AccessControlServiceRepoImpl extends AccessControlServiceRepo {

    private[this] object oq {

      val roles =
        Compiled(for {
          r <- Roles
        } yield (
          r.name,
          r.parent,
          r.createdAt,
          r.createdBy,
          r.publiq))

      val named = {
        def getRole(name: Column[String]) =
          for {
            r <- Roles if r.name is name
          } yield (
            r.name,
            r.parent,
            r.createdAt,
            r.createdBy,
            r.publiq)

        Compiled(getRole _)
      }

      val userRoles = {
        def getUserRoles(id: Column[java.util.UUID]) =
          for {
            (ch, ur) <- Roles leftJoin UsersRoles on (_.name is _.role)
            if (ur.userId is id) || (ch.parent in (UsersRoles where (_.userId is id) map (_.role)))
          } yield (
            ur.userId?,
            ch.name,
            ur.grantedAt?,
            ur.grantedBy)

        Compiled(getUserRoles _)
      }

      val userRoleExists = {
        def getUserRole(id: Column[java.util.UUID], role: Column[String]) =
          Query(UsersRoles where (ur => (ur.userId is id) && (ur.role is role)) exists)

        Compiled(getUserRole _)
      }

      val permissions = Compiled(for {
        p <- Permissions
      } yield (
        p.name,
        p.clientId))

      val rolePermissions = {
        def getRolePermissions(name: Column[String]) =
          for {
            r <- RolesPermissions if r.role is name
          } yield (
            r.role,
            r.permission,
            r.grantedAt,
            r.grantedBy)

        Compiled(getRolePermissions _)
      }

      val userPermissions =
        userRoles flatMap {
          f =>

            def getUserPermissions(id: Column[java.util.UUID]) =
              for {
                (rp, _) <- RolesPermissions leftJoin f(id) on (_.role is _._2)
              } yield rp.permission

            Compiled(getUserPermissions _)
        }

      val clientPermissions = {
        def getClientPermissions(clientId: Column[String]) =
          for {
            p <- Permissions if p.clientId is clientId
          } yield (
            p.name,
            p.clientId)

        Compiled(getClientPermissions _)
      }

      val parent = {
        def getParent(role: Column[String]) =
          for { r <- Roles if r.name is role } yield r.parent

        Compiled(getParent _)
      }

      val roleExists = {
        def getRole(name: Column[String]) =
          Query(Roles where (_.name.toLowerCase is name) exists)

        Compiled(getRole _)
      }

      val forUpdate = {
        def updateRole(name: Column[String]) =
          Roles where (r => (r.name isNot R.SuperUserR.name) && (r.name is name)) map (o => (o.name, o.parent))

        Compiled(updateRole _)
      }
    }

    def getRoles = {
      import Database.dynamicSession

      val roles = oq.roles

      val result = db.withDynSession {
        roles.list
      }

      result map {
        case (name, parent, createdAt, createdBy, publiq) => Role(name, parent, createdAt, createdBy, publiq)
      }
    }

    def getRole(name: String) = {
      import Database.dynamicSession

      val role = oq.named(name)

      val result = db.withDynSession {
        role.firstOption
      }

      result map {
        case (name, parent, createdAt, createdBy, publiq) => Role(name, parent, createdAt, createdBy, publiq)
      }
    }

    def getUserRoles(userId: String) = {
      import Database.dynamicSession

      val id = uuid(userId)
      val userRoles = oq.userRoles(id)

      val result = db.withDynSession {
        userRoles.list
      }

      result map {
        case (sUserId, role, grantedAt, grantedBy) =>
          UserRole(sUserId getOrElse id, role, grantedAt getOrElse 0L, grantedBy, delegated = sUserId eq None)
      }
    }

    def getPermissions = {
      import Database.dynamicSession

      val permissions = oq.permissions

      val result = db.withDynSession {
        permissions.list
      }

      result map {
        case (name, clientId) => Permission(name, clientId)
      }
    }

    def getRolePermissions(role: String) = {
      import Database.dynamicSession

      val rolePermissions = oq.rolePermissions(role)

      val result = db.withDynSession {
        rolePermissions.list
      }

      result map {
        case (sRole, permission, grantedAt, grantedBy) => RolePermission(sRole, permission, grantedAt, grantedBy)
      }
    }

    def getUserPermissions(userId: String) = {
      import Database.dynamicSession

      val userPermissions = oq.userPermissions(uuid(userId))

      Set(
        db.withDynSession {
          userPermissions.list
        }: _*)
    }

    def getClientPermissions(clientId: String) = {
      import Database.dynamicSession

      val clientPermissions = oq.clientPermissions(clientId)

      val result = db.withDynSession {
        clientPermissions.list
      }

      result map {
        case (name, sClientId) => Permission(name, sClientId)
      }
    }

    def saveRole(name: String, parent: Option[String], createdBy: Option[String]) = db.withTransaction { implicit session =>
      Roles += Role(name, parent, System.currentTimeMillis, createdBy = createdBy map uuid)

      val role = oq.named(name)

      role.firstOption map {
        case (sName, sParent, createdAt, sCreatedBy, publiq) => Role(sName, sParent, createdAt, sCreatedBy, publiq)
      }
    }

    def grantRolePermissions(role: String, permissions: Set[String], grantedBy: Option[String]) = db.withTransaction { implicit session =>
      RolesPermissions ++= (permissions map (RolePermission(role, _, grantedBy = grantedBy map uuid)))
    }

    def grantUserRoles(userId: String, roles: Set[String], grantedBy: Option[String]) = db.withTransaction { implicit session =>
      UsersRoles ++= (roles map (UserRole(uuid(userId), _, grantedBy = grantedBy map uuid)))
    }

    def revokeUserRole(userId: String, roles: Set[String]) = db.withTransaction { implicit session =>
      val q = for { arg <- UsersRoles if (arg.userId is uuid(userId)) && (arg.role inSet roles) } yield arg
      q.delete
    }

    def revokeRolePermission(role: String, permissions: Set[String]) = db.withTransaction { implicit session =>
      val q = for { arg <- RolesPermissions if (arg.role is role) && (arg.permission inSet permissions) } yield arg
      q.delete
    }

    def purgeRoles(roles: Set[String]) = db.withTransaction { implicit session =>
      val q = for { r <- Roles if (r.name isNot R.SuperUserR.name) && r.publiq && (r.name inSet roles) } yield r
      q.delete
    }

    def userHasRole(userId: String, role: String) =
      if (U.SuperUser.id exists (_.toString == userId)) true
      else {
        import Database.dynamicSession

        val id = uuid(userId)

        @inline
        def getParent(role: String) =
          oq.parent(role).firstFlatten

        @scala.annotation.tailrec
        def userHasRoleAcc(role: String): Boolean = {

          val userRoleExists = oq.userRoleExists(id, role)

          (userRoleExists.firstOption getOrElse false) || (getParent(role) match {
            case Some(parent) => userHasRoleAcc(parent)
            case _            => false
          })
        }

        db.withDynSession {
          userHasRoleAcc(role)
        }
      }

    def roleHasPermission(name: String, permissions: Set[String]) =
      if (name.compareToIgnoreCase(R.SuperUserR.name) == 0) true
      else db.withSession { implicit session =>
        val q = for {
          r <- RolesPermissions if (r.role is name) && (r.permission inSet permissions)
        } yield true

        Query(q.exists).firstOption getOrElse false
      }

    def roleExists(name: String) =
      if (R.all exists (_.name.compareToIgnoreCase(name) == 0)) true
      else {
        import Database.dynamicSession

        val role = oq.roleExists(name.toLowerCase)

        db.withDynSession {
          role.firstOption
        } getOrElse false
      }

    def updateRole(name: String, newName: String, parent: Option[String]) =
      if (name.compareToIgnoreCase(R.SuperUserR.name) == 0) false
      else db.withTransaction { implicit sesssion =>
        oq.forUpdate(name).update(newName, parent) == 1
      }
  }
}