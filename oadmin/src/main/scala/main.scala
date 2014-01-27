package schola
package oadmin

import org.clapper.avsl.Logger

object main extends App {

  import unfiltered.jetty._
  import unfiltered.response.Pass
  import unfiltered.oauth2.OAuthorization

  import oauth2._

  import unfiltered.request.&

  val log = Logger("oadmin.main")

  val server = Http(Port, Hostname)

  server
    .context("/") {
      _.filter(Plans /)
    }
    .context("/assets") {
      _.resources(getClass.getResource("/WEB-INF/assets"))
    }
    .context("/oauth") {
      _.filter(unfiltered.filter.Planify {
        case unfiltered.request.UserAgent(uA) & req =>
          OAuthorization(new AuthServerProvider(uA).auth).intent.lift(req).getOrElse(Pass)
      })
    }
    .context(s"/api/$API_VERSION") {
      _.filter(OAuth2Protection(new OAdminAuthSource))
        .filter(Plans.api)
    }

  try S.drop() catch {
    case ex: Throwable => log.info("DROP failed:"); ex.printStackTrace()
  }

  S.init(SuperUser.id.get)
  server.start()

  val cleanUp = S.genFixtures

  log.info("The server is runing . . .")
  log.info("Press any key to stop server . . .")
  System.in.read()

  Cache.clearAll()

  //  cleanUp()

  system.shutdown()
  system.awaitTermination()

  server.stop()
  server.destroy()
}