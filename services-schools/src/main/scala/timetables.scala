package ma.epsilon.schola
package school

import _root_.ma.epsilon.schola.domain._, school.domain._

trait TimetableServicesComponent {

  trait Timetables {

    /*    def fetchOrgEvents(
      org: String, 
      startDate: java.time.LocalDate, 
      endDate: Option[java.time.LocalDate]): List[EventInfo]    

    def findOrUpdateOrNewScheduledEvent(event: ???.type)
*/
    /*
     *  Creates a timetable entry, adds a timetable event entry if dayOfWeek is today
    */
    /*    def addOrUpdateScheduledEvents(
      batch: Uuid, events: List[???.type]): TimetableEvent // new event*/

    /*
     *  Gets a timetable event entry; if it's a lecture or break and it's not expired, creates or refreshes an associated timetable events for `date`
    */
    /*    def getScheduledEvent(
      date: java.time.LocalDate,
      org: Uuid,
      batch: Uuid,
      subject: Uuid,
      `type`: TimetableEventType,
      date: java.time.LocalDate,
      startTime: java.time.LocalTime,
      endTime: java.time.LocalTime,
      createdBy: Option[String]): Option[TimetableEvent]

    def getStudentScheduledEvents(
      id: String,
      date: java.time.LocalDate,
      org: Uuid,
      batch: Uuid): List[TimetableEvent]        

    def getEmployeeScheduledEvents(
      id: String,
      date: java.time.LocalDate,
      org: Uuid,
      batch: Uuid): List[TimetableEvent]    */
  }

}