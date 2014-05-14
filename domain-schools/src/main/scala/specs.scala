package ma.epsilon.schola
package school
package domain

import _root_.ma.epsilon.schola.domain._, school.domain._

trait UniversitySpec {

  def name: Option[String]

  def contacts: UpdateSpec[AddressInfoSpec]

  def address: UpdateSpec[AddressInfoSpec]  
}

class DefaultUniversitySpec {

  lazy val name: Option[String] = None

  lazy val contacts: UpdateSpec[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec]()

  lazy val address: UpdateSpec[AddressInfoSpec] = UpdateSpecImpl[AddressInfoSpec]()
}

trait OrgSpec {
  
  def name: Option[String]

  def contacts: UpdateSpec[AddressInfoSpec]

  def address: UpdateSpec[AddressInfoSpec]  

  def university: UpdateSpec[Long]
}

class DefaultOrgSpec {
  
  lazy val name: Option[String] = None

  lazy val contacts: UpdateSpec[ContactInfoSpec] = UpdateSpecImpl[ContactInfoSpec]()

  lazy val address: UpdateSpec[AddressInfoSpec] = UpdateSpecImpl[AddressInfoSpec]()

  lazy val university: UpdateSpec[Long] = UpdateSpecImpl[Long]()
}

trait DeptSpec {
  
  def name: Option[String]

  def departmentChefId: UpdateSpec[String]
}

class DefaultDeptSpec {
  
  lazy val name: Option[String] = None

  lazy val departmentChefId: UpdateSpec[String] = UpdateSpecImpl[String]()
}

trait CourseSpec {

  def name: Option[String]
  
  def code: Option[String]
}

class DefaultCourseSpec {

  lazy val name: Option[String] = None
  
  lazy val code: Option[String] = None
}

trait SubjectSpec {

  def name: Option[String]
}

class DefaultSubjectSpec {
  lazy val name: Option[String] = None
}

trait BatchSpec {
  def name: Option[String]
}

class DefaultBatchSpec {
  lazy val name: Option[String] = None
}

trait EmployeeSpec extends UserSpec {
  
  def jobTitle: Option[String]

  def dept: Option[Long]
}

class DefaultEmployeeSpec extends DefaultUserSpec {
  
  lazy val jobTitle: Option[String] = None

  lazy val dept: Option[Long] = None
}

trait StudentSpec extends UserSpec {
  
  def dateOB: Option[java.time.LocalDate]

  def nationality: Option[String]
}

class DefaultStudentSpec extends DefaultUserSpec {
  
  lazy val dateOB: Option[java.time.LocalDate] = None

  lazy val nationality: Option[String] = None
}

trait ExamCategorySpec {
  
  def name: Option[String]

  def startDate: Option[java.time.LocalDate]
  
  def endDate: Option[java.time.LocalDate]
}

class DefaultExamCategorySpec {
  
  lazy val name: Option[String] = None

  lazy val startDate: Option[java.time.LocalDate] = None
  
  lazy val endDate: Option[java.time.LocalDate] = None
}