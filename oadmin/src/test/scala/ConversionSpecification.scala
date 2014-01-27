package schola
package oadmin
package test

import schema._
import Q._

object ConversionSpecification extends org.specs.Specification {
  import S._

  val userId = SuperUser.id.get

  def initialize() = init(userId)

  "support for conversions" should {

    "be able to persist scopes" in {

      withTransaction {
        implicit session =>
          OAuthTokens += domain.OAuthToken(
            "access_token.0", "oadmin", "http://localhost/oadmin", userId, None, "djdjdj", "Chrome", None, None, scopes = Set("oadmin", "schola", "orphans"))
      } must be equalTo 1

      val o = withTransaction {
        implicit session =>
          oauthService.getUserSession(Map("bearerToken" -> "access_token.0", "userAgent" -> "Chrome"))
      }

      o must not be empty
      o.get.key must be equalTo "access_token.0"
      o.get.scopes must be equalTo Set("oadmin", "schola", "orphans")

      val x = withTransaction {
        implicit session =>
          oauthService.saveToken(
            "access_token.1", None, "djdjdj", "Chrome", "oadmin", "http://localhost/oadmin", userId.toString, None, None, scopes = Set("oadmin", "schola", "orphans"))
      }

      x must not be empty
      x.get.accessToken must be equalTo "access_token.1"
      x.get.scopes must be equalTo Set("oadmin", "schola", "orphans")

      withTransaction {
        implicit session =>
          OAuthTokens += domain.OAuthToken(
            "access_token.3", "oadmin", "http://localhost/oadmin", userId, None, "djdjdj", "Chrome", None, None, scopes = Set())
      } must be equalTo 1

      val g = withTransaction {
        implicit session =>
          oauthService.getUserSession(Map("bearerToken" -> "access_token.3", "userAgent" -> "Chrome"))
      }

      g must not be empty
      g.get.key must be equalTo "access_token.3"
      g.get.scopes must be equalTo Set()
    }

    "be able to persist home and work addresses" in {

      val u = withTransaction {
        implicit session =>
          Users insert (
            "amsayk.0",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id,
            homeAddress = Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
            workAddress = Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")))
      }

      u must not be null
      val uId = u.id.get

      val o = withTransaction {
        implicit session =>
          oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.primaryEmail must be equalTo "amsayk.0"
      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue

      o.get.homeAddress must not be empty
      o.get.homeAddress.get must be equalTo domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")

      o.get.workAddress must not be empty
      o.get.workAddress.get must be equalTo domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")
    }

    "be able to persist contacts" in {

      // Empty contacts {
      val u = withTransaction {
        implicit session =>
          Users insert (
            "amsayk.11",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id /*,
            contacts = None*/ )
      }

      u must not be null
      val uId = u.id.get

      val o = withTransaction {
        implicit session =>
          oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.primaryEmail must be equalTo "amsayk.11"
      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue
      o.get.gender mustBe domain.Gender.Male
      /*o.get.contacts must be equalTo None*/
    }

    // Emails
    "with only emails" in {

      //      val contacts = Set[domain.ContactInfo](
      //        domain.HomeContactInfo(domain.Email("cisse.amadou.9@gmail.com")),
      //        domain.WorkContactInfo(domain.Email("amsayk@facebook.com")),
      //        domain.WorkContactInfo(domain.Email("amadou.cisse@epsilon.ma"))
      //      )
      //
      //      val u = withTransaction {
      //        implicit session =>
      //          Users insert (
      //            "amsayk.12",
      //            passwords crypt "amsayk.0",
      //            "Amadou",
      //            "Cisse",
      //            createdBy = SuperUser.id,
      //            contacts = contacts
      //          )
      //      }
      //
      //      u must not be null
      //      val uId = u.id.get
      //
      //      val o = withTransaction {
      //        implicit session =>
      //          oauthService.getUser(uId.toString)
      //      }
      //
      //      o must not be empty
      //      o.get.primaryEmail must be equalTo "amsayk.12"
      //      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue
      //      o.get.gender mustBe domain.Gender.Male
      //      o.get.contacts must haveSize(3)
      //
      //      o.get.contacts must be equalTo contacts
    }

    // Telephone
    "with only telephones" in {
      //      val contacts = Set[domain.ContactInfo](
      //        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
      //        domain.MobileContactInfo(domain.PhoneNumber("+212600793152"))
      //      )
      //
      //      val u = withTransaction {
      //        implicit session =>
      //          Users insert (
      //            "amsayk.13",
      //            passwords crypt "amsayk.0",
      //            "Amadou",
      //            "Cisse",
      //            createdBy = SuperUser.id,
      //            contacts = contacts
      //          )
      //      }
      //
      //      u must not be null
      //      val uId = u.id.get
      //
      //      val o = withTransaction {
      //        implicit session =>
      //          oauthService.getUser(uId.toString)
      //      }
      //
      //      o must not be empty
      //      o.get.primaryEmail must be equalTo "amsayk.13"
      //      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue
      //      o.get.gender mustBe domain.Gender.Male
      //      o.get.contacts must haveSize(2)
      //
      //      o.get.contacts must be equalTo contacts
    }

    "be able to persist gender" in {

      val u = withTransaction {
        implicit session =>
          Users insert (
            "amsayk.10",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            createdBy = SuperUser.id)
      }

      u must not be null
      val uId = u.id.get

      val o = withTransaction {
        implicit session =>
          oauthService.getUser(uId.toString)
      }

      o must not be empty
      o.get.primaryEmail must be equalTo "amsayk.10"
      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue
      o.get.gender mustBe domain.Gender.Male

      val u2 = withTransaction {
        implicit session =>
          Users insert (
            "amsayk.40",
            passwords crypt "amsayk.0",
            "Amadou",
            "Cisse",
            gender = domain.Gender.Female,
            createdBy = SuperUser.id)
      }

      u2 must not be null
      val uId2 = u2.id.get

      val d = withTransaction {
        implicit session =>
          oauthService.getUser(uId2.toString)
      }

      d must not be empty
      d.get.primaryEmail must be equalTo "amsayk.40"
      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue
      d.get.gender mustBe domain.Gender.Female
    }

    "be able to persist contacts and handle inheritance right" in {
      //      val contacts = Set[domain.ContactInfo](
      //        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
      //        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
      //      )
      //
      //      val u = withTransaction {
      //        implicit session =>
      //          Users insert (
      //            "amsayk.18",
      //            passwords crypt "amsayk.0",
      //            "Amadou",
      //            "Cisse",
      //            createdBy = SuperUser.id,
      //            contacts = contacts
      //          )
      //      }
      //
      //      u must not be null
      //      val uId = u.id.get
      //
      //      val o = withTransaction {
      //        implicit session =>
      //          oauthService.getUser(uId.toString)
      //      }
      //
      //      o must not be empty
      //      o.get.primaryEmail must be equalTo "amsayk.18"
      //      passwords.verify("amsayk.0", passwords crypt "amsayk.0") must beTrue
      //      o.get.gender mustBe domain.Gender.Male
      //      o.get.contacts must haveSize(2)
      //
      //      o.get.contacts must be equalTo contacts
    }
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize() must beTrue }
  doAfterSpec { drop() }
}
