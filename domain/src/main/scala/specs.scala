package schola
package oadmin
package domain

trait UpdateSpec[T] {
  def set: Option[Option[T]]

  def foreach(f: Option[T] => Boolean) = set map f getOrElse true
}

trait SetSpec[T] {

  def toRem: Set[T]

  def toAdd: Set[T]

  final def diff(os: Set[T]): Set[T] = os ++ toAdd -- toRem
}

trait UserSpec {

  case class UpdateSpecImpl[T: scala.reflect.ClassTag](set: Option[Option[T]] = None) extends UpdateSpec[T]

  case class MobileNumbersSpec(
    mobile1: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    mobile2: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactInfoSpec[T](
    email: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    fax: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    phoneNumber: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactsSpec(
    mobiles: MobileNumbersSpec = MobileNumbersSpec(),
    home: Option[ContactInfoSpec[ContactInfo]] = None,
    work: Option[ContactInfoSpec[ContactInfo]] = None)

  def contacts: Option[ContactsSpec]

  def homeAddress: UpdateSpec[AddressInfo]

  def workAddress: UpdateSpec[AddressInfo]

  def primaryEmail: Option[String]

  def password: Option[String] // Though this is an Option, its required!

  def oldPassword: Option[String]

  def givenName: Option[String]

  def familyName: Option[String]

  def gender: Option[Gender.Value]

  def avatar: UpdateSpec[String]
}

class DefaultUserSpec extends UserSpec {

  lazy val contacts: Option[ContactsSpec] = None

  lazy val homeAddress = UpdateSpecImpl[AddressInfo]()

  lazy val workAddress = UpdateSpecImpl[AddressInfo]()

  lazy val primaryEmail: Option[String] = None

  lazy val password: Option[String] = None

  lazy val oldPassword: Option[String] = None

  lazy val givenName: Option[String] = None

  lazy val familyName: Option[String] = None

  lazy val gender: Option[Gender.Value] = None

  lazy val avatar: UpdateSpec[String] = UpdateSpecImpl[String]()
}