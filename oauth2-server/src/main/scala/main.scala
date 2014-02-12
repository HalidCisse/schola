package schola
package oadmin

package oauth2

import com.mchange.v2.c3p0.ComboPooledDataSource

object main extends App
    with OAuth2Component
    with impl.AccessControlServicesRepoComponentImpl
    with impl.AccessControlServicesComponentImpl
    with impl.UserServicesComponentImpl
    with impl.UserServicesRepoComponentImpl
    with impl.OAuthServicesRepoComponentImpl
    with impl.OAuthServicesComponentImpl
    with impl.LabelServicesRepoComponentImpl
    with impl.LabelServicesComponentImpl
    with impl.AvatarServicesRepoComponentImpl
    with impl.AvatarServicesComponentImpl {

  import unfiltered.jetty.Http
  import unfiltered.response.Pass
  import unfiltered.oauth2.OAuthorization

  import unfiltered.request.&

  import schema._
  import Q._

  private val log = Logger("oauth2.main")

  /* UNUSED */ lazy val avatarServices = ??? // new AvatarServicesImpl
  /* UNUSED */ protected lazy val avatarServicesRepo = ??? // new AvatarServicesRepoImpl
  /* UNUSED */ lazy val labelService = ??? // new LabelServicesImpl 
  /* UNUSED */ lazy val accessControlService = ??? // new AccessControlServicesImpl

  val oauthService = new OAuthServicesImpl

  val userService = new UserServicesImpl

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
    log.info(s"users in db ${userService.getUsersStats}")
    log.info(s"oauth2-server is runing at ${s.url} . . . ")

  }, { s =>

    system.shutdown()
    system.awaitTermination()
  })
}