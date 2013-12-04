package schola
package oadmin

import schema._
import Q._

object ConversionSpecification extends org.specs.Specification {

  val userId = SuperUser.id

  def initialize() = façade.init(userId)

  def drop() = façade.drop()

  "support for conversions" should {

    "be able to persist scopes" in {

      façade.withTransaction {
        implicit session =>
          OAuthTokens += domain.OAuthToken(
            "access_token.0", "oadmin", "http://localhost/oadmin", SuperUser.id, None, "djdjdj", None, None, scopes = Set("oadmin", "schola", "orphans")
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getToken("access_token.0")
      }

      o must not be empty
      o.get.accessToken must be equalTo "access_token.0"
      o.get.scopes must be equalTo Set("oadmin", "schola", "orphans")

      val x = façade.withTransaction {
        implicit session =>
          façade.oauthService.saveToken(
            "access_token.1", None, "djdjdj", "oadmin", "http://localhost/oadmin", SuperUser.id.toString, None, None, scopes = Set("oadmin", "schola", "orphans")
          )
      }

      x must not be empty
      x.get.accessToken must be equalTo "access_token.1"
      x.get.scopes must be equalTo Set("oadmin", "schola", "orphans")

      façade.withTransaction {
        implicit session =>
          OAuthTokens += domain.OAuthToken(
            "access_token.3", "oadmin", "http://localhost/oadmin", SuperUser.id, None, "djdjdj", None, None, scopes = Set()
          )
      } must be equalTo 1

      val g = façade.withTransaction {
        implicit session =>
          façade.oauthService.getToken("access_token.3")
      }

      g must not be empty
      g.get.accessToken must be equalTo "access_token.3"
      g.get.scopes must be equalTo Set()
    }

    "be able to persist home and work addresses" in {
      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.0",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = Some(SuperUser.id),
            homeAddress = Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
            workAddress = Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.0", "amsayk.0")
      }

      o must not be empty
      o.get.username must be equalTo "amsayk.0"
      passwords.verify("amsayk.0", o.get.password) must beTrue

      o.get.homeAddress must not be empty
      o.get.homeAddress.get must be equalTo domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")

      o.get.workAddress must not be empty
      o.get.workAddress.get must be equalTo domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")
    }

    "be able to persist contacts" in {
      // Empty contacts {
      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.11",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = Some(SuperUser.id),
            contacts = Set()
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.11", "amsayk.0")
      }

      o must not be empty
      o.get.username must be equalTo "amsayk.11"
      passwords.verify("amsayk.0", o.get.password) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must be equalTo Set()
    }

    // Emails
    "with only emails" in {

      val contacts = Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.Email("cisse.amadou.9@gmail.com"), primary = true),
        domain.WorkContactInfo(domain.Email("amsayk@facebook.com")),
        domain.WorkContactInfo(domain.Email("amadou.cisse@epsilon.ma"))
      )

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.12",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = Some(SuperUser.id),
            contacts = contacts
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.12", "amsayk.0")
      }

      o must not be empty
      o.get.username must be equalTo "amsayk.12"
      passwords.verify("amsayk.0", o.get.password) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must haveSize(3)

      o.get.contacts must be equalTo contacts
    }

    // Telephone
    "with only telephones" in {
      val contacts = Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159"), primary = true),
        domain.MobileContactInfo(domain.PhoneNumber("+212600793152"))
      )

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.13",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = Some(SuperUser.id),
            contacts = contacts
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.13", "amsayk.0")
      }

      o must not be empty
      o.get.username must be equalTo "amsayk.13"
      passwords.verify("amsayk.0", o.get.password) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must haveSize(2)

      o.get.contacts must be equalTo contacts
    }

    "be able to persist gender" in {
      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.10",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = Some(SuperUser.id)
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.10", "amsayk.0")
      }

      o must not be empty
      o.get.username must be equalTo "amsayk.10"
      passwords.verify("amsayk.0", o.get.password) must beTrue
      o.get.gender mustBe domain.Gender.Male

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.40",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            gender = domain.Gender.Female,
            createdBy = Some(SuperUser.id)
          )
      } must be equalTo 1

      val d = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.40", "amsayk.0")
      }

      d must not be empty
      d.get.username must be equalTo "amsayk.40"
      passwords.verify("amsayk.0", o.get.password) must beTrue
      d.get.gender mustBe domain.Gender.Female
    }

    "be able to persist contacts and handle inheritance right" in {
      val contacts = Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159"), primary = true),
        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"), primary = true)
      )

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            java.util.UUID.randomUUID,
            "amsayk.18",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = Some(SuperUser.id),
            contacts = contacts
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.authUser("amsayk.18", "amsayk.0")
      }

      o must not be empty
      o.get.username must be equalTo "amsayk.18"
      passwords.verify("amsayk.0", o.get.password) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must haveSize(2)

      o.get.contacts must be equalTo contacts
    }
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize () must beTrue }
  doAfterSpec { drop () }
}
