package ma.epsilon.schola
package domain

sealed trait SetSpec[T] {
  import SetSpec.Cmd

  def value: Option[Cmd[T]]

  final def map(f: Cmd[T] => Boolean) = value.map(f).getOrElse(true)
}

object SetSpec {

  sealed trait Cmd[T] {
    def values: Set[T]
  }

  case class Only[T](values: Set[T]) extends Cmd[T]
  case class Add[T](values: Set[T]) extends Cmd[T]
  case class Remove[T](values: Set[T]) extends Cmd[T]

  @inline def empty[T] = new SetSpec[T] { val value = None }

  def apply[T](v: Cmd[T]) = new SetSpec[T] { val value = Some(v) }

  def only[T](values: Set[T]) = SetSpec(Only(values))
}

sealed trait UpdateSpec[T] {
  def set: Option[Option[T]]

  @inline final def foreach(f: Option[T] => Boolean) = set map f getOrElse true

  @inline final def isEmpty = set eq None
}

case class UpdateSpecImpl[T](set: Option[Option[T]] = None) extends UpdateSpec[T]

case class ContactInfoSpec(
  email: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  fax: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  phoneNumber: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

case class AddressInfoSpec(
  city: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  country: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  postalCode: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
  streetAddress: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

trait UserSpec {

  case class MobileNumbersSpec(
    mobile1: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    mobile2: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactsSpec(
    mobiles: UpdateSpecImpl[MobileNumbersSpec] = UpdateSpecImpl[MobileNumbersSpec](),
    home: UpdateSpecImpl[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec](),
    work: UpdateSpecImpl[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec](),
    site: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  def contacts: UpdateSpec[ContactsSpec]

  def homeAddress: UpdateSpec[AddressInfoSpec]

  def workAddress: UpdateSpec[AddressInfoSpec]

  def cin: Option[String]

  def stars: Option[Int]

  def primaryEmail: Option[String]

  def password: Option[String] // Though this is an Option, its required!

  def oldPassword: Option[String]

  def givenName: Option[String]

  def familyName: Option[String]

  def jobTitle: Option[String]

  def gender: Option[Gender]

  // def avatar: UpdateSpec[String]

  // def accessRights: Option[Set[Uuid]]
  def accessRights: SetSpec[Uuid]

  def suspended: Option[Boolean]

  def updatedBy: Option[Uuid]
}

class DefaultUserSpec extends UserSpec {

  lazy val contacts = UpdateSpecImpl[ContactsSpec]()

  lazy val homeAddress = UpdateSpecImpl[AddressInfoSpec]()

  lazy val workAddress = UpdateSpecImpl[AddressInfoSpec]()

  lazy val cin: Option[String] = None

  lazy val primaryEmail: Option[String] = None

  lazy val password: Option[String] = None

  lazy val oldPassword: Option[String] = None

  lazy val givenName: Option[String] = None

  lazy val familyName: Option[String] = None

  lazy val jobTitle: Option[String] = None

  lazy val stars: Option[Int] = None

  lazy val gender: Option[Gender.Value] = None

  // lazy val avatar = UpdateSpecImpl[String]()

  // lazy val accessRights: Option[Set[Uuid]] = None
  lazy val accessRights: SetSpec[Uuid] = SetSpec.empty[Uuid]

  lazy val suspended: Option[Boolean] = None

  lazy val updatedBy: Option[Uuid] = None
}