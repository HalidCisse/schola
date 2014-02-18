package schola
package oadmin
package domain

trait UpdateSpec[T] {
  def set: Option[Option[T]]

  @inline final def foreach(f: Option[T] => Boolean) = set map f getOrElse true

  @inline final def isEmpty = set eq None
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

  case class ContactInfoSpec(
    email: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    fax: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    phoneNumber: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactsSpec(
    mobiles: UpdateSpecImpl[MobileNumbersSpec] = UpdateSpecImpl[MobileNumbersSpec](),
    home: UpdateSpecImpl[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec](),
    work: UpdateSpecImpl[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec]())

  case class AddressInfoSpec(
    city: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    country: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    postalCode: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    streetAddress: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  def contacts: UpdateSpec[ContactsSpec]

  def homeAddress: UpdateSpec[AddressInfoSpec]

  def workAddress: UpdateSpec[AddressInfoSpec]

  def primaryEmail: Option[String]

  def password: Option[String] // Though this is an Option, its required!

  def oldPassword: Option[String]

  def givenName: Option[String]

  def familyName: Option[String]

  def gender: Option[Gender]

  def avatar: UpdateSpec[String]
}

class DefaultUserSpec extends UserSpec {

  lazy val contacts = UpdateSpecImpl[ContactsSpec]()

  lazy val homeAddress = UpdateSpecImpl[AddressInfoSpec]()

  lazy val workAddress = UpdateSpecImpl[AddressInfoSpec]()

  lazy val primaryEmail: Option[String] = None

  lazy val password: Option[String] = None

  lazy val oldPassword: Option[String] = None

  lazy val givenName: Option[String] = None

  lazy val familyName: Option[String] = None

  lazy val gender: Option[Gender.Value] = None

  lazy val avatar = UpdateSpecImpl[String]()
}