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

  object ResourceOwner {

    import javax.servlet.http.HttpServletRequest
    import unfiltered.request._

    def unapply[T <: HttpServletRequest](request: HttpRequest[T]): Option[unfiltered.oauth2.ResourceOwner] =
      request.underlying.getAttribute(unfiltered.oauth2.OAuth2.XAuthorizedIdentity) match {
        case sId: String => Some(new unfiltered.oauth2.ResourceOwner { val id = sId; val password = None })
        case _ => None
      }
  }

  case class ValidatePasswd(next: unfiltered.filter.Plan) extends unfiltered.filter.Plan{
    import unfiltered.request.&

    val intent : unfiltered.filter.Plan.Intent = {
      case ResourceOwner(resourceOwner) & oauth2.TokenA(token) & req =>

        Façade.oauthService.getUser(resourceOwner.id) match {
          case Some(user) =>

            if(user.passwordValid) next.intent(req)
            else Scalate(
                    req,
                    "changepasswd.jade",
                    Seq(
                      "key" -> token.accessToken,
                      "secret" -> token.macKey,
                      "issuedTime" -> token.createdAt.toString,
                      "email" -> user.email) map{ case (key, value) => key -> java.net.URLEncoder.encode(value, "utf-8") } : _*
                   )

          case _  => unfiltered.response.BadRequest
        }
    }
  }

  trait UpdateSpec[T] {
    def set: Option[Option[T]]

    def foreach(f: Option[T] => Boolean) = set map f getOrElse true
  }

  trait SetSpec[T] {

    def toRem: Set[T]

    def toAdd: Set[T]

    def diff(os: Set[T]): Set[T] = os ++ toAdd -- toRem
  }

  trait UserSpec {

    case class ContactInfoSpec(toRem: Set[ContactInfo] = Set(), toAdd: Set[ContactInfo] = Set()) extends SetSpec[domain.ContactInfo]

    def contacts: Option[ContactInfoSpec]

    def homeAddress: UpdateSpec[domain.AddressInfo]

    def workAddress: UpdateSpec[domain.AddressInfo]

    def email: Option[String]

    def password: Option[String]

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

    val avatar = UpdateSpecImpl[(domain.AvatarInfo, Array[Byte])]()
  }

  class Avatars extends akka.actor.Actor with akka.actor.ActorLogging {
    lazy val mongo = new com.mongodb.Mongo("localhost", 27017)
    lazy val db = mongo.getDB("schola")

    lazy val gfsPhoto = new com.mongodb.gridfs.GridFS(db, "users_avatars")

    def uploadAvatar(id: String, contentType: String, data: Array[Byte]) = {
      log.debug(s"saving $id")
      val gfsFile = gfsPhoto.createFile(data)

      gfsFile.setFilename(id)
      gfsFile.setContentType(contentType)

      try gfsFile.save()
      catch {
        case scala.util.control.NonFatal(e) => log.debug("save() failed")
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

  import org.fusesource.scalate.{
  TemplateEngine, Binding, DefaultRenderContext, RenderContext}
  import unfiltered.request.{Path,HttpRequest}
  import unfiltered.response.ResponseWriter
  import java.io.{File,OutputStreamWriter,PrintWriter}

  object Scalate {
    /** Constructs a ResponseWriter for Scalate templates.
      *  Note that any parameter in the second, implicit set
      *  can be overriden by specifying an implicit value of the
      *  expected type in a pariticular scope. */
    def apply[A, B](request: HttpRequest[A],
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
          val context = contextBuilder(Path(request), printWriter, engine)
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
      new File("src/main/resources/templates") :: Nil
    private val defaultEngine = new TemplateEngine(defaultTemplateDirs)
    private val defaultRenderContext: ToRenderContext =
      (path, writer, engine) =>
        new DefaultRenderContext(path, engine, writer)
  }
}