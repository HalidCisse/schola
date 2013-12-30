package schola
package oadmin

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

    val f = XXHashFactory.fastestInstance().hash32

    (bytes: Array[Byte]) => {
      val x = f.hash(bytes, 0, bytes.length, 0xCAFEBABE)
      Integer.toHexString(x)
    }
  }

  def timeF(thunk: => Any) = {
    val start = System.currentTimeMillis
    thunk
    System.currentTimeMillis - start
  }

  // ---------------------------------------------------------------------------------------------------

  import org.json4s._

  def findFieldStr(json: JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _ => false
    } collect {
      case org.json4s.JField(_, org.json4s.JString(s)) => s
    }

  def findFieldJArr(json: JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _ => false
    } collect {
      case org.json4s.JField(_, a@org.json4s.JArray(_)) => a
    }

  def findFieldJObj(json: JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _ => false
    } collect {
      case org.json4s.JField(_, o@org.json4s.JObject(_)) => o
    }
}

package utils {
  import domain._

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

    case class UpdateSpecImpl[T: scala.reflect.ClassTag](set: Option[Option[T]] = None) extends UpdateSpec[T]

    case class MobileNumbersSpec(
      mobile1: UpdateSpecImpl[PhoneNumber] = UpdateSpecImpl[PhoneNumber](),
      mobile2: UpdateSpecImpl[PhoneNumber] = UpdateSpecImpl[PhoneNumber]())

    case class ContactsSpec(
      home: UpdateSpecImpl[HomeContactInfo] = UpdateSpecImpl[HomeContactInfo](),
      work: UpdateSpecImpl[WorkContactInfo] = UpdateSpecImpl[WorkContactInfo](),
      mobiles: MobileNumbersSpec = MobileNumbersSpec())

    def contacts: Option[ContactsSpec]

    def homeAddress: UpdateSpec[AddressInfo]

    def workAddress: UpdateSpec[AddressInfo]

    def primaryEmail: Option[String]

    def password: Option[String] // Though this is an Option, its required!

    def oldPassword: Option[String]

    def givenName: Option[String]

    def familyName: Option[String]

    def gender: Option[Gender.Value]

    def avatar: UpdateSpec[(AvatarInfo, Array[Byte])]
  }

  class DefaultUserSpec extends UserSpec {

    val contacts: Option[ContactsSpec] = None

    val homeAddress = UpdateSpecImpl[AddressInfo]()

    val workAddress = UpdateSpecImpl[AddressInfo]()

    val primaryEmail: Option[String] = None

    val password: Option[String] = None

    val oldPassword: Option[String] = None

    val givenName: Option[String] = None

    val familyName: Option[String] = None

    val gender: Option[Gender.Value] = None

    val avatar: UpdateSpec[(AvatarInfo, Array[Byte])] = UpdateSpecImpl[(AvatarInfo, Array[Byte])]()
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
        Façade.simple.oauthService.getUser(id) map {
          user =>
            (AvatarInfo("image/png"), if (user.gender eq Gender.Male) DefaultAvatars.M else DefaultAvatars.F)
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

        Some((AvatarInfo(f.getContentType), com.owtelse.codec.Base64.encode(bos.toByteArray)))
      }
    }

    def receive = {
      case Avatars.Add(id, info, data) =>
        uploadAvatar(id, info.mimeType, data)

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

  class Avatars(config: MongoDBSettings) extends akka.actor.Actor with akka.actor.ActorLogging {

    import akka.actor._
    import akka.routing._

    import config._

    val gfsPhotos = {
      val mongo = new com.mongodb.Mongo(Host, Port)
      val db = mongo.getDB(DatabaseName)

      new com.mongodb.gridfs.GridFS(db, CollectionName)
    }

    val workerPool = context.actorOf(
      props = RandomPool(3/* Workers */).props(Props(classOf[AvatarWorkers], gfsPhotos)/*.withDispatcher(Dispatcher)*/).withDeploy(Deploy.local), // Add Workers and BalancingDispatcher to config
      name = "Avatars_upload-workers")

    def receive = {
      case msg: Avatars.Msg =>
        workerPool tell(msg, sender)
    }
  }

  object Avatars {

    sealed trait Msg

    case class Add(id: String, info: AvatarInfo, data: Array[Byte]) extends Msg

    case class Get(id: String) extends Msg

    case class Purge(id: String) extends Msg

  }

  // ---------------------------------------------------------------------------------------------------------------

  import org.fusesource.scalate.{
  TemplateEngine, Binding, DefaultRenderContext, RenderContext}
  import unfiltered.request.{Path, HttpRequest}
  import unfiltered.response.ResponseWriter
  import java.io.{File, OutputStreamWriter, PrintWriter}

  object Scalate {

    import libs.Flash

    def flash[A](req: HttpRequest[A]) =
      unfiltered.request.Cookies(req).get(Flash.COOKIE_NAME).map(Flash.decodeFromCookie(_).data).getOrElse(Map[String, String]())

    def apply[A, B](request: HttpRequest[A],
                    template: String,
                    attributes: (String, Any)*)
                   (implicit
                    engine: TemplateEngine = defaultEngine,
                    contextBuilder: ToRenderContext = defaultRenderContext,
                    bindings: List[Binding] = Nil,
                    additionalAttributes: Seq[(String, Any)] = Nil
                     ): ResponseWriter = apply(Path(request), template, Seq("flash" -> flash(request)) ++ attributes: _*)

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
      new File(config.getString("templates.dir")) :: Nil
    private val defaultEngine = new TemplateEngine(defaultTemplateDirs)
    private val defaultRenderContext: ToRenderContext =
      (path, writer, engine) =>
        new DefaultRenderContext(path, engine, writer)
  }

  // -------------------------------------------------------------------------------------------------------------------------

  object Crypto {

    import java.security.SecureRandom

    import javax.crypto._
    import javax.crypto.spec.SecretKeySpec

    import org.bouncycastle.util.encoders.Hex

    val secret = config.getString("secret")

    private val random = new SecureRandom

    def sign(message: String, key: Array[Byte]): String = {
      val mac = Mac.getInstance("HmacSHA1")
      mac.init(new SecretKeySpec(key, "HmacSHA1"))
      Hex.toHexString(mac.doFinal(message.getBytes("utf-8")))
    }

    def sign(message: String): String =
      sign(message, secret.getBytes("utf-8"))

    val generateToken = {
      val bytes = new Array[Byte](12)

      () => bytes.synchronized {
        random.nextBytes(bytes)
        new String(Hex.toHexString(bytes))
      }
    }

    def generateSignedToken = signToken(generateToken())

    def signToken(token: String) = {
      val nonce = System.currentTimeMillis()
      val joined = nonce + "-" + token
      sign(joined) + "-" + joined
    }

    def extractSignedToken(token: String) = {
      token.split("-", 3) match {
        case Array(signature, nonce, raw) if constantTimeEquals(signature, sign(nonce + "-" + raw)) => Some(raw)
        case _ => None
      }
    }

    def constantTimeEquals(a: String, b: String) = {
      if (a.length != b.length) {
        false
      } else {
        var equal = 0
        for (i <- 0 until a.length) {
          equal |= a(i) ^ b(i)
        }
        equal == 0
      }
    }
  }
}