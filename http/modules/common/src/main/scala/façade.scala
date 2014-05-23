package ma.epsilon.schola
package http

import play.api.{ Plugin, Application }

import com.typesafe.plugin._

import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.mchange.v2.c3p0.ComboPooledDataSource
import com.jolbox.bonecp.BoneCPDataSource

import caching.impl.{ CacheSystem, CacheSystemProvider }

import com.github.tminglei.slickpg.PgEnumSupportUtils

trait DB extends Plugin {
  def db: jdbc.Q.Database
}

class C3P0(app: Application) extends DB {
  val db = jdbc.Q.Database.forDataSource(new ComboPooledDataSource)
}

class BoneCP(app: Application) extends DB {
  val db = jdbc.Q.Database.forDataSource(new BoneCPDataSource)
}

trait Façade extends MainFaçade
  with AdminFaçade
  with SchoolFaçade
  with Plugin

trait MainFaçade extends impl.OAuthServicesRepoComponentImpl
  with impl.OAuthServicesComponentImpl
  with impl.AppsRepoImpl
  with impl.AppsImpl
  with MailingComponent
  with MailingComponentImpl
  with AkkaSystemProvider
  with CachingServicesComponent
  with CacheSystemProvider
  with jdbc.WithDatabase

trait AdminFaçade extends impl.UserServicesComponentImpl
    with impl.UserServicesRepoComponentImpl
    with impl.LabelServicesRepoComponentImpl
    with impl.LabelServicesComponentImpl
    with impl.UploadServicesRepoComponentImpl
    with impl.UploadServicesComponentImpl
    with impl.CachingUserServicesComponentImpl
    with impl.CachingServicesComponentImpl {

  this: MailingComponent with OAuthServicesComponent with AkkaSystemProvider with CachingServicesComponent with CacheSystemProvider with jdbc.WithDatabase =>
}

trait SchoolFaçade extends school.impl.SchoolServicesComponentImpl

    with school.impl.CommonSchoolServicesComponentImpl
    with school.impl.CommonSchoolServicesRepoComponentImpl

    with school.impl.CoursesSchoolServicesComponentImpl
    with school.impl.CoursesSchoolServicesRepoComponentImpl

    with school.impl.EmployeeServicesComponentImpl
    with school.impl.EmployeeServicesRepoComponentImpl

    with school.impl.StudentServicesComponentImpl
    with school.impl.StudentServicesRepoComponentImpl

    with school.impl.ControlsServicesComponentImpl
    with school.impl.ControlsServicesRepoComponentImpl {

  this: jdbc.WithDatabase with UserServicesComponent =>
}

class DefaultFaçade(app: Application) extends Façade {

  implicit lazy val system = play.libs.Akka.system

  lazy val cacheSystem = new CacheSystem(config.getInt("cache.ttl"))

  lazy val uploadServices = new UploadServicesImpl

  lazy val oauthService = new OAuthServicesImpl

  lazy val userService = new UserServicesImpl with CachingUserServicesImpl {}

  lazy val labelService = new LabelServicesImpl

  lazy val appService = new AppServicesImpl

  lazy val schoolService = new SchoolServicesImpl

  protected lazy val db = use[DB].db

  lazy val mailer = new MailerImpl

  // ----------------------------------------------------------------------------------------------------------

  import schema._
  import domain._
  import school.domain._
  import jdbc.Q._

  private val log = Logger("schola.Façade")

  def withTransaction[T](f: jdbc.Q.Session => T) =
    db.withTransaction {
      f
    }

  def withSession[T](f: jdbc.Q.Session => T) =
    db.withSession {
      f
    }

  def drop = db withTransaction {
    implicit session =>
      import scala.slick.jdbc.{ StaticQuery => T }

      (T[Int] + """ alter table if exists "org_compaigns" drop constraint "COMPAIGN_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "modules" drop constraint "MODULE_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses_modules" drop constraint "ORG_COURSE_MODULE_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses_modules" drop constraint "ORG_COURSE_MODULE_COURSE_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses_modules" drop constraint "ORG_COURSE_MODULE_COMPOSITION_COMPAIGN_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses_modules" drop constraint "ORG_COURSE_MODULE_MODULE_FK" """).execute
      (T[Int] + """ alter table if exists "batch" drop constraint "ORG_MODULE_SUBJECT_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "batch" drop constraint "ORG_MODULE_SUBJECT_EMPLOYEE_FK" """).execute
      (T[Int] + """ alter table if exists "batch" drop constraint "ORG_MODULE_SUBJECT_OrgCoursesModules_FK" """).execute
      (T[Int] + """ alter table if exists "batch" drop constraint "ORG_MODULE_SUBJECT_SUBJECT_FK" """).execute
      (T[Int] + """ alter table if exists "org_compositions" drop constraint "COMPOSITION_COMPAIGN_FK" """).execute
      (T[Int] + """ alter table if exists "org_settings" drop constraint "ORG_SETTING_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "teaching_histories" drop constraint "TEACHING_HISTORY_BATCH_FK" """).execute
      (T[Int] + """ alter table if exists "teaching_histories" drop constraint "TEACHING_HISTORY_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "teaching_histories" drop constraint "TEACHING_HISTORY_EMPLOYEE_FK" """).execute
      (T[Int] + """ alter table if exists "attendances" drop constraint "ATTENDANCE_USER_FK" """).execute
      (T[Int] + """ alter table if exists "attendances" drop constraint "ATTENDANCE_TIMETABLE_EVENT_FK" """).execute
      (T[Int] + """ alter table if exists "attendances" drop constraint "ATTENDANCE_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "events" drop constraint "TIMETABLE_EVENT_BATCH_FK" """).execute
      (T[Int] + """ alter table if exists "events" drop constraint "TIMETABLE_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "marks" drop constraint "MARK_CONTROL_FK" """).execute
      (T[Int] + """ alter table if exists "marks" drop constraint "MARK_STUDENT_FK" """).execute
      (T[Int] + """ alter table if exists "marks" drop constraint "MARK_EMPLOYEE_FK" """).execute
      (T[Int] + """ alter table if exists "controls" drop constraint "CONTROL_BATCH_FK" """).execute
      (T[Int] + """ alter table if exists "controls" drop constraint "CONTROL_TYPE_FK" """).execute
      (T[Int] + """ alter table if exists "controls" drop constraint "TIMETABLE_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "controls" drop constraint "CONTROL_EVENT_FK" """).execute
      (T[Int] + """ alter table if exists "controls_categories" drop constraint "CONTROL_CATEGORY_FK" """).execute
      (T[Int] + """ alter table if exists "inscriptions" drop constraint "INSCRIPTION_COMPAIGN_FK" """).execute
      (T[Int] + """ alter table if exists "inscriptions" drop constraint "INSCRIPTION_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "inscriptions" drop constraint "INSCRIPTION_ADMISSION_FK" """).execute
      (T[Int] + """ alter table if exists "admissions" drop constraint "ADMISSION_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "admissions" drop constraint "ADMISSION_COURSE_FK" """).execute
      (T[Int] + """ alter table if exists "admissions" drop constraint "ADMISSION_STUDENT_FK" """).execute
      (T[Int] + """ alter table if exists "admissions" drop constraint "ADMISSION_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "students" drop constraint "STUDENT_USER_FK" """).execute
      (T[Int] + """ alter table if exists "students" drop constraint "STUDENT_GUARDIAN_FK" """).execute
      (T[Int] + """ alter table if exists "guardians" drop constraint "GUARDIAN_USER_FK" """).execute
      (T[Int] + """ alter table if exists "org_employments" drop constraint "ORG_EMPLOYEE_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "org_employments" drop constraint "ORG_EMPLOYEE_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "org_employments" drop constraint "ORG_EMPLOYEE_DEPT_FK" """).execute
      (T[Int] + """ alter table if exists "org_employments" drop constraint "ORG_EMPLOYEE_EMPLOYEE_FK" """).execute
      (T[Int] + """ alter table if exists "employees" drop constraint "EMPLOYEE_USER_FK" """).execute
      (T[Int] + """ alter table if exists "org_subjects" drop constraint "ORG_SUBJECT_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses" drop constraint "ORG_COURSE_COURSE_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses" drop constraint "ORG_COURSE_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "org_courses" drop constraint "ORG_COURSE_DEPT_FK" """).execute
      (T[Int] + """ alter table if exists "depts" drop constraint "DEPT_ORG_FK" """).execute
      (T[Int] + """ alter table if exists "depts" drop constraint "DEPT_EMPLOYEE_FK" """).execute
      (T[Int] + """ alter table if exists "orgs" drop constraint "ORG_UNIVERSITY_FK" """).execute
      (T[Int] + """ alter table if exists "orgs" drop constraint "ORG_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "org_compaigns" drop constraint "COMPAIGN_PK" """).execute
      (T[Int] + """ drop table if exists "org_compaigns" """).execute
      (T[Int] + """ alter table if exists "modules" drop constraint "MODULE_PK" """).execute
      (T[Int] + """ drop table if exists "modules" """).execute
      (T[Int] + """ alter table if exists "org_courses_modules" drop constraint "ORG_COURSE_MODULE_PK" """).execute
      (T[Int] + """ drop table if exists "org_courses_modules" """).execute
      (T[Int] + """ alter table if exists "batch" drop constraint "ORG_MODULE_SUBJECT_PK" """).execute
      (T[Int] + """ drop table if exists "batch" """).execute
      (T[Int] + """ alter table if exists "org_compositions" drop constraint "COMPOSITION_PK" """).execute
      (T[Int] + """ drop table if exists "org_compositions" """).execute
      (T[Int] + """ alter table if exists "org_settings" drop constraint "ORG_SETTING_PK" """).execute
      (T[Int] + """ drop table if exists "org_settings" """).execute
      (T[Int] + """ alter table if exists "teaching_histories" drop constraint "TEACHING_HISTORY_PK" """).execute
      (T[Int] + """ drop table if exists "teaching_histories" """).execute
      (T[Int] + """ alter table if exists "attendances" drop constraint "ATTENDANCE_PK" """).execute
      (T[Int] + """ drop table if exists "attendances" """).execute
      (T[Int] + """ alter table if exists "events" drop constraint "TIMETABLE_EVENT_PK" """).execute
      (T[Int] + """ drop table if exists "events" """).execute
      (T[Int] + """ alter table if exists "marks" drop constraint "MARK_PK" """).execute
      (T[Int] + """ drop table if exists "marks" """).execute
      (T[Int] + """ alter table if exists "controls" drop constraint "CONTROL_PK" """).execute
      (T[Int] + """ drop table if exists "controls" """).execute
      (T[Int] + """ alter table if exists "controls_categories" drop constraint "CONTROL_CATEGORY_PK" """).execute
      (T[Int] + """ drop table if exists "controls_categories" """).execute
      (T[Int] + """ alter table if exists "inscriptions" drop constraint "INSCRIPTION_PK" """).execute
      (T[Int] + """ drop table if exists "inscriptions" """).execute
      (T[Int] + """ alter table if exists "admissions" drop constraint "ADMISSION_PK" """).execute
      (T[Int] + """ drop table if exists "admissions" """).execute
      (T[Int] + """ alter table if exists "students" drop constraint "STUDENT_PK" """).execute
      (T[Int] + """ drop table if exists "students" """).execute
      (T[Int] + """ alter table if exists "guardians" drop constraint "GUARDIAN_PK" """).execute
      (T[Int] + """ drop table if exists "guardians" """).execute
      (T[Int] + """ alter table if exists "org_employments" drop constraint "ORG_EMPLOYEE_PK" """).execute
      (T[Int] + """ drop table if exists "org_employments" """).execute
      (T[Int] + """ alter table if exists "employees" drop constraint "EMPLOYEE_PK" """).execute
      (T[Int] + """ drop table if exists "employees" """).execute
      (T[Int] + """ alter table if exists "org_subjects" drop constraint "ORG_SUBJECT_PK" """).execute
      (T[Int] + """ drop table if exists "org_subjects" """).execute
      (T[Int] + """ alter table if exists "org_courses" drop constraint "ORG_COURSE_PK" """).execute
      (T[Int] + """ drop table if exists "org_courses" """).execute
      (T[Int] + """ alter table if exists "courses" drop constraint "COURSE_PK" """).execute
      (T[Int] + """ drop table if exists "courses" """).execute
      (T[Int] + """ alter table if exists "depts" drop constraint "DEPT_PK" """).execute
      (T[Int] + """ drop table if exists "depts" """).execute
      (T[Int] + """ alter table if exists "orgs" drop constraint "ORG_PK" """).execute
      (T[Int] + """ drop table if exists "orgs" """).execute
      (T[Int] + """ alter table if exists "universities" drop constraint "UNIVERSITY_PK" """).execute
      (T[Int] + """ drop table if exists "universities" """).execute

      //

      (T[Int] + """ alter table if exists "users_labels" drop constraint "USER_LABEL_USER_FK" """).execute
      (T[Int] + """ alter table if exists "users_labels" drop constraint "USER_LABEL_LABEL_FK" """).execute
      (T[Int] + """ alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_USER_FK" """).execute
      (T[Int] + """ alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_USER_GRANTOR_FK" """).execute
      (T[Int] + """ alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_ACCESS_RIGHT_FK" """).execute
      (T[Int] + """ alter table if exists "access_rights" drop constraint "ACCESS_RIGHT_APP_FK" """).execute
      (T[Int] + """ alter table if exists "oauth_tokens" drop constraint "TOKEN_USER_FK" """).execute
      (T[Int] + """ alter table if exists "users" drop constraint "USER_CREATOR_FK" """).execute
      (T[Int] + """ alter table if exists "users" drop constraint "USER_MODIFIER_FK" """).execute
      (T[Int] + """ alter table if exists "users_labels" drop constraint "USER_LABEL_PK" """).execute
      (T[Int] + """ drop table if exists "users_labels" """).execute
      (T[Int] + """ alter table if exists "labels" drop constraint "LABEL_PK" """).execute
      (T[Int] + """ drop table if exists "labels" """).execute
      (T[Int] + """ alter table if exists "users_access_rights" drop constraint "USER_ACCESS_RIGHT_PK" """).execute
      (T[Int] + """ drop table if exists "users_access_rights" """).execute
      (T[Int] + """ alter table if exists "access_rights" drop constraint "ACCESS_RIGHT_PK" """).execute
      (T[Int] + """ drop table if exists "access_rights" """).execute
      (T[Int] + """ alter table if exists "apps" drop constraint "APP_PK" """).execute
      (T[Int] + """ drop table if exists "apps" """).execute
      (T[Int] + """ alter table if exists "oauth_tokens" drop constraint "TOKEN_PK" """).execute
      (T[Int] + """ drop table if exists "oauth_tokens" """).execute
      (T[Int] + """ alter table if exists "users" drop constraint "USER_PK" """).execute
      (T[Int] + """ drop table if exists "users" """).execute

      (T[Int] + """ DROP EXTENSION if exists "uuid-ossp" """).execute

      (T[Int] + """ DROP EXTENSION if exists "btree_gist" """).execute

      (T[Int] + "drop cast if exists (varchar as json)").execute

      utils.tryo { PgEnumSupportUtils.buildDropSql("Gender").execute }
      utils.tryo { PgEnumSupportUtils.buildDropSql("InscriptionStatus").execute }
      utils.tryo { PgEnumSupportUtils.buildDropSql("ClosureStatus").execute }
      utils.tryo { PgEnumSupportUtils.buildDropSql("GuardianRelation").execute }
      utils.tryo { PgEnumSupportUtils.buildDropSql("TimetableEventType").execute }
      utils.tryo { PgEnumSupportUtils.buildDropSql("ModuleType").execute }
  }

  def init(userId: Uuid) = db withTransaction {
    implicit session =>

      import scala.slick.jdbc.{ StaticQuery => T }

      try {

        utils.tryo { PgEnumSupportUtils.buildCreateSql("ModuleType", ModuleType).execute }
        utils.tryo { PgEnumSupportUtils.buildCreateSql("Gender", Gender).execute }
        utils.tryo { PgEnumSupportUtils.buildCreateSql("InscriptionStatus", InscriptionStatus).execute }
        utils.tryo { PgEnumSupportUtils.buildCreateSql("ClosureStatus", ClosureStatus).execute }
        utils.tryo { PgEnumSupportUtils.buildCreateSql("GuardianRelation", GuardianRelation).execute }
        utils.tryo { PgEnumSupportUtils.buildCreateSql("TimetableEventType", TimetableEventType).execute }

        /*
          val ddl = _root_.ma.epsilon.schola.schema.Users.ddl ++ OAuthTokens.ddl ++ Apps.ddl ++ AccessRights.ddl ++ UsersAccessRights.ddl ++ Labels.ddl ++ UsersLabels.ddl // ++ OAuthClients.ddl
        */

        (T[Int] + "create cast (varchar as json) without function as implicit").execute

        (T[Int] + """ CREATE EXTENSION "uuid-ossp" """).execute

        (T[Int] + """ CREATE EXTENSION "btree_gist" """).execute

        (T[Int] + """ create table "users" ("cin" VARCHAR(254) NOT NULL,"primary_email" VARCHAR(254) NOT NULL,"password" text NOT NULL,"given_name" VARCHAR(254) NOT NULL,"family_name" VARCHAR(254) NOT NULL,"job_title" VARCHAR(254) NOT NULL,"created_at" TIMESTAMP NOT NULL,"created_by" uuid,"last_login_time" TIMESTAMP,"last_modified_at" TIMESTAMP,"last_modified_by" uuid,"stars" INTEGER DEFAULT 0 NOT NULL,"gender" gender DEFAULT 'Male' NOT NULL,"home_address" json,"work_address" json,"contacts" json,"user_activation_key" text,"_deleted" BOOLEAN DEFAULT false NOT NULL,"suspended" BOOLEAN DEFAULT false NOT NULL,"change_password_at_next_login" BOOLEAN DEFAULT false NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "users" add constraint "USER_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "USER_USERNAME_INDEX" on "users" ("primary_email") """).execute
        (T[Int] + """ create unique index "USER_CIN_INDEX" on "users" ("cin") """).execute
        (T[Int] + """ create table "oauth_tokens" ("access_token" VARCHAR(254) NOT NULL,"user_id" uuid NOT NULL,"refresh_token" VARCHAR(254),"secret" VARCHAR(254) NOT NULL,"user_agent" text NOT NULL,"expires_in" interval,"refresh_expires_in" interval,"created_at" TIMESTAMP NOT NULL,"last_access_time" TIMESTAMP NOT NULL,"token_type" VARCHAR(254) DEFAULT 'mac' NOT NULL,"access_rights" json DEFAULT '[]' NOT NULL,"active_access_right_id" uuid) """).execute
        (T[Int] + """ alter table "oauth_tokens" add constraint "TOKEN_PK" primary key("access_token") """).execute
        (T[Int] + """ create index "TOKEN_REFRESH_TOKEN_INDEX" on "oauth_tokens" ("refresh_token") """).execute
        (T[Int] + """ create table "apps" ("name" VARCHAR(254) NOT NULL,"scopes" text ARRAY DEFAULT '{}' NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "apps" add constraint "APP_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "APP_NAME_INDEX" on "apps" ("name") """).execute
        (T[Int] + """ create table "access_rights" ("alias" VARCHAR(254) NOT NULL,"display_name" VARCHAR(254) NOT NULL,"redirect_uri" VARCHAR(254) NOT NULL,"app_id" uuid NOT NULL,"scopes" json DEFAULT '[]' NOT NULL,"grant_options" uuid ARRAY DEFAULT '{}' NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "access_rights" add constraint "ACCESS_RIGHT_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "ACCESS_RIGHT_ALIAS_INDEX" on "access_rights" ("alias") """).execute
        (T[Int] + """ create table "users_access_rights" ("user_id" uuid NOT NULL,"access_right_id" uuid NOT NULL,"granted_at" TIMESTAMP NOT NULL,"granted_by" uuid) """).execute
        (T[Int] + """ alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_PK" primary key("user_id","access_right_id") """).execute
        (T[Int] + """ create table "labels" ("name" VARCHAR(254) NOT NULL,"color" VARCHAR(254) NOT NULL) """).execute
        (T[Int] + """ alter table "labels" add constraint "LABEL_PK" primary key("name") """).execute
        (T[Int] + """ create table "users_labels" ("user_id" uuid NOT NULL,"label" VARCHAR(254) NOT NULL) """).execute
        (T[Int] + """ alter table "users_labels" add constraint "USER_LABEL_PK" primary key("user_id","label") """).execute
        (T[Int] + """ alter table "users" add constraint "USER_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "users" add constraint "USER_MODIFIER_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "oauth_tokens" add constraint "TOKEN_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "access_rights" add constraint "ACCESS_RIGHT_APP_FK" foreign key("app_id") references "apps"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_USER_GRANTOR_FK" foreign key("granted_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "users_access_rights" add constraint "USER_ACCESS_RIGHT_ACCESS_RIGHT_FK" foreign key("access_right_id") references "access_rights"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "users_labels" add constraint "USER_LABEL_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "users_labels" add constraint "USER_LABEL_LABEL_FK" foreign key("label") references "labels"("name") on update CASCADE on delete CASCADE """).execute

        //

        (T[Int] + """ create table "universities" ("name" VARCHAR(254) NOT NULL,"website" VARCHAR(254),"contacts" json DEFAULT '{}' NOT NULL,"address" json DEFAULT '{}' NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "universities" add constraint "UNIVERSITY_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "UNIVERSITY_NAME_INDEX" on "universities" ("name") """).execute
        (T[Int] + """ create table "orgs" ("name" VARCHAR(254) NOT NULL,"acronyms" VARCHAR(254),"website" VARCHAR(254),"contacts" json NOT NULL,"address" json NOT NULL,"university_id" UUID,"_deleted" BOOLEAN DEFAULT false NOT NULL,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "orgs" add constraint "ORG_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "ORG_NAME_INDEX" on "orgs" ("name") """).execute
        (T[Int] + """ create table "depts" ("name" VARCHAR(254) NOT NULL,"org" UUID NOT NULL,"department_chef_id" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "depts" add constraint "DEPT_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "DEPT_NAME_INDEX" on "depts" ("org","name") """).execute
        (T[Int] + """ create table "courses" ("name" VARCHAR(254) NOT NULL,"code" VARCHAR(254),"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "courses" add constraint "COURSE_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "COURSE_NAME_INDEX" on "courses" ("name") """).execute
        (T[Int] + """ create unique index "COURSE_CODE_INDEX" on "courses" ("code") """).execute
        (T[Int] + """ create table "org_courses" ("org" UUID NOT NULL,"levels" INTEGER NOT NULL,"dept_id" UUID NOT NULL,"desc" text,"course_id" UUID NOT NULL) """).execute
        (T[Int] + """ alter table "org_courses" add constraint "ORG_COURSE_PK" primary key("org","course_id") """).execute
        (T[Int] + """ create table "org_subjects" ("org" UUID NOT NULL,"name" VARCHAR(254) NOT NULL,"desc" text,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "org_subjects" add constraint "ORG_SUBJECT_PK" primary key("id") """).execute
        (T[Int] + """ create index "ORG_SUBJECT_NAME_INDEX" on "org_subjects" ("name") """).execute
        (T[Int] + """ create unique index "ORG_SUBJECT_ORG_NAME_INDEX" on "org_subjects" ("org","name") """).execute
        (T[Int] + """ create table "employees" ("emp_no" VARCHAR(254) NOT NULL,"user_id" UUID NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "employees" add constraint "EMPLOYEE_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "EMPLOYEE_EMP_NO_INDEX" on "employees" ("emp_no") """).execute
        (T[Int] + """ create table "org_employments" ("org" UUID NOT NULL,"emp_id" UUID NOT NULL,"dept_id" UUID,"join_date" DATE NOT NULL,"end_date" TIMESTAMP,"end_status" ClosureStatus,"end_remarques" VARCHAR(254),"created_by" UUID) """).execute
        (T[Int] + """ alter table "org_employments" add constraint "ORG_EMPLOYEE_PK" primary key("org","emp_id") """).execute
        (T[Int] + """ create table "guardians" ("relation" GuardianRelation NOT NULL,"user_id" UUID NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "guardians" add constraint "GUARDIAN_PK" primary key("id") """).execute
        (T[Int] + """ create table "students" ("reg_no" VARCHAR(254) NOT NULL,"date_ob" DATE NOT NULL,"nationality" VARCHAR(254) NOT NULL,"user_id" UUID NOT NULL,"guardian_id" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "students" add constraint "STUDENT_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "STUDENT_REG_NO_INDEX" on "students" ("reg_no") """).execute
        (T[Int] + """ create table "admissions" ("org" UUID NOT NULL,"student_id" UUID NOT NULL,"course_id" UUID NOT NULL,"inscription_status" InscriptionStatus DEFAULT 'PendingApproval' NOT NULL,"end_status" ClosureStatus,"end_remarques" VARCHAR(254),"adm_date" DATE NOT NULL,"end_date" TIMESTAMP,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "admissions" add constraint "ADMISSION_PK" primary key("id") """).execute
        (T[Int] + """ create table "inscriptions" ("admission_id" UUID NOT NULL,"compaign_id" UUID NOT NULL,"level" INTEGER NOT NULL,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "inscriptions" add constraint "INSCRIPTION_PK" primary key("id") """).execute
        (T[Int] + """ create table "controls_categories" ("name" VARCHAR(254) NOT NULL,"during" tsrange NOT NULL,"composition_id" UUID NOT NULL,"coefficient" DOUBLE PRECISION,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "controls_categories" add constraint "CONTROL_CATEGORY_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "CONTROL_CATEGORY_NAME_INDEX" on "controls_categories" ("composition_id","name") """).execute
        (T[Int] + """ create table "controls" ("event_id" UUID NOT NULL,"name" VARCHAR(254) NOT NULL,"batch_id" UUID NOT NULL,"supervisors" uuid ARRAY DEFAULT '{}' NOT NULL,"type" UUID NOT NULL,"coefficient" DOUBLE PRECISION,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "controls" add constraint "CONTROL_PK" primary key("id") """).execute
        (T[Int] + """ create table "marks" ("student_id" UUID NOT NULL,"emp_id" UUID NOT NULL,"exam_id" UUID NOT NULL,"marks" DOUBLE PRECISION NOT NULL,"created_at" TIMESTAMP NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "marks" add constraint "MARK_PK" primary key("id") """).execute
        (T[Int] + """ create table "events" ("batch_id" UUID NOT NULL,"type" TimetableEventType DEFAULT 'Lecture' NOT NULL,"class" VARCHAR(254) NOT NULL,"during" tsrange NOT NULL,"recurrence" VARCHAR(254) DEFAULT '"None"' NOT NULL,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "events" add constraint "TIMETABLE_EVENT_PK" primary key("id") """).execute
        (T[Int] + """ create table "attendances" ("user_id" UUID NOT NULL,"event_id" UUID NOT NULL,"created_at" TIMESTAMP NOT NULL,"created_by" UUID) """).execute
        (T[Int] + """ alter table "attendances" add constraint "ATTENDANCE_PK" primary key("user_id","event_id") """).execute
        (T[Int] + """ create table "teaching_histories" ("emp_id" UUID NOT NULL,"batch_id" UUID NOT NULL,"start_date" DATE NOT NULL,"end_date" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "teaching_histories" add constraint "TEACHING_HISTORY_PK" primary key("id") """).execute
        (T[Int] + """ create table "org_settings" ("org" UUID NOT NULL,"session_duration" interval DEFAULT 'PT2H' NOT NULL,"weekdays" VARCHAR(254) DEFAULT '{"Monday":true,"Tuesday":true,"Wednesday":true,"Thursday":true,"Friday":true,"Saturday":false,"Sunday":false}' NOT NULL,"start_of_inscription" DATE,"end_of_inscription" DATE,"enable_attendance" BOOLEAN DEFAULT true NOT NULL) """).execute
        (T[Int] + """ alter table "org_settings" add constraint "ORG_SETTING_PK" primary key("org") """).execute
        (T[Int] + """ create table "org_compositions" ("compaign_id" UUID NOT NULL,"name" VARCHAR(254) NOT NULL,"during" tsrange NOT NULL,"coefficient" DOUBLE PRECISION,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "org_compositions" add constraint "COMPOSITION_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "COMPOSITION_NAME_INDEX" on "org_compositions" ("name","compaign_id") """).execute
        (T[Int] + """ create unique index "COMPOSITION_ID_COMPAIGN_INDEX" on "org_compositions" ("id","compaign_id") """).execute
        (T[Int] + """ create table "batch" ("current_emp_id" UUID NOT NULL,"compaign_id" UUID NOT NULL,"composition_id" UUID NOT NULL,"course_id" UUID NOT NULL,"module_id" UUID NOT NULL,"subject_id" UUID NOT NULL,"level" INTEGER NOT NULL,"coefficient" DOUBLE PRECISION,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "batch" add constraint "ORG_MODULE_SUBJECT_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "ORG_MODULE_SUBJECT_PK_INDEX" on "batch" ("compaign_id","composition_id","course_id","module_id","subject_id","level") """).execute
        (T[Int] + """ create table "org_courses_modules" ("compaign_id" UUID NOT NULL,"composition_id" UUID NOT NULL,"course_id" UUID NOT NULL,"module_id" UUID NOT NULL,"level" INTEGER NOT NULL,"coefficient" DOUBLE PRECISION,"created_at" TIMESTAMP NOT NULL,"created_by" UUID,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "org_courses_modules" add constraint "ORG_COURSE_MODULE_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "ORG_COURSE_MODULE_PK_INDEX" on "org_courses_modules" ("compaign_id","composition_id","course_id","module_id","level") """).execute
        (T[Int] + """ create table "modules" ("org" UUID NOT NULL,"name" VARCHAR(254) NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "modules" add constraint "MODULE_PK" primary key("id") """).execute
        (T[Int] + """ create unique index "MODULE_ORG_NAME_INDEX" on "modules" ("org","name") """).execute
        (T[Int] + """ create table "org_compaigns" ("org" UUID NOT NULL,"during" tsrange NOT NULL,"module_type" VARCHAR(254) DEFAULT '"MODULE"' NOT NULL,"id" uuid NOT NULL DEFAULT uuid_generate_v4()) """).execute
        (T[Int] + """ alter table "org_compaigns" add constraint "COMPAIGN_PK" primary key("id") """).execute
        (T[Int] + """ alter table "orgs" add constraint "ORG_UNIVERSITY_FK" foreign key("university_id") references "universities"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "orgs" add constraint "ORG_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "depts" add constraint "DEPT_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "depts" add constraint "DEPT_EMPLOYEE_FK" foreign key("department_chef_id") references "users"("id") on update RESTRICT on delete SET NULL """).execute
        (T[Int] + """ alter table "org_courses" add constraint "ORG_COURSE_COURSE_FK" foreign key("course_id") references "courses"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "org_courses" add constraint "ORG_COURSE_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "org_courses" add constraint "ORG_COURSE_DEPT_FK" foreign key("dept_id") references "depts"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "org_subjects" add constraint "ORG_SUBJECT_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "employees" add constraint "EMPLOYEE_USER_FK" foreign key("user_id") references "users"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "org_employments" add constraint "ORG_EMPLOYEE_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "org_employments" add constraint "ORG_EMPLOYEE_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "org_employments" add constraint "ORG_EMPLOYEE_DEPT_FK" foreign key("dept_id") references "depts"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "org_employments" add constraint "ORG_EMPLOYEE_EMPLOYEE_FK" foreign key("emp_id") references "employees"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "guardians" add constraint "GUARDIAN_USER_FK" foreign key("user_id") references "users"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "students" add constraint "STUDENT_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "students" add constraint "STUDENT_GUARDIAN_FK" foreign key("guardian_id") references "guardians"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "admissions" add constraint "ADMISSION_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "admissions" add constraint "ADMISSION_COURSE_FK" foreign key("course_id") references "courses"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "admissions" add constraint "ADMISSION_STUDENT_FK" foreign key("student_id") references "users"("id") on update RESTRICT on delete RESTRICT """).execute
        (T[Int] + """ alter table "admissions" add constraint "ADMISSION_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "inscriptions" add constraint "INSCRIPTION_COMPAIGN_FK" foreign key("compaign_id") references "org_compaigns"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "inscriptions" add constraint "INSCRIPTION_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "inscriptions" add constraint "INSCRIPTION_ADMISSION_FK" foreign key("admission_id") references "admissions"("id") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "controls_categories" add constraint "CONTROL_CATEGORY_FK" foreign key("composition_id") references "org_compositions"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "controls" add constraint "CONTROL_BATCH_FK" foreign key("batch_id") references "batch"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "controls" add constraint "CONTROL_TYPE_FK" foreign key("type") references "controls_categories"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "controls" add constraint "TIMETABLE_CREATOR_FK" foreign key("created_by") references "users"("id") on update RESTRICT on delete SET NULL """).execute
        (T[Int] + """ alter table "controls" add constraint "CONTROL_EVENT_FK" foreign key("event_id") references "events"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "marks" add constraint "MARK_CONTROL_FK" foreign key("exam_id") references "controls"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "marks" add constraint "MARK_STUDENT_FK" foreign key("student_id") references "users"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "marks" add constraint "MARK_EMPLOYEE_FK" foreign key("emp_id") references "users"("id") on update RESTRICT on delete RESTRICT """).execute
        (T[Int] + """ alter table "events" add constraint "TIMETABLE_EVENT_BATCH_FK" foreign key("batch_id") references "batch"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "events" add constraint "TIMETABLE_CREATOR_FK" foreign key("created_by") references "users"("id") on update RESTRICT on delete SET NULL """).execute
        (T[Int] + """ alter table "attendances" add constraint "ATTENDANCE_USER_FK" foreign key("user_id") references "users"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "attendances" add constraint "ATTENDANCE_TIMETABLE_EVENT_FK" foreign key("event_id") references "events"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "attendances" add constraint "ATTENDANCE_CREATOR_FK" foreign key("created_by") references "users"("id") on update RESTRICT on delete SET NULL """).execute
        (T[Int] + """ alter table "teaching_histories" add constraint "TEACHING_HISTORY_BATCH_FK" foreign key("batch_id") references "batch"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "teaching_histories" add constraint "TEACHING_HISTORY_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "teaching_histories" add constraint "TEACHING_HISTORY_EMPLOYEE_FK" foreign key("emp_id") references "users"("id") on update RESTRICT on delete RESTRICT """).execute
        (T[Int] + """ alter table "org_settings" add constraint "ORG_SETTING_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "org_compositions" add constraint "COMPOSITION_COMPAIGN_FK" foreign key("compaign_id") references "org_compaigns"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "batch" add constraint "ORG_MODULE_SUBJECT_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "batch" add constraint "ORG_MODULE_SUBJECT_EMPLOYEE_FK" foreign key("current_emp_id") references "users"("id") on update RESTRICT on delete RESTRICT """).execute
        (T[Int] + """ alter table "batch" add constraint "ORG_MODULE_SUBJECT_OrgCoursesModules_FK" foreign key("compaign_id","composition_id","course_id","module_id","level") references "org_courses_modules"("compaign_id","composition_id","course_id","module_id","level") on update CASCADE on delete CASCADE """).execute
        (T[Int] + """ alter table "batch" add constraint "ORG_MODULE_SUBJECT_SUBJECT_FK" foreign key("subject_id") references "org_subjects"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "org_courses_modules" add constraint "ORG_COURSE_MODULE_CREATOR_FK" foreign key("created_by") references "users"("id") on update CASCADE on delete SET NULL """).execute
        (T[Int] + """ alter table "org_courses_modules" add constraint "ORG_COURSE_MODULE_COURSE_FK" foreign key("course_id") references "courses"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "org_courses_modules" add constraint "ORG_COURSE_MODULE_COMPOSITION_COMPAIGN_FK" foreign key("composition_id","compaign_id") references "org_compositions"("id","compaign_id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "org_courses_modules" add constraint "ORG_COURSE_MODULE_MODULE_FK" foreign key("module_id") references "modules"("id") on update CASCADE on delete RESTRICT """).execute
        (T[Int] + """ alter table "modules" add constraint "MODULE_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute
        (T[Int] + """ alter table "org_compaigns" add constraint "COMPAIGN_ORG_FK" foreign key("org") references "orgs"("id") on update RESTRICT on delete CASCADE """).execute

        //

        (T[Int] + """ alter table "org_compositions" add constraint "ORG_COMPOSITION_DURING" EXCLUDE USING gist ((compaign_id :: text) WITH =, during WITH &&) """).execute

        (T[Int] + """ alter table "org_compaigns" add constraint "ORG_COMPAIGN_DURING" EXCLUDE USING gist ((org :: text) WITH =, during WITH &&) """).execute

        (T[Int] + """ alter table "events" add constraint "EVENT_DURING" EXCLUDE USING gist (class WITH =, during WITH &&) """).execute

        (T[Int] + """ alter table "controls_categories" add constraint "CONTROL_CATEGORY_DURING" EXCLUDE USING gist ((composition_id :: text) WITH =, during WITH &&) """).execute

        // Add a client - schola:schola
        val _1 = true // (OAuthClients += OAuthClient("schola", "schola", "http://localhost/schola")) == 1

        //Add a user
        val _2 = Users.forceInsert(U.SuperUser copy (password = U.SuperUser.password map passwords.crypt)) == 1

        /*        val _3 = (Roles ++= List(
          R.SuperUserR,
          R.AdministratorR,
          Role("Role One"),
          Role("Role Two"),
          Role("Role Three", createdBy = Some(userId)),
          Role("Role Four"),
          Role("Role X", Some("Role One"), createdBy = Some(userId)))) == Some(7)

        val _4 = (Permissions ++= List(
          Permission("P1", "oadmin"),
          Permission("P2", "oadmin"),
          Permission("P3", "oadmin"),
          Permission("P4", "oadmin"),

          Permission("P5", "schola"),
          Permission("P6", "schola"),
          Permission("P7", "schola"),
          Permission("P8", "schola"),
          Permission("P9", "schola"),
          Permission("P10", "schola"))) == Some(10)

        val _5 = (RolesPermissions ++= List(
          RolePermission("Role One", "P1"),
          RolePermission("Role One", "P2", grantedBy = Some(userId)),
          RolePermission("Role One", "P3"),
          RolePermission("Role One", "P4"),
          RolePermission("Role One", "P5", grantedBy = Some(userId)))) == Some(5)

        val _6 = (UsersRoles ++= List(
          UserRole(userId, R.SuperUserR.name),
          UserRole(userId, R.AdministratorR.name),
          UserRole(userId, "Role Three", grantedBy = Some(userId)),
          UserRole(userId, "Role Two"))) == Some(3)*/

        val adminApp = Apps insert ("admin", List("users", "stats.admin", "settings.*"))

        val adminReadonlyRight = AccessRights insert ("admin.readonly", "display_name", "rediect_uri", adminApp.id.get, List(
          domain.Scope("users", write = false, trash = false),
          domain.Scope("stats.admin"),
          domain.Scope("settings.*", write = false, trash = false)))

        val adminRight = AccessRights insert ("admin", "display_name", "rediect_uri", adminApp.id.get, List(
          domain.Scope("users"),
          domain.Scope("stats.admin")))

        val adminSettingsRight = AccessRights insert ("admin.settings", "display_name", "rediect_uri", adminApp.id.get, List(
          domain.Scope("users"),
          domain.Scope("stats.admin"),
          domain.Scope("settings.*")))

        val accessRights = List(
          adminReadonlyRight, adminRight, adminSettingsRight)

        val schoolsApp = Apps forceInsert App("schools", scopes = List(), id = Some(Uuid(config.getString("schools.app_id"))))

        // UsersAccessRights insert UserAccessRight(U.SuperUser.id.get, adminSettingsRight.id.get)

        Cache.clearAll

        _1 && _2 /*&& _3 && _4 && _5 && _6*/
      } catch {
        case e: java.sql.SQLException =>
          var cur = e
          while (cur ne null) {
            cur.printStackTrace()
            cur = cur.getNextException
          }
          false
      }
  }

  def genFixtures(implicit system: akka.actor.ActorSystem) = {
    import domain._
    import org.apache.commons.lang3.{ RandomStringUtils => Rnd }

    def rndEmail = s"${Rnd.randomAlphanumeric(7)}@${Rnd.randomAlphanumeric(5)}.${Rnd.randomAlphanumeric(3)}"
    def rndString(len: Int) = Rnd.randomAlphabetic(len).toLowerCase.capitalize
    def rndPhone = Rnd.randomNumeric(9)
    def rndPostalCode = Rnd.randomNumeric(5)
    def rndStreetAddress = s"${rndString(7)} ${rndString(2)} ${rndString(7)} ${rndString(4)}"

    val amadouEpsilon =
      User(
        s"CIN-${rndString(6)}",
        "amadou.cisse@epsilon.ma",
        Some("amsayk"),
        "Amadou",
        "Cisse",
        "Software Engineer",
        createdBy = U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),
        workAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),

        contacts = Some(Contacts(Some(MobileNumbers(Some(rndPhone), None)), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))))),

        changePasswordAtNextLogin = false,
        id = Some(java.util.UUID.randomUUID))

    db.withTransaction {
      implicit session => Users.forceInsert(amadouEpsilon copy (password = amadouEpsilon.password map passwords.crypt))
    }

    val amadouGmail =
      User(
        s"CIN-${rndString(6)}",
        "cisse.amadou.9@gmail.com",
        Some("amsayk"),
        "Ousman",
        "Cisse",
        "Software Engineer",
        createdBy = U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),
        workAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),

        contacts = Some(Contacts(Some(MobileNumbers(Some(rndPhone), None)), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))))),

        changePasswordAtNextLogin = false,
        id = Some(java.util.UUID.randomUUID))

    db.withTransaction {
      implicit session => Users.forceInsert(amadouGmail copy (password = amadouGmail.password map passwords.crypt))
    }

    def rndUser =
      User(
        s"CIN-${rndString(6)}",
        rndEmail.toLowerCase,
        Some(rndString(4)),
        rndString(5),
        rndString(9),
        "Software Engineer",
        createdBy = if (scala.util.Random.nextBoolean) if (scala.util.Random.nextBoolean) amadouGmail.id else amadouEpsilon.id else U.SuperUser.id,
        gender = Gender.Male,

        homeAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),
        workAddress = Some(AddressInfo(Some(rndString(10)), Some(rndString(10)), Some(rndPostalCode), Some(rndStreetAddress))),

        contacts = Some(Contacts(Some(MobileNumbers(Some(rndPhone), None)), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))), Some(ContactInfo(Some(rndEmail), Some(rndPhone), Some(rndPhone))))),

        changePasswordAtNextLogin = false)

    val rndUsers = (0 to 600).par map (_ => rndUser)
    //    val rndRoles = (((0 to 5) map (_ => rndRole)) ++ createRndRoles).par
    //    val rndPermissions = (0 to 50).par map (_ => rndPermission)
    val rndLabels = (0 to 6).par map (i => Label(s"${rndString(6)} $i", "#fff"))

    val users = {
      log.info("Creating users . . . ")

      (rndUsers map { u =>

        userService.saveUser(
          s"CIN-${rndString(6)}",
          u.primaryEmail,
          // u.password.get,
          u.givenName,
          u.familyName,
          u.jobTitle,
          u.createdBy,
          u.gender,
          u.homeAddress,
          u.workAddress,
          u.contacts,
          u.suspended,
          u.changePasswordAtNextLogin,
          Nil)

      }) ++ Set(amadouEpsilon, amadouGmail)
    }

    log.info("Deleting users . . .")
    userService.removeUsers(users.map(_.id.get).seq.take(75).toSet)

    log.info("Suspending users . . .")

    users.seq.take(25) foreach {
      user =>

        db.withTransaction {
          implicit session => Users filter (_.id === user.id) map (_.suspended) update (true)
        }
    }

    users.seq.drop(75).take(250) foreach {
      user =>

        db.withTransaction {
          implicit session => Users filter (_.id === user.id) map (_.suspended) update (true)
        }
    }

    log.info("Creating labels . . .")
    val labels = rndLabels flatMap { label =>
      labelService.findOrNew(label.name, Some(label.color))
    }

    val labelledUsers = users.map(_.id.get).seq.drop(300).take(128)

    labelledUsers foreach { userId =>
      db.withTransaction {
        implicit session => labels foreach (label => UsersLabels += UserLabel(userId, label.name))
      }
    }

    /*

    log.info("Creating apps . . .")
    val archives = appService.addApp("archives", List("archives.scope.0", "archives.scope.1", "archives.scope.2"))
    val orphans = appService.addApp("orphans", List("orphans.scope.0", "orphans.scope.1", "orphans.scope.2"))
    val schools = appService.addApp("schools", List("schools.scope.0", "schools.scope.1", "schools.scope.2"))

    val apps = List(
       archives,  orphans, schools
    )

    log.info("Creating access rights . . .")
    val archivesRights =
      db.withTransaction {
        implicit session =>
          List(

            AccessRights insert ("archives.readonly", archives.id.get.toString, List(
              domain.Scope("archives.scope.0", write = false, del = false),
              domain.Scope("archives.scope.1", write = false, del = false),
              domain.Scope("archives.scope.2", write = false, del = false),
              domain.Scope("stats.archives"),
              domain.Scope("settings.archives", write = false, del = false))),

            AccessRights insert ("archives", archives.id.get.toString, List(
              domain.Scope("archives.scope.0"),
              domain.Scope("archives.scope.1"),
              domain.Scope("archives.scope.2"),
              domain.Scope("stats.archives"),
              domain.Scope("settings.archives")))
          )
      }

    val orphansRights =
      db.withTransaction {
        implicit session =>

          List(

            AccessRights insert ("orphans.readonly", archives.id.get.toString, List(
              domain.Scope("orphans.scope.0", write = false, del = false),
              domain.Scope("orphans.scope.1", write = false, del = false),
              domain.Scope("orphans.scope.2", write = false, del = false),
              domain.Scope("stats.orphans"),
              domain.Scope("settings.orphans", write = false, del = false))),

            AccessRights insert ("orphans", archives.id.get.toString, List(
              domain.Scope("orphans.scope.0"),
              domain.Scope("orphans.scope.1"),
              domain.Scope("orphans.scope.2"),
              domain.Scope("stats.orphans"),
              domain.Scope("settings.orphans")))
          )
      }

    val schoolsRights =
      db.withTransaction {
        implicit session =>

          List(

            AccessRights insert ("schools.readonly", archives.id.get.toString, List(
              domain.Scope("schools.scope.0", write = false, del = false),
              domain.Scope("schools.scope.1", write = false, del = false),
              domain.Scope("schools.scope.2", write = false, del = false),
              domain.Scope("stats.schools"),
              domain.Scope("settings.schools", write = false, del = false))),

            AccessRights insert ("schools", archives.id.get.toString, List(
              domain.Scope("schools.scope.0"),
              domain.Scope("schools.scope.1"),
              domain.Scope("schools.scope.2"),
              domain.Scope("stats.schools"),
              domain.Scope("settings.schools")))
          )
      }

     val allRights = archivesRights ::: orphansRights ::: schoolsRights ::: Nil

     val accessRightUsers = users.map(_.id.get).seq.drop(75).take(50)

     val userAccessRights = ((accessRightUsers flatMap {
       userId => archivesRights map(right => UserAccessRight(userId, right.id.get))
     }) :: (accessRightUsers flatMap {
       userId => orphansRights map(right => UserAccessRight(userId, right.id.get))
     }) :: (accessRightUsers flatMap {
       userId => schoolsRights map(right => UserAccessRight(userId, right.id.get))
     }) :: Nil) flatten

     db.withTransaction{
       implicit session => UsersAccessRights ++= userAccessRights
     } */

    Cache.clearAll

    () => {

      /* userAccessRights foreach { userAccessRight =>
        db.withTransaction {
          implicit session =>
            UsersAccessRights
              .filter(o => (o.userId === userAccessRight.userId) && (o.accessRightId === userAccessRight.accessRightId))
              .delete
        }
      }

      allRights foreach { accessRight =>
        db.withTransaction {
          implicit session =>
            AccessRights delete accessRight.id.get.toString
        }
      }

      apps foreach{ app =>
        db.withTransaction{
          implicit session =>
            appService removeApp app.id.get.toString
        }
      }*/

      users foreach (u => userService.purgeUsers(Set(u.id.get)))

      withTransaction { implicit s =>
        labelService.remove(labels.map(_.name).seq.toSet)
      }

      Cache.clearAll
    }
  }
}

trait MailingComponentImpl extends MailingComponent {
  this: AkkaSystemProvider =>

  object MockMailer extends MailerImpl {
    override def sendPasswordResetEmail(username: String, key: String) {}
    override def sendPasswordChangedNotice(username: String) {}
    override def sendWelcomeEmail(username: String, password: String) {}
  }

  class MailerImpl extends Mailer {

    private[this] val log = Logger("http.MailerImpl")

    def sendPasswordResetEmail(username: String, key: String) {
      val subj = "[Schola] Password reset request"

      val msg = s"""
        | Someone requested that the password be reset for the following account:\r\n\r\n
        | Username: $username \r\n\r\n
        | If this was a mistake, just ignore this email and nothing will happen. \r\n\r\n
        | To reset your password, visit the following address:\r\n\r\n
        | < http://localhost/RstPasswd?key=$key&login=${java.net.URLEncoder.encode(username, "UTF-8")} >\r\n""".stripMargin

      sendEmail(subj, username, (Some(msg), None))
    }

    def sendPasswordChangedNotice(username: String) {
      val subj = "[Schola] Password change notice"

      val msg = s"""
        | Someone just changed the password for the following account:\r\n\r\n
        | Username: $username \r\n\r\n
        | If this was you, congratulation! the change was successfull. \r\n\r\n
        | Otherwise, contact your administrator immediately.\r\n""".stripMargin

      sendEmail(subj, username, (Some(msg), None))
    }

    def sendWelcomeEmail(username: String, password: String) {
      val subj = "[Schola] Welcome to Schola!"

      val msg = s"""
        | Congratulation, your account was successfully created.\r\n\r\n
        | Here are the details:\r\n\r\n
        | Username: $username \r\n\r\n
        | Password: $password \r\n\r\n
        | Sign in immediately at < http://localhost/Login > to reset your password and start using the service.\r\n\r\n
        | Thank you.\r\n""".stripMargin

      sendEmail(subj, username, (Some(msg), None))
    }

    lazy val FromAddress = implicitly[Application].configuration.getString("smtp.from").getOrElse(throw new RuntimeException("From addres is required."))

    private lazy val mailerRepo = use[com.typesafe.plugin.MailerPlugin].email

    private def sendEmail(subject: String, recipient: String, body: (Option[String], Option[String])) {
      import scala.concurrent.duration._

      if (log.isDebugEnabled) {
        log.debug("[Schola] sending email to %s".format(recipient))
        log.debug("[Schola] mail = [%s]".format(body))
      }

      system.scheduler.scheduleOnce(1000 microseconds) {

        mailerRepo.setSubject(subject)
        mailerRepo.setRecipient(recipient)
        mailerRepo.setFrom(FromAddress)

        mailerRepo.setReplyTo(FromAddress)

        // the mailer plugin handles null / empty string gracefully
        mailerRepo.send(body._1 getOrElse "", body._2 getOrElse "")
      }
    }
  }
}

/*

// import ma.epsilon.schola._, schema._, Q._, domain._, ma.epsilon.schola.http._
// import ma.epsilon.schola.domain._, ma.epsilon.schola.school.domain._, ma.epsilon.schola.school.schema._, ma.epsilon.schola.jdbc.Q._, ma.epsilon.schola.http._
import ma.epsilon.schola._, schema._, ma.epsilon.schola.domain._, school.schema._, school.domain._, ma.epsilon.schola.jdbc.Q._, ma.epsilon.schola.http._
import java.time._

//import com.mchange.v2.c3p0.ComboPooledDataSource
import com.jolbox.bonecp.BoneCPDataSource

implicit val app: play.api.Application = null

object d extends DefaultFaçade(app) { 
  // override lazy val db = ma.epsilon.schola.jdbc.Q.Database.forDataSource(new ComboPooledDataSource)
  override lazy val db = ma.epsilon.schola.jdbc.Q.Database.forDataSource(new BoneCPDataSource { setDriverClass("org.postgresql.Driver") })
  override implicit lazy val system = akka.actor.ActorSystem()
  override lazy val mailer  = MockMailer.asInstanceOf[MailerImpl]
}

import d._
import schoolService._

// EXCLUDE USING gist (cast(org as text) WITH =, during WITH &&)

// val ddl = _root_.ma.epsilon.schola.schema.Users.ddl ++ OAuthTokens.ddl ++ Apps.ddl ++ AccessRights.ddl ++ UsersAccessRights.ddl ++ Labels.ddl ++ UsersLabels.ddl // ++ OAuthClients.ddl
val ddl = Universities.ddl ++ Orgs.ddl ++ Depts.ddl ++ Courses.ddl ++ OrgCourses.ddl ++ OrgSubjects.ddl ++ Employees.ddl ++ OrgEmployments.ddl ++ Guardians.ddl ++ Students.ddl ++ Admissions.ddl ++ Inscriptions.ddl ++ ControlCategories.ddl ++ Controls.ddl ++ Marks.ddl ++ Timetables.ddl ++ Attendances.ddl ++ TeachingHistories.ddl ++ OrgSettings.ddl ++ OrgCompositions.ddl ++ Batches.ddl ++ OrgCoursesModules.ddl ++ Modules.ddl ++ Compaigns.ddl

println(ddl.createStatements.map(s=>"(T[Int] + " + " \"\"\" " + s + " \"\"\").execute\n").mkString.replaceAll("\"id\" uuid NOT NULL", "\"id\" uuid NOT NULL DEFAULT uuid_generate_v4()").replaceAll("SERIAL", "BIGSERIAL"))

println(ddl.dropStatements.map(s=>"(T[Int] + " + " \"\"\" " + s + " \"\"\").execute\n").map(s=>s.replace(" table", " table if exists")).mkString)

drop
init(U.SuperUser.id.get)
genFixtures(d.system)
Cache.clearAll

/*

LocalDateTime ldt = ...;
Instant instant = ldt.atZone(ZoneId.systemDefault()).toInstant();
Date res = Date.from(instant);

## Convert java.time.LocalDate to java.util.Date
LocalDate ld = ...;Instant instant = ld.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant();
Date res = Date.from(instant);


*/

*/ 