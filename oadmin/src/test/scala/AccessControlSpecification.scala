package schola
package oadmin
package test

object AccessControlSpecification extends org.specs.Specification{

  val userId = SuperUser.id.get

  def initialize() = Façade.init(userId)

  def drop() = Façade.drop()

  "access control services" should {

    "get all users in db" in { Façade.oauthService.getUsers.length must be equalTo 1 }

    "get all roles in db" in { Façade.accessControlService.getRoles.length must be equalTo 7 }
    "get all user roles in db" in { Façade.accessControlService.getUserRoles(userId.toString).length must be equalTo 3 }

    "get all permissions in db" in { Façade.accessControlService.getPermissions.length must be equalTo 10 }
    "get all role permissions in db" in { Façade.accessControlService.getRolePermissions("Role One").length must be equalTo 5 }
    "get all client permissions in db" in { Façade.accessControlService.getClientPermissions("oadmin").length must be equalTo 4 }

    "save role" in { Façade.accessControlService.saveRole("Role XI", None, None) must not be empty }
    "grant role" in { Façade.accessControlService.grantUserRoles(userId.toString, Set("Role Four"), None) must not throwA(new Exception) }
    "grant permission" in { Façade.accessControlService.grantRolePermissions("Role X", Set("P7", "P8"), None) must not throwA(new Exception) }

    "test user role" in { Façade.accessControlService.userHasRole(userId.toString, "Role One") must beTrue }
    "test user role - case of parent delegation" in { Façade.accessControlService.userHasRole(userId.toString, "Role X") must beTrue }
    "test role permission" in { Façade.accessControlService.roleHasPermission("Role One", Set("P1")) must beTrue }

    "not be able to delete a non-public role" in {}

    "not be able to delete a permission" in { /*No API*/ }
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize() must beTrue }
  doAfterSpec { drop() }
}
