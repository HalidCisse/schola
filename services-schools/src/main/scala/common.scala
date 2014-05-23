package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait CommonSchoolServicesComponent {

  trait Common {

    def getCompaigns(org: Uuid): List[Compaign]
    def newCompaign(org: Uuid, duration: Range[java.time.Instant], moduleType: ModuleType): Compaign
    def updateCompaign(id: Uuid, newDuration: Range[java.time.Instant]): Boolean
    def delCompaign(id: Uuid)

    def getOrgCompositions(compaign: Uuid): List[OrgComposition]
    def newComposition(compaign: Uuid, name: String, duration: Range[java.time.Instant], coefficient: Option[Double]): OrgComposition
    def updateComposition(id: Uuid, spec: ScheduledSpec): Boolean
    def delComposition(id: Uuid)

    def getUniversities: List[University]
    def saveUniversity(name: String, website: Option[String], contacts: ContactInfo, address: AddressInfo): University
    def updateUniversity(id: Uuid, spec: UniversitySpec): Boolean
    def delUniversity(id: Uuid)

    def getOrgs: List[Org]
    def getTrashedOrgs: List[Org]
    def saveOrg(name: String, acronyms: Option[String], website: Option[String], university: Option[Uuid], contacts: ContactInfo, address: AddressInfo, createdBy: Option[Uuid]): Org
    def updateOrg(id: Uuid, spec: OrgSpec): Boolean
    def updateOrgSettings(org: Uuid, spec: OrgSettingSpec): Boolean
    def fetchOrgSettings(org: Uuid): OrgSetting
    def delOrg(id: Uuid)
    def purgeOrg(id: Uuid)

    def getDepts(org: Uuid): List[Dept]
    def saveDept(name: String, org: Uuid, departmentChefId: Option[Uuid]): Dept
    def updateDept(id: Uuid, spec: DeptSpec): Boolean
    def delDept(id: Uuid)
  }
}

trait CommonSchoolServicesRepoComponent {

  val commonServicesRepo: CommonServicesRepo

  trait CommonServicesRepo {

    def getCompaigns(org: Uuid): List[Compaign]
    def newCompaign(org: Uuid, duration: Range[java.time.Instant], moduleType: ModuleType): Compaign
    def updateCompaign(id: Uuid, newDuration: Range[java.time.Instant]): Boolean
    def delCompaign(id: Uuid)

    def getOrgCompositions(compaign: Uuid): List[OrgComposition]
    def newComposition(compaign: Uuid, name: String, duration: Range[java.time.Instant], coefficient: Option[Double]): OrgComposition
    def updateComposition(id: Uuid, spec: ScheduledSpec): Boolean
    def delComposition(id: Uuid)  

    def getUniversities: List[University]
    def saveUniversity(name: String, website: Option[String], contacts: ContactInfo, address: AddressInfo): University
    def updateUniversity(id: Uuid, spec: UniversitySpec): Boolean
    def delUniversity(id: Uuid)

    def getOrgs: List[Org]
    def getTrashedOrgs: List[Org]
    def saveOrg(name: String, acronyms: Option[String], website: Option[String], university: Option[Uuid], contacts: ContactInfo, address: AddressInfo, createdBy: Option[Uuid]): Org
    def updateOrg(id: Uuid, spec: OrgSpec): Boolean
    def updateOrgSettings(org: Uuid, spec: OrgSettingSpec): Boolean
    def fetchOrgSettings(org: Uuid): OrgSetting
    def delOrg(id: Uuid)
    def purgeOrg(id: Uuid)

    def getDepts(org: Uuid): List[Dept]
    def saveDept(name: String, org: Uuid, departmentChefId: Option[Uuid]): Dept
    def updateDept(id: Uuid, spec: DeptSpec): Boolean
    def delDept(id: Uuid)
  }
}
