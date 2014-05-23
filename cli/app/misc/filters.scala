package misc

import play.api._
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.concurrent.duration.Deadline

object LoggingFilter extends EssentialFilter {

  def apply(nextFilter: EssentialAction) = new EssentialAction {

    def apply(requestHeader: RequestHeader) = {

      val startTime = Deadline.now

      val action = requestHeader.tags(Routes.ROUTE_CONTROLLER) + "." + requestHeader.tags(Routes.ROUTE_ACTION_METHOD)

      nextFilter(requestHeader).map { result =>

        val endTime = Deadline.now
        val requestTime = endTime - startTime

        Logger.info(
          s"${action}[${requestHeader.method} ${requestHeader.uri}] took ${requestTime}ms and returned ${result.header.status}")

        result.withHeaders("Request-Time" -> requestTime.toString)
      }
    }
  }
}