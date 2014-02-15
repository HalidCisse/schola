package schola
package oadmin

package oauth2

import com.mchange.v2.c3p0.ComboPooledDataSource

object main extends App
    with OAuth2Component
    with impl.OAuthServicesRepoComponentImpl
    with impl.OAuthServicesComponentImpl
    with impl.AccessControlServicesRepoComponentImpl
    with impl.AccessControlServicesComponentImpl {

  /* UNUSED */ lazy val accessControlService = null

  import unfiltered.jetty.Http
  import unfiltered.response.Pass
  import unfiltered.oauth2.OAuthorization

  import unfiltered.request.&

  import schema._
  import Q._

  private val log = Logger("oauth2.main")

  val oauthService = new OAuthServicesImpl

  protected val db = Database.forDataSource(new ComboPooledDataSource)

  val server = Http(Port, Hostname)

  implicit val system = akka.actor.ActorSystem("ScholaActorSystem")

  server
    .context("/oauth") {
      _.filter(unfiltered.filter.Planify {
        case unfiltered.request.UserAgent(uA) & req =>
          OAuthorization(newProvider(uA).auth).intent.lift(req).getOrElse(Pass)
      })
    }

  server.run({ s =>
    log.info(s"oauth2-server is runing at ${s.url} . . . ")

  }, { s =>

    system.shutdown()
    system.awaitTermination()
  })
}