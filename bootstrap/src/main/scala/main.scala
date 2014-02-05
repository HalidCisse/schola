package schola
package oadmin

object main extends App {

  import unfiltered.jetty.Http
  import unfiltered.response.Pass
  import unfiltered.oauth2.OAuthorization

  import oauth2._

  import unfiltered.request.&

  private val log = Logger("oadmin.main")

  val server = Http(Port, Hostname)

  implicit val system = akka.actor.ActorSystem("ScholaActorSystem")

  object façade extends FaçadeImpl

  object plans extends Plans with Server {
    val f = façade
  }

  server
    .context("/oauth") {
      _.filter(unfiltered.filter.Planify {
        case unfiltered.request.UserAgent(uA) & req =>
          OAuthorization(façade.newProvider(uA).auth).intent.lift(req).getOrElse(Pass)
      })
    }
    .context(s"/api/$API_VERSION") {
      _.filter(plans.session)
        .filter(plans.password)
        .filter(façade.newProtection())
        .filter(plans.api)
    }

  server.underlying.setAttribute("org.eclipse.jetty.server.Request.maxFormContentSize", MaxUploadSize * 1024 * 1024)

  /*import façade.simple._

  try drop() catch {
    case ex: Throwable => log.info("DROP failed:"); ex.printStackTrace()
  }

  init(domain.U.SuperUser.id.get)

  val cleanUp = genFixtures*/

  server.run({ s =>
    log.info(s"The server is runing at ${s.url} . . .")
  }, { s =>

    Cache.clearAll()

    //  cleanUp()

    system.shutdown()
    system.awaitTermination()
  })
}