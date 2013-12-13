package schola
package oadmin
package test

import schema._
import Q._

object ConversionSpecification extends org.specs.Specification {

  val userId = SuperUser.id.get

  def initialize() = façade.init(userId)

  def drop() = façade.drop()

  "support for conversions" should {

    "be able to persist scopes" in {

      façade.withTransaction {
        implicit session =>
          OAuthTokens += domain.OAuthToken(
            "access_token.0", "oadmin", "http://localhost/oadmin",userId, None, "djdjdj", "Chrome", None, None, scopes = Set("oadmin", "schola", "orphans")
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
            "access_token.1", None, "djdjdj", "Chrome", "oadmin", "http://localhost/oadmin", userId.toString, None, None, scopes = Set("oadmin", "schola", "orphans")
          )
      }

      x must not be empty
      x.get.accessToken must be equalTo "access_token.1"
      x.get.scopes must be equalTo Set("oadmin", "schola", "orphans")

      façade.withTransaction {
        implicit session =>
          OAuthTokens += domain.OAuthToken(
            "access_token.3", "oadmin", "http://localhost/oadmin", userId, None, "djdjdj", "Chrome", None, None, scopes = Set()
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
      val uId = java.util.UUID.randomUUID

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId),
            "amsayk.0",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id,
            homeAddress = Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
            workAddress = Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.email must be equalTo "amsayk.0"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue

      o.get.homeAddress must not be empty
      o.get.homeAddress.get must be equalTo domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")

      o.get.workAddress must not be empty
      o.get.workAddress.get must be equalTo domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")
    }

    "be able to persist contacts" in {
      val uId = java.util.UUID.randomUUID

      // Empty contacts {
      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId),
            "amsayk.11",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id,
            contacts = Set()
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.email must be equalTo "amsayk.11"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must be equalTo Set()
    }

    // Emails
    "with only emails" in {

      val contacts = Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.Email("cisse.amadou.9@gmail.com")),
        domain.WorkContactInfo(domain.Email("amsayk@facebook.com")),
        domain.WorkContactInfo(domain.Email("amadou.cisse@epsilon.ma"))
      )

      val uId = java.util.UUID.randomUUID

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId),
            "amsayk.12",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id,
            contacts = contacts
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.email must be equalTo "amsayk.12"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must haveSize(3)

      o.get.contacts must be equalTo contacts
    }

    // Telephone
    "with only telephones" in {
      val contacts = Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
        domain.MobileContactInfo(domain.PhoneNumber("+212600793152"))
      )

      val uId = java.util.UUID.randomUUID

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId),
            "amsayk.13",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id,
            contacts = contacts
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.email must be equalTo "amsayk.13"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must haveSize(2)

      o.get.contacts must be equalTo contacts
    }

    "be able to persist gender" in {
      val uId = java.util.UUID.randomUUID

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId),
            "amsayk.10",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.email must be equalTo "amsayk.10"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.gender mustBe domain.Gender.Male

      val uId2 = java.util.UUID.randomUUID

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId2),
            "amsayk.40",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            gender = domain.Gender.Female,
            createdBy = SuperUser.id
          )
      } must be equalTo 1

      val d = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId2.toString)
      }

      d must not be empty
      d.get.email must be equalTo "amsayk.40"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      d.get.gender mustBe domain.Gender.Female
    }

    "be able to persist contacts and handle inheritance right" in {
      val contacts = Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
      )

      val uId = java.util.UUID.randomUUID

      façade.withTransaction {
        implicit session =>
          Users += domain.User(
            Some(uId),
            "amsayk.18",
            Some(passwords crypt "amsayk.0"),
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id,
            contacts = contacts
          )
      } must be equalTo 1

      val o = façade.withTransaction {
        implicit session =>
          façade.oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.email must be equalTo "amsayk.18"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.gender mustBe domain.Gender.Male
      o.get.contacts must haveSize(2)

      o.get.contacts must be equalTo contacts
    }
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize () must beTrue }
  doAfterSpec { drop () }
}
