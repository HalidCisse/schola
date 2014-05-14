package ma.epsilon.schola
package oauth2

// import com.mchange.v2.c3p0.ComboPooledDataSource
import com.jolbox.bonecp.BoneCPDataSource

object main extends App
    with OAuth2Component
    with impl.OAuthServicesRepoComponentImpl
    with impl.OAuthServicesComponentImpl {

  import unfiltered.jetty.Http
  import unfiltered.response.Pass
  import unfiltered.oauth2.OAuthorization

  import unfiltered.request.&

  import schema._
  import jdbc.Q._

  private val log = Logger("oauth2.main")

  val oauthService = new OAuthServicesImpl

  // protected val db = Database.forDataSource(new ComboPooledDataSource)
  protected val db = Database.forDataSource(new BoneCPDataSource)

  val server = Http(3000)

  implicit val system = akka.actor.ActorSystem("ScholaActorSystem")

  server
    .context("/oauth") {
      _.filter(unfiltered.filter.Planify {
        case unfiltered.request.UserAgent(uA) & req =>
          OAuthorization(newProvider(uA).auth).intent.lift(req).getOrElse(Pass)
      })
    }

  server.run({ s =>
    log.info(s"oauth2-server is running at ${s.url} . . . ")

  }, { s =>

    system.shutdown()
    system.awaitTermination()
  })
}