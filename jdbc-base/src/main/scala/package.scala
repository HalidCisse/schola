package ma.epsilon.schola

package jdbc

import slick.driver.PostgresDriver
import com.github.tminglei.slickpg._

trait ScholaPostgresDriver extends PostgresDriver
    with PgArraySupport
    with PgDate2Support
    with PgRangeSupport
    // with PgHStoreSupport
    // with PgPlayJsonSupport
    // with PgSearchSupport
    with PgEnumSupport {

  override lazy val Implicit = new ImplicitsPlus {}
  override val simple = new SimpleQLPlus with DateTimeImplicits {}

  trait ImplicitsPlus extends Implicits
    with ArrayImplicits
    with DateTimeImplicits
    with RangeImplicits
  // with HStoreImplicits
  // with JsonImplicits
  // with SearchImplicits

  trait SimpleQLPlus extends SimpleQL
    with ImplicitsPlus
    // with SearchAssistants
    with EnumImplicits

  trait EnumImplicits {
    import domain._
    import school.domain._

    implicit val genderTypeMapper = createEnumJdbcType("gender", Gender)
    implicit val genderListTypeMapper = createEnumListJdbcType("gender", Gender)

    implicit val inscriptionStatusTypeMapper = createEnumJdbcType("InscriptionStatus", InscriptionStatus)
    implicit val inscriptionStatusListTypeMapper = createEnumListJdbcType("InscriptionStatus", InscriptionStatus)

    implicit val closureStatusTypeMapper = createEnumJdbcType("ClosureStatus", ClosureStatus)
    implicit val closureStatusListTypeMapper = createEnumListJdbcType("ClosureStatus", ClosureStatus)

    implicit val guardianRelationTypeMapper = createEnumJdbcType("GuardianRelation", GuardianRelation)
    implicit val guardianRelationListTypeMapper = createEnumListJdbcType("GuardianRelation", GuardianRelation)

    implicit val timetableEventTypeTypeMapper = createEnumJdbcType("TimetableEventType", TimetableEventType)
    implicit val timetableEventTypeListTypeMapper = createEnumListJdbcType("TimetableEventType", TimetableEventType)

    implicit val genderColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(Gender)
    implicit val inscriptionStatusColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(InscriptionStatus)
    implicit val closureStatusColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(ClosureStatus)
    implicit val guardianRelationColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(GuardianRelation)
    implicit val timetableEventTypeColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(TimetableEventType)
  }
}

object ScholaPostgresDriver extends ScholaPostgresDriver

object `package` {

  val Q = ScholaPostgresDriver.simple

  // ----------------------------------------------------------------------------

  import scala.util.DynamicVariable

  val page = new DynamicVariable[Int](0)

  val pageSize = new DynamicVariable[Int](100)

  implicit def _pagination = Page(page.value * pageSize.value, pageSize.value)
}

trait WithDatabase {
  protected def db: Q.Database
}