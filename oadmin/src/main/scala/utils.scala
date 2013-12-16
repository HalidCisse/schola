package schola
package oadmin

import domain.{ContactInfo, AddressInfo}
import akka.actor.Actor

package object utils {

  def genPasswd(key: String) = passwords.crypt(key)

  def randomString(size: Int) = {
    val random = java.security.SecureRandom.getInstance("SHA1PRNG")
    val b = new Array[Byte](size)
    random.nextBytes(b)
    org.bouncycastle.util.encoders.Hex.toHexString(b)
  }

//  val generateNonce = {
//    import java.security.SecureRandom
//    import org.bouncycastle.util.encoders.Hex
//
//    val b = new Array[Byte](8)
//    val random = SecureRandom.getInstance("SHA1PRNG")
//
//    (size: Int) => b.synchronized{
//      random.nextBytes(b)
//      Hex.toHexString(b)
//    }
//  }

  object ResourceOwner {

    import javax.servlet.http.HttpServletRequest
    import unfiltered.request._

    def unapply[T <: HttpServletRequest](request: HttpRequest[T]): Option[unfiltered.oauth2.ResourceOwner] =
      request.underlying.getAttribute(unfiltered.oauth2.OAuth2.XAuthorizedIdentity) match {
        case sId: String => Some(new unfiltered.oauth2.ResourceOwner { val id = sId; val password = None })
        case _ => None
      }
  }

  trait UpdateSpec[T] {
    def set: Option[Option[T]]

    def foreach(f: Option[T] => Boolean) = set map f getOrElse true
  }

  trait SetSpec[T] {

    def toRem: Set[T]

    def toAdd: Set[T]

    final def diff(os: Set[T]): Set[T] = os ++ toAdd -- toRem
  }

  trait UserSpec {

    case class ContactInfoSpec(toRem: Set[ContactInfo] = Set(), toAdd: Set[ContactInfo] = Set()) extends SetSpec[domain.ContactInfo]

    def contacts: Option[ContactInfoSpec]

    def homeAddress: UpdateSpec[domain.AddressInfo]

    def workAddress: UpdateSpec[domain.AddressInfo]

    def email: Option[String]

    def password: Option[String] // Though this is an Option, its required!

    def oldPassword: Option[String]

    def firstname: Option[String]

    def lastname: Option[String]

    def gender: Option[domain.Gender.Value]

    def avatar: UpdateSpec[(domain.AvatarInfo, Array[Byte])]
  }

  class DefaultUserSpec extends UserSpec {

    case class UpdateSpecImpl[T : scala.reflect.ClassTag](set: Option[Option[T]] = None) extends UpdateSpec[T]

    val contacts: Option[ContactInfoSpec] = None

    val homeAddress = UpdateSpecImpl[AddressInfo]()

    val workAddress = UpdateSpecImpl[AddressInfo]()

    val email: Option[String] = None

    val password: Option[String] = None

    val oldPassword: Option[String] = None

    val firstname: Option[String] = None

    val lastname: Option[String] = None

    val gender: Option[domain.Gender.Value] = None

    val avatar: UpdateSpec[(domain.AvatarInfo, Array[Byte])] = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])]()
  }

  class Avatars extends akka.actor.Actor with akka.actor.ActorLogging {
    lazy val mongo = new com.mongodb.Mongo(MongoDB.Hostname, MongoDB.Port)
    lazy val db = mongo.getDB(MongoDB.DatabaseName)

    lazy val gfsPhoto = new com.mongodb.gridfs.GridFS(db, MongoDB.CollectionName)

    def uploadAvatar(id: String, contentType: String, data: Array[Byte]) = {
      log.debug(s"uploading $id")
      val gfsFile = gfsPhoto.createFile(data)

      gfsFile.setFilename(id)
      gfsFile.setContentType(contentType)

      try gfsFile.save()
      catch {
        case e: Throwable => log.debug("save() failed: " + e.getMessage)
      }
    }

    def purgeAvatar(id: String) = gfsPhoto.remove(gfsPhoto.findOne(id))

    def receive: Actor.Receive = {
      case Avatars.Add(id, info, data) =>
        uploadAvatar(id, info.contentType, data)

      case Avatars.Get(id) =>

        log.debug(s"get $id")

        Option(gfsPhoto.findOne(id)) map { f =>

          val in = f.getInputStream
          val bos = new java.io.ByteArrayOutputStream
          val ba = new Array[Byte](4096)

          @scala.annotation.tailrec
          def read() {
            val len = in.read(ba)
            if (len > 0) bos.write(ba, 0, len)
            if (len >= 0) read()
          }
          read()
          in.close()

          sender ! (domain.AvatarInfo(f.getContentType), bos.toByteArray)
        }

      case Avatars.Purge(id) =>
        log.debug(s"purge $id")
        purgeAvatar(id)
    }
  }

  object Avatars {
    case class Add(id: String, info: domain.AvatarInfo, data: Array[Byte])
    case class Get(id: String)
    case class Purge(id: String)
  }

  // ---------------------------------------------------------------------------------------------------------------

  import unfiltered.oauth2._

  /** Configured Authorization server module */
  case class OAuthorization(auth: unfiltered.oauth2.AuthorizationServer) extends Authorized
  with DefaultAuthorizationPaths with DefaultValidationMessages {

    override def onPassword(
                    userName: String, password: String,
                    clientId: String, clientSecret: String, scope: Seq[String]) =
      auth(PasswordRequest(
        userName, password, clientId, clientSecret, scope)) match {

        case AccessTokenResponse(
        accessToken, tokenType, expiresIn, refreshToken, aScope, _, extras) =>
          accessResponder(
            accessToken, tokenType, expiresIn, refreshToken, aScope, extras
          )

        case ErrorResponse("changepasswd", id, _, _) =>

            utils.Scalate(
              TokenPath,
              "changepasswd.jade",
              "id" -> java.net.URLEncoder.encode(id, "utf-8")
            )


        case ErrorResponse(error, desc, euri, state) =>
          errorResponder(error, desc, euri, state)
      }
  }

  // ---------------------------------------------------------------------------------------------------------------

  import org.fusesource.scalate.{
  TemplateEngine, Binding, DefaultRenderContext, RenderContext}
  import unfiltered.request.{Path,HttpRequest}
  import unfiltered.response.ResponseWriter
  import java.io.{File,OutputStreamWriter,PrintWriter}

  object Scalate {

    def apply[A, B](request: HttpRequest[A],
                    template: String,
                    attributes:(String,Any)*)
                   ( implicit
                     engine: TemplateEngine = defaultEngine,
                     contextBuilder: ToRenderContext = defaultRenderContext,
                     bindings: List[Binding] = Nil,
                     additionalAttributes: Seq[(String, Any)] = Nil
                     ): ResponseWriter = apply(Path(request), template, attributes : _*)

    /** Constructs a ResponseWriter for Scalate templates.
      *  Note that any parameter in the second, implicit set
      *  can be overriden by specifying an implicit value of the
      *  expected type in a pariticular scope. */
    def apply[A, B](path: String,
                    template: String,
                    attributes:(String,Any)*)
                   ( implicit
                     engine: TemplateEngine = defaultEngine,
                     contextBuilder: ToRenderContext = defaultRenderContext,
                     bindings: List[Binding] = Nil,
                     additionalAttributes: Seq[(String, Any)] = Nil
                     ) = new ResponseWriter {
      def write(writer: OutputStreamWriter) {
        val printWriter = new PrintWriter(writer)
        try {
          val scalateTemplate = engine.load(template, bindings)
          val context = contextBuilder(path, printWriter, engine)
          (additionalAttributes ++ attributes) foreach {
            case (k,v) => context.attributes(k) = v
          }
          engine.layout(scalateTemplate, context)
        } catch {
          case e if engine.isDevelopmentMode =>
            printWriter.println("Exception: " + e.getMessage)
            e.getStackTrace.foreach(printWriter.println)
          case scala.util.control.NonFatal(e) => throw e
        }
      }
    }

    /* Function to construct a RenderContext. */
    type ToRenderContext =
    (String, PrintWriter, TemplateEngine) => RenderContext

    private val defaultTemplateDirs =
      new File(config.getString("jade.template-dir")) :: Nil
    private val defaultEngine = new TemplateEngine(defaultTemplateDirs)
    private val defaultRenderContext: ToRenderContext =
      (path, writer, engine) =>
        new DefaultRenderContext(path, engine, writer)
  }
}