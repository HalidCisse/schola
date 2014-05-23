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

object PagingFilter extends Filter {
  import ma.epsilon.schola.jdbc.{ page, pageSize }
  import scala.util.Try

  def apply(nextFilter: RequestHeader => Future[Result])(request: RequestHeader): Future[Result] = {

    val _page = request.getQueryString("page").flatMap(str => Try(str.toInt).toOption).getOrElse(0)

    page.withValue(_page) {

      request.getQueryString("pageSize").flatMap(str => Try(str.toInt).toOption) match {

        case Some(_pageSize) =>

          pageSize.withValue(_pageSize) {
            nextFilter(request)
          }

        case _ => nextFilter(request)
      }
    }
  }
}