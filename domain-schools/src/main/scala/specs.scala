package ma.epsilon.schola
package school
package domain

import _root_.ma.epsilon.schola.domain._, school.domain._

import java.time.{ LocalDate, LocalDateTime, Duration, Instant }

trait UniversitySpec {

  def name: Option[String]

  def website: Option[String]

  def contacts: UpdateSpec[ContactInfoSpec]

  def address: UpdateSpec[AddressInfoSpec]
}

class DefaultUniversitySpec extends UniversitySpec {

  lazy val name: Option[String] = None

  lazy val website: Option[String] = None

  lazy val contacts: UpdateSpec[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec]()

  lazy val address: UpdateSpec[AddressInfoSpec] = UpdateSpecImpl[AddressInfoSpec]()
}

trait OrgSpec {

  def name: Option[String]

  def acronyms: Option[String]

  def website: Option[String]

  def contacts: UpdateSpec[ContactInfoSpec]

  def address: UpdateSpec[AddressInfoSpec]

  def university: UpdateSpec[Uuid]
}

class DefaultOrgSpec extends OrgSpec {

  lazy val name: Option[String] = None

  lazy val acronyms: Option[String] = None

  lazy val website: Option[String] = None

  lazy val contacts: UpdateSpec[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec]()

  lazy val address: UpdateSpec[AddressInfoSpec] = UpdateSpecImpl[AddressInfoSpec]()

  lazy val university: UpdateSpec[Uuid] = UpdateSpecImpl[Uuid]()
}

trait OrgSettingSpec {

  def sessionDuration: Option[Duration]

  def weekDays: Option[WeekDays]

  def startOfInscription: Option[LocalDate]

  def endOfInscription: Option[LocalDate]

  def attendanceEnabled: Option[Boolean]
}

trait DefaultOrgSettingSpec extends OrgSettingSpec {

  lazy val sessionDuration: Option[Duration] = None

  lazy val weekDays: Option[WeekDays] = None

  lazy val startOfInscription: Option[LocalDate] = None

  lazy val endOfInscription: Option[LocalDate] = None

  lazy val attendanceEnabled: Option[Boolean] = None
}

trait DeptSpec {

  def name: Option[String]

  def departmentChefId: UpdateSpec[Uuid]
}

class DefaultDeptSpec extends DeptSpec {

  lazy val name: Option[String] = None

  lazy val departmentChefId: UpdateSpec[Uuid] = UpdateSpecImpl[Uuid]()
}

trait BatchSpec {

  def employee: Option[Uuid]

  def saveTeachingHistory: Boolean

  def updatedBy: Option[Uuid]
}

class DefaultBatchSpec extends BatchSpec {

  lazy val employee: Option[Uuid] = None

  lazy val saveTeachingHistory = true

  lazy val updatedBy: Option[Uuid] = None
}

trait EmployeeSpec extends UserSpec {

  // def jobTitle: Option[String]

  def dept: UpdateSpec[Uuid]
}

class DefaultEmployeeSpec extends DefaultUserSpec {

  // lazy val jobTitle: Option[String] = None

  lazy val dept: UpdateSpec[Uuid] = UpdateSpecImpl[Uuid]()
}

trait StudentSpec extends UserSpec {

  def dateOB: Option[LocalDate]

  def nationality: Option[String]
}

class DefaultStudentSpec extends DefaultUserSpec {

  lazy val dateOB: Option[LocalDate] = None

  lazy val nationality: Option[String] = None
}

trait ScheduledSpec {

  def name: Option[String]

  def during: Option[Range[Instant]]

  def coefficient: Option[Double]
}

class DefaultScheduledSpec extends ScheduledSpec {

  lazy val name: Option[String] = None

  lazy val during: Option[Range[Instant]] = None

  lazy val coefficient: Option[Double] = None
}
