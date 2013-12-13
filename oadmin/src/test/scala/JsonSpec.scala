package schola
package oadmin
package test

object JsonSpec extends org.specs.Specification {

  import conversions.json._

  val userId = SuperUser.id.get

  def initialize() = façade.init(userId)

  def drop() = façade.drop()

  "domain objects" should {

    "convert user to json" in {

      val o = façade.oauthService.saveUser(
        "username0",
        "amsayk.0",
        "Amadou",
        "Cisse",
        Some(userId.toString),
        domain.Gender.Female,
        Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
        Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")),
        Set[domain.ContactInfo](
          domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com")),
          domain.MobileContactInfo(domain.PhoneNumber("+212600793159"))
        ),
        passwordValid = true
      )

      o must not be empty
      println(tojson(o.get))
    }

    "convert to json" in {

      val o = façade.accessControlService

      println(tojson (o.getRoles))
      println(tojson (o.getUserRoles(userId.toString)))
      println(tojson (o.getPermissions))
      println(tojson (o.getRolePermissions("Role One")))
      println(tojson (o.getClientPermissions("oadmin")))
      println(tojson (o.getUserPermissions(userId.toString)))
      println(o.saveRole("Role XI", None, None) map(x => tojson(x.asInstanceOf[domain.Role])))
    }

    "convert permission to json" in {}

    "convert user_role to json" in {}

    "convert role_permission to json" in {}
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize () must beTrue }
  doAfterSpec { drop () }
}
