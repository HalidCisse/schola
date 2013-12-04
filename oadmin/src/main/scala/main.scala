package schola
package oadmin

object main extends App {
  import unfiltered.jetty._
  import unfiltered.oauth2._
  import schola.oadmin.oauth2._

  val server = Http(3000)

  val authProvider = new AuthServerProvider

  server
    .resources(getClass.getResource("/static/"))
    .context("/oauth") {
      _.filter(OAuthorization(authProvider.auth))
    }
    .context("/api") {
      _.filter(OAuth2Protection(new OAdminAuthSource))
        .filter(plans.routes)
    }

  try façade.drop() catch { case _: Throwable => }
  
  façade.init(SuperUser.id)
  server.start()

  println("server is runing...")
  println("press any key to stop server...")
  System.in.read()

  server.stop()
  server.destroy()
  façade.drop()
}