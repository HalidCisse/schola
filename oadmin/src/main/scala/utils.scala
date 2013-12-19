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

  val xxHash = {
    import net.jpountz.xxhash.XXHashFactory

    val f = XXHashFactory.nativeInstance().hash32

    (bytes: Array[Byte]) => {
      val x = f.hash(bytes, 0, bytes.length, 0xCAFEBABE)
      Integer.toHexString(x)
    }
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

    case class UpdateSpecImpl[T: scala.reflect.ClassTag](set: Option[Option[T]] = None) extends UpdateSpec[T]

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

  class AvatarWorkers(gfsPhotos: com.mongodb.gridfs.GridFS) extends akka.actor.Actor with akka.actor.ActorLogging {

    def uploadAvatar(id: String, contentType: String, data: Array[Byte]) = {
      log.debug(s"uploading $id")
      val gfsFile = gfsPhotos.createFile(data)

      gfsFile.setFilename(id)
      gfsFile.setContentType(contentType)

      try gfsFile.save()
      catch {
        case scala.util.control.NonFatal(e) => log.debug("save() failed: " + e.getMessage)
      }
    }

    def purgeAvatar(id: String) = gfsPhotos.remove(gfsPhotos.findOne(id))

    def getAvatar(id: String) = {
      val f = gfsPhotos.findOne(id)

      if (f eq null)
        Façade.oauthService.getUser(id) map {
          user =>
            (domain.AvatarInfo("image/png"), if (user.gender eq domain.Gender.Male) DefaultAvatars.Male else DefaultAvatars.Female)
        }

      else {

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

        Some((domain.AvatarInfo(f.getContentType), com.owtelse.codec.Base64.encode(bos.toByteArray)))
      }
    }

    def receive = {
      case Avatars.Add(id, info, data) =>
        uploadAvatar(id, info.contentType, data)

      case Avatars.Get(id) =>

        import scala.concurrent.Future
        import akka.pattern._
        import impl.CacheActor._

        log.debug(s"get $id")

        Future {
          getAvatar(id)
        } pipeTo sender

      case Avatars.Purge(id) =>
        log.debug(s"purge $id")
        purgeAvatar(id)
    }
  }

  class Avatars extends akka.actor.Actor with akka.actor.ActorLogging {
    import akka.actor._
    import akka.routing._

    val gfsPhotos = {
      val mongo = new com.mongodb.Mongo(MongoDB.Hostname, MongoDB.Port)
      val db = mongo.getDB(MongoDB.DatabaseName)

      new com.mongodb.gridfs.GridFS(db, MongoDB.CollectionName)
    }

    val workerPool = context.actorOf(
      props = RandomPool(3).props(Props(classOf[AvatarWorkers], gfsPhotos)).withDeploy(Deploy.local),
      name = "Avatars_upload-workers")

    def receive = {
      case msg: Avatars.Msg =>
        workerPool tell(msg, sender)
    }
  }

  object Avatars {

    sealed trait Msg

    case class Add(id: String, info: domain.AvatarInfo, data: Array[Byte]) extends Msg

    case class Get(id: String) extends Msg

    case class Purge(id: String) extends Msg

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
  import unfiltered.request.{Path, HttpRequest}
  import unfiltered.response.ResponseWriter
  import java.io.{File, OutputStreamWriter, PrintWriter}

  object Scalate {

    def apply[A, B](request: HttpRequest[A],
                    template: String,
                    attributes: (String, Any)*)
                   (implicit
                    engine: TemplateEngine = defaultEngine,
                    contextBuilder: ToRenderContext = defaultRenderContext,
                    bindings: List[Binding] = Nil,
                    additionalAttributes: Seq[(String, Any)] = Nil
                     ): ResponseWriter = apply(Path(request), template, attributes: _*)

    /** Constructs a ResponseWriter for Scalate templates.
      * Note that any parameter in the second, implicit set
      * can be overriden by specifying an implicit value of the
      * expected type in a pariticular scope. */
    def apply[A, B](path: String,
                    template: String,
                    attributes: (String, Any)*)
                   (implicit
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
            case (k, v) => context.attributes(k) = v
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