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

  val server = Http(3000)

  server
    .context("/") {
      _.filter(Plans / (server.host, server.port))
    }
    .context("/assets") {
    _.resources(getClass.getResource("/static/assets"))
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

//  S.test()

//  try S.drop() catch {
//    case _: Throwable =>
//  }

//  S.init(SuperUser.id.get)
  server.start()

  log.info("The server is runing . . .")
  log.info("Press any key to stop server . . .")
  System.in.read()

  Cache.clearAll()

  system.shutdown()
  system.awaitTermination()

  server.stop()
  server.destroy()

//  S.drop()
}