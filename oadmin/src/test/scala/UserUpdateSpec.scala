package schola
package oadmin
package test

object UserUpdateSpec extends org.specs.Specification {
  val userId = SuperUser.id.get

  def initialize() = façade.init(userId)

  def drop() = façade.drop()

  val updateFn = façade.oauthService.updateUser _

  "updating a user" should {

    "update nothing" in {

      updateFn(userId.toString, new utils.DefaultUserSpec) must not be empty

      val o = façade.oauthService.getUser(userId.toString)

      o must not be empty
      o.get.email must be equalTo SuperUser.email
      o.get.password must be equalTo SuperUser.password
      o.get.firstname must be equalTo SuperUser.firstname
      o.get.lastname must be equalTo SuperUser.lastname
      o.get.gender must be equalTo SuperUser.gender
      o.get.homeAddress must be equalTo SuperUser.homeAddress
      o.get.workAddress must be equalTo SuperUser.workAddress
      o.get.contacts must be equalTo SuperUser.contacts
    }

    "update email" in {

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
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
        ),
        passwordValid = true
      )

      o must not be empty
      o.get.email must be equalTo "username0"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.firstname must be equalTo "Amadou"
      o.get.lastname must be equalTo "Cisse"
      o.get.gender must be equalTo domain.Gender.Female
      o.get.homeAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella"))
      o.get.workAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
      o.get.contacts must be equalTo Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
      )

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val email = Some("username1")
      }) must not be empty

      val h = façade.oauthService.getUser(o.get.id.get.toString)

      h must not be empty
      h.get.email must be equalTo "username1"

      façade.oauthService.removeUser(o.get.id.get.toString) must beTrue
    }

    "update password only of it matches" in {
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
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
        ),
        passwordValid = true
      )

      o must not be empty
      o.get.email must be equalTo "username0"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.firstname must be equalTo "Amadou"
      o.get.lastname must be equalTo "Cisse"
      o.get.gender must be equalTo domain.Gender.Female
      o.get.homeAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella"))
      o.get.workAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
      o.get.contacts must be equalTo Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
      )

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val password = Some("amsayk.9")
//        override val passwordConfirm = Some("username1")
      }) must beEmpty

      val hh = façade.oauthService.getUser(o.get.id.get.toString)
      hh must not be empty
      passwords.verify("amsayk.0", hh.get.password.get) must beTrue

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val password = Some("amsayk.9")
        override val oldPassword = Some("username1dd") // wrong password confirmation
      }) must beEmpty

      val hhh = façade.oauthService.getUser(o.get.id.get.toString)
      hhh must not be empty
      passwords.verify("amsayk.0", hhh.get.password.get) must beTrue

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val password = Some("amsayk.9")
        override val oldPassword = Some("amsayk.0")
      }) must not be empty

      val gg = façade.oauthService.getUser(o.get.id.get.toString)
      gg must not be empty
      passwords.verify("amsayk.9", gg.get.password.get) must beTrue

      façade.oauthService.removeUser(o.get.id.get.toString) must beTrue
    }

    "update contacts" in {
      val sContacts = Set[domain.ContactInfo](
          domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
        )

      val o = façade.oauthService.saveUser(
        "username2",
        "amsayk.0",
        "Amadou",
        "Cisse",
        Some(userId.toString),
        domain.Gender.Female,
        Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
        Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")),
        sContacts,
        passwordValid = true
      )

      o must not be empty
      o.get.email must be equalTo "username2"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.firstname must be equalTo "Amadou"
      o.get.lastname must be equalTo "Cisse"
      o.get.gender must be equalTo domain.Gender.Female
      o.get.homeAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella"))
      o.get.workAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
      o.get.contacts must be equalTo sContacts

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val contacts = Some(ContactInfoSpec(toAdd = Set(domain.MobileContactInfo(domain.PhoneNumber("+212600793159")))))
      }) must not be empty

      val h = façade.oauthService.getUser(o.get.id.get.toString)

      h must not be empty
      h.get.contacts must haveSize(3)
      h.get.contacts must be equalTo (sContacts + domain.MobileContactInfo(domain.PhoneNumber("+212600793159")))

      //-----------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val contacts = Some(ContactInfoSpec(toRem = sContacts))
      }) must not be empty

      val hh = façade.oauthService.getUser(o.get.id.get.toString)

      hh must not be empty
      hh.get.contacts must haveSize(1)
      hh.get.contacts must be equalTo Set(domain.MobileContactInfo(domain.PhoneNumber("+212600793159")))

      //------------------------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val contacts = Some(ContactInfoSpec(toAdd = sContacts))
      }) must not be empty

      val hhh = façade.oauthService.getUser(o.get.id.get.toString)

      hhh must not be empty
      hhh.get.contacts must haveSize(3)
      hhh.get.contacts must be equalTo (sContacts + domain.MobileContactInfo(domain.PhoneNumber("+212600793159")))

      //-----------------------------------------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val contacts = Some(ContactInfoSpec(toRem = Set(), toAdd = Set()))
      }) must not be empty

      val g = façade.oauthService.getUser(o.get.id.get.toString)

      g must not be empty
      g.get.contacts must haveSize(3)
      g.get.contacts must be equalTo (sContacts + domain.MobileContactInfo(domain.PhoneNumber("+212600793159")))

      //-----------------------------------------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val contacts = None
      }) must not be empty

      val gg = façade.oauthService.getUser(o.get.id.get.toString)

      gg must not be empty
      gg.get.contacts must haveSize(3)
      gg.get.contacts must be equalTo (sContacts + domain.MobileContactInfo(domain.PhoneNumber("+212600793159")))

      //-----------------------------------------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val contacts = Some(ContactInfoSpec(toRem = sContacts + domain.MobileContactInfo(domain.PhoneNumber("+212600793159"))))
      }) must not be empty

      val ggg = façade.oauthService.getUser(o.get.id.get.toString)

      ggg must not be empty
      ggg.get.contacts must haveSize(0)
      ggg.get.contacts must be equalTo Set()

      façade.oauthService.removeUser(o.get.id.get.toString) must beTrue
    }

    "update workAddress" in {}

    "update gender" in {}

    "update firstname" in {}

    "update lastname" in {}

    "remove homeAddress" in {

      val o = façade.oauthService.saveUser(
        "username10",
        "amsayk.0",
        "Amadou",
        "Cisse",
        Some(userId.toString),
        domain.Gender.Female,
        Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
        Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")),
        Set[domain.ContactInfo](
          domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
        ),
        passwordValid = true
      )

      o must not be empty
      o.get.email must be equalTo "username10"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.firstname must be equalTo "Amadou"
      o.get.lastname must be equalTo "Cisse"
      o.get.gender must be equalTo domain.Gender.Female
      o.get.homeAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella"))
      o.get.workAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
      o.get.contacts must be equalTo Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
      )

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val homeAddress = UpdateSpecImpl[domain.AddressInfo](set = Some(None))
      }) must not be empty

      val h = façade.oauthService.getUser(o.get.id.get.toString)

      h must not be empty
      h.get.homeAddress must beEmpty

      //---------------------------------------------------------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val homeAddress = UpdateSpecImpl(set = Some(Some(domain.AddressInfo("CASABLANCA", "Morocco", "500000", "5, Appt. 27, Rue Jabal Tazaka"))))
      }) must not be empty

      val hh = façade.oauthService.getUser(o.get.id.get.toString)

      hh must not be empty
      hh.get.homeAddress must not be empty
      hh.get.homeAddress.get must be equalTo domain.AddressInfo("CASABLANCA", "Morocco", "500000", "5, Appt. 27, Rue Jabal Tazaka")

      //---------------------------------------------------------------------------------------------

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        // override val homeAddress = super.homeAddress copy(set = Some(Some(domain.AddressInfo("CASABLANCA", "Morocco", "500000", "5, Appt. 27, Rue Jabal Tazaka"))))
      }) must not be empty

      val hhh = façade.oauthService.getUser(o.get.id.get.toString)

      hhh must not be empty
      hhh.get.homeAddress must not be empty
      hhh.get.homeAddress.get must be equalTo domain.AddressInfo("CASABLANCA", "Morocco", "500000", "5, Appt. 27, Rue Jabal Tazaka")      

      façade.oauthService.removeUser(o.get.id.get.toString) must beTrue
    }

    "update email, firstname, lastname, gender, homeAddress, workAddress and contacts" in {
      val sContacts = Set[domain.ContactInfo](
          domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
        )

      val o = façade.oauthService.saveUser(
        "username11",
        "amsayk.0",
        "Amadou",
        "Cisse",
        Some(userId.toString),
        domain.Gender.Female,
        Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella")),
        Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka")),
        Set[domain.ContactInfo](
          domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
          domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
        ),
        passwordValid = true
      )

      o must not be empty
      o.get.email must be equalTo "username11"
      passwords.verify("amsayk.0", o.get.password.get) must beTrue
      o.get.firstname must be equalTo "Amadou"
      o.get.lastname must be equalTo "Cisse"
      o.get.gender must be equalTo domain.Gender.Female
      o.get.homeAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10032", "Imm. B, Appt. 23, Cite Mabella, Mabella"))
      o.get.workAddress must be equalTo Some(domain.AddressInfo("RABAT", "Morocco", "10000", "5, Appt. 23, Rue Jabal Tazaka"))
      o.get.contacts must be equalTo Set[domain.ContactInfo](
        domain.HomeContactInfo(domain.PhoneNumber("+212600793159")),
        domain.WorkContactInfo(domain.Email("ousmancisse64@gmail.com"))
      )

      updateFn(o.get.id.get.toString, new utils.DefaultUserSpec {
        override val homeAddress = UpdateSpecImpl[domain.AddressInfo](set = Some(None))
        override val workAddress = UpdateSpecImpl[domain.AddressInfo](set = Some(None))
        override val contacts = Some(ContactInfoSpec(toAdd = sContacts))
        override val gender = Some(domain.Gender.Male)
      }) must not be empty

      val h = façade.oauthService.getUser(o.get.id.get.toString)

      h must not be empty
      h.get.homeAddress must beEmpty
      h.get.workAddress must beEmpty
      h.get.contacts must be equalTo sContacts
      h.get.gender must be equalTo domain.Gender.Male    

      façade.oauthService.removeUser(o.get.id.get.toString) must beTrue
    }
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize () must beTrue }
  doAfterSpec { drop () }
}
