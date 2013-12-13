package schola
package oadmin
package test

object AvatarSpec extends org.specs.Specification {

  val userId = SuperUser.id.get

  def initialize() = façade.init(userId)

  def drop() = façade.drop()

  "avatar services" should {

    "save, modify and purge avatar" in {
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
      o.get.avatar must beEmpty

      façade.oauthService.updateUser(o.get.id.get.toString, new utils.DefaultUserSpec{
        override val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(Some(domain.AvatarInfo("image/png"), "file contents".getBytes("utf-8"))))
      }) must not be empty

      Thread.sleep(2 * 1000)

      val x = façade.oauthService.getAvatar(o.get.id.get.toString)

      x must not be empty
      x.get._1.contentType must be equalTo "image/png"
      new String(x.get._2, "utf-8") must be equalTo "file contents"

      façade.oauthService.updateUser(o.get.id.get.toString, new utils.DefaultUserSpec{
        override val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])](set = Some(None))
      }) must not be empty

      Thread.sleep(2 * 1000)

      façade.oauthService.getAvatar(o.get.id.get.toString) must beEmpty

      façade.oauthService.removeUser(o.get.id.get.toString) must beTrue
      façade.oauthService.purgeUsers(Set(o.get.id.get.toString))
    }
  }

  doBeforeSpec { try drop() catch { case _: Throwable => }; initialize () must beTrue }
  doAfterSpec { drop () }
}
