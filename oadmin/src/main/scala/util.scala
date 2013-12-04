package schola
package oadmin

import schola.oadmin.domain.{ContactInfo, AddressInfo, Gender}

package object util {

  def genPasswd(key: String) = passwords.crypt(key)

  def randomString(size: Int) = {
    val random = java.security.SecureRandom.getInstance("SHA1PRNG")
    val b = new Array[Byte](size)
    random.nextBytes(b)
    org.bouncycastle.util.encoders.Hex.toHexString(b)
  }

  trait UpdateSpec[T] {
    def set: Option[Option[T]]

    def remove: Boolean

    def foreach(f: Option[T] => Boolean) = if (remove) f(None) else if (set.nonEmpty) f(set.get) else true
  }

  trait AddSpec[T] {
    def addition: Set[T]
  }

  trait SubtractSpec[T] {
    def subtraction: Set[T]
  }

  trait Diff[T] {
    def diff(obj: Set[T]): Set[T]
  }

  trait UserSpec {

    trait ContactInfoDiff extends Diff[domain.ContactInfo] {
      self: AddSpec[domain.ContactInfo] with SubtractSpec[domain.ContactInfo] =>
      def diff(contacts: Set[domain.ContactInfo]) = contacts -- subtraction ++ addition
    }

    def contacts: AddSpec[domain.ContactInfo] with SubtractSpec[domain.ContactInfo] with ContactInfoDiff

    def homeAddress: UpdateSpec[domain.AddressInfo]

    def workAddress: UpdateSpec[domain.AddressInfo]

    def username: Option[String]

    def password: Option[String]

    def passwordConfirm: Option[String]

    def firstname: Option[String]

    def lastname: Option[String]

    def gender: Option[domain.Gender.Value]
  }

  class DefaultUserSpec extends UserSpec {

    case class UpdateSpecImpl(set: Option[Option[AddressInfo]] = None, remove: Boolean = false) extends UpdateSpec[AddressInfo]

    case class ContactsSpecImpl(
         addition: Set[ContactInfo] = Set(),
         subtraction: Set[ContactInfo] = Set()
    ) extends ContactInfoDiff with AddSpec[ContactInfo] with SubtractSpec[ContactInfo]

    val contacts = ContactsSpecImpl()

    val homeAddress = UpdateSpecImpl()

    val workAddress = UpdateSpecImpl()

    val username: Option[String] = None

    val password: Option[String] = None

    val passwordConfirm: Option[String] = None

    val firstname: Option[String] = None

    val lastname: Option[String] = None

    val gender: Option[domain.Gender.Value] = None
  }

  def updateUser(id: String, spec: UserSpec) = {
    import Q._

    val q = schema.Users.filter(_.id is java.util.UUID.fromString(id))

    façade.withSession {
      implicit s => (for {u <- q} yield (u.password, u.contacts)).firstOption
    } match {

      case Some((sPassword, sContacts)) => façade.withTransaction {
        implicit session =>

          val _1 = spec.username map {
            username =>
              (q map(_.username) update(username)) == 1
          } getOrElse true

          val _2 = spec.password map {
            password =>
              spec.passwordConfirm.nonEmpty && (passwords verify(spec.passwordConfirm.get, sPassword)) && ((q map(_.password) update(passwords crypt(password))) == 1)
          } getOrElse true

          val _3 = spec.firstname map {
            firstname =>
              (q map(_.firstname) update(firstname)) == 1
          } getOrElse true

          val _4 = spec.lastname map {
            lastname =>
              (q map(_.lastname) update(lastname)) == 1
          } getOrElse true

          val _5 = spec.gender map {
            gender =>
              (q map(_.gender) update(gender)) == 1
          } getOrElse true

          val _6 = spec.homeAddress foreach {
            case homeAddress =>
              (q map(_.homeAddress) update(homeAddress)) == 1
          }

          val _7 = spec.workAddress foreach {
            case workAddress =>
              (q map(_.workAddress) update(workAddress)) == 1
          }

          val _8 = (q map(_.contacts) update(spec.contacts.diff(sContacts))) == 1

          _1 && _2 && _3 && _4 && _5 && _6 && _7 && _8
      }

      case _ => false
    }
  }
}