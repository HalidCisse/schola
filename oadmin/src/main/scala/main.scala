package schola
package oadmin

import org.clapper.avsl.Logger

import akka.actor.{ Actor, DeadLetter, Props }

class Listener extends Actor {
  def receive = {
    case d: DeadLetter =>
      println(d)
  }
}


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
    _.filter(Plans /)
  }
    .context("/assets") {
    _.resources(getClass.getResource("/static/public"))
  }
    .context("/oauth") {
    _.filter(unfiltered.filter.Planify {
      case unfiltered.request.UserAgent(uA) & req =>
        OAuthorization(new AuthServerProvider(uA).auth).intent.lift(req).getOrElse(Pass)
    })
  }
    .context("/api/v1") {
    _.filter(OAuth2Protection(new OAdminAuthSource))
      .filter(Plans.routes)
  }

  val listener = system.actorOf(Props(classOf[Listener]))
  system.eventStream.subscribe(listener, classOf[DeadLetter])

//  Façade.simple.test()

//  try Façade.simple.drop() catch {
//    case _: Throwable =>
//  }

//  Façade.simple.init(SuperUser.id.get)
  server.start()

  log.info("The server is runing . . .")
  log.info("press any key to stop server...")
  System.in.read()

  Cache.clearAll()

  system.shutdown()
  system.awaitTermination()

  server.stop()
  server.destroy()

//  Façade.simple.drop()
}