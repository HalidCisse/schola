package schola
package oadmin

import org.clapper.avsl.Logger

object main extends App {

  import unfiltered.jetty._
  import unfiltered.response.Pass

  import oauth2._

  import unfiltered.request.&

  val log = Logger("oadmin.main")

  val server = Http(3000)

  server
    .resources(getClass.getResource("/static/public"))
    .context("/oauth") {
      _.filter(unfiltered.filter.Planify{
        case unfiltered.request.UserAgent(uA) & req =>
          utils.OAuthorization(new AuthServerProvider(uA).auth).intent.lift(req).getOrElse(Pass)
      })
    }
    .context("/api/v1") {
      _.filter(OAuth2Protection(new OAdminAuthSource))
       .filter(Plans.routes)
    }

  try Façade.drop() catch { case _: Throwable => }

  Façade.init(SuperUser.id.get)
  server.start()

  log.info("server is runing . . .")
  log.info("press any key to stop server...")
  System.in.read()

  system.shutdown()
  system.awaitTermination()

  server.stop()
  server.destroy()

  Façade.drop()
}