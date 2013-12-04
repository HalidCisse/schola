package schola
package oadmin

package object plans {

  import oauth2._
  import unfiltered.request.{Path => UFPath, _}
  import unfiltered.response._

  private object ResourceOwner {

    import javax.servlet.http.HttpServletRequest

    def unapply[T <: HttpServletRequest](request: HttpRequest[T]): Option[unfiltered.oauth2.ResourceOwner] =
      request.underlying.getAttribute(unfiltered.oauth2.OAuth2.XAuthorizedIdentity) match {
        case sId: String => Some(new unfiltered.oauth2.ResourceOwner { val id = sId; val password = None })
        case _ => None
      }
  }

  val routes = unfiltered.filter.Planify {
    case ResourceOwner(user) & req =>

      type Intent = unfiltered.filter.Plan.Intent

      val users: Intent = {

        case POST(UFPath(Seg("api" :: "users" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "users" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "session" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "user" :: userId :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case PUT(UFPath(Seg("api" :: "user" :: userId :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case DELETE(UFPath(Seg("api" :: "user" :: userId :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "user" :: userId :: "exists" :: Nil))) =>

          ResponseString(s"user: ${user.id}")
      }

      val roles: Intent = {
        case POST(UFPath(Seg("api" :: "roles" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case POST(UFPath(Seg("api" :: "permissions" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "roles" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "role" :: name :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case PUT(UFPath(Seg("api" :: "role" :: name :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case DELETE(UFPath(Seg("api" :: "role" :: name :: Nil))) =>

          ResponseString(s"user: ${user.id}")
      }

      val auth: Intent = {

        case GET(UFPath(Seg("api" :: "login" :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case UFPath(Seg("api" :: "logout" :: Nil)) & Token(token) =>

          façade.oauthService.revokeToken(token)
          ResponseString("Logout success")
      }

      val accesscontrol: Intent = {

        case POST(UFPath(Seg("api" :: "roles" :: userId :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "permissions" :: roleName :: Nil))) =>

          ResponseString(s"user: ${user.id}")

        case GET(UFPath(Seg("api" :: "accesscontrol" :: userId :: Nil))) =>

          ResponseString(s"user: ${user.id}")
      }

      val app = users orElse roles orElse auth orElse accesscontrol
      app(req)
  }
}