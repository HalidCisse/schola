package schola
package oadmin

package object façade extends impl.AccessControlServicesComponentImpl
with impl.AccessControlServicesRepoComponentImpl
with impl.OAuthServicesComponentImpl
with impl.OAuthServicesRepoComponentImpl {

  import schema._
  import domain._
  import Q._

  private[oadmin] def withTransaction[T](f: Q.Session => T) = db.withTransaction { f }
  private[oadmin] def withSession[T](f: Q.Session => T) = db.withSession { f }

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
      val _2 = (Users += SuperUser) == 1 //List(
        SuperUser // User(userId, "amadou", "amsayk", "amadou", "cisse", createdBy = None),
        //      User(adminId, "admin", "admin", "super", "user", createdBy = None)

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

  def test = {
    val o = accessControlService

    val userId = SuperUser.id

    def initialize() = init(userId)

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

  // -------------------------------------------------------

  // -------------------------------------------------------------------------------------------------

  def addUser(): Option[User] = ???

  def updateUser(userId: String): Option[User] = ???

  def removeUser(userId: String): Option[User] = ???

  def getUser(userId: String): Option[User] = ???

  def getUsers: List[User] = ???

  def getPurgedUsers: List[User] = ???

  def purgeUsers(ids: Set[String]): Boolean = ???

  def changePasswd(userId: String): Boolean = ???

  // ---------------------------------------------------------------------------------------------------

  def addRole(role: String): Option[Role] = ???

  def updateRole(role: String): Option[Role] = ???

  def removeRole(role: String): Option[Role] = ???

  def getRole(role: String): Option[Role] = ???

  def getRoles: List[Role] = ???

  def getPurgedRoles: List[Role] = ???

  def purgeRoles(roles: Set[String]): Boolean = ???

  def usernameExists(username: String): Boolean = ???

  // ---------------------------------------------------------------------------------------------------

  def getPermissions: List[Permission] = ???

  def getRolePermissions(role: String): List[Permission] = ???

  def getUserRoles(userId: String): List[UserRole] = ???

  def logout: Boolean = ???

  //-------------------------------------------------------------

//  java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider)
}