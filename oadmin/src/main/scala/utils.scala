package schola
package oadmin
package utils

import domain._

object ResourceOwner {

  import javax.servlet.http.HttpServletRequest
  import unfiltered.request._

  def unapply[T <: HttpServletRequest](request: HttpRequest[T]): Option[unfiltered.oauth2.ResourceOwner] =
    request.underlying.getAttribute(unfiltered.oauth2.OAuth2.XAuthorizedIdentity) match {
      case sId: String => Some(new unfiltered.oauth2.ResourceOwner { val id = sId; val password = None })
      case _           => None
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
    mobile1: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    mobile2: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactInfoSpec[T](
    email: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    fax: UpdateSpecImpl[String] = UpdateSpecImpl[String](),
    phoneNumber: UpdateSpecImpl[String] = UpdateSpecImpl[String]())

  case class ContactsSpec(
    mobiles: MobileNumbersSpec = MobileNumbersSpec(),
    home: Option[ContactInfoSpec[HomeContactInfo]] = None,
    work: Option[ContactInfoSpec[WorkContactInfo]] = None)

  def contacts: Option[ContactsSpec]

  def homeAddress: UpdateSpec[AddressInfo]

  def workAddress: UpdateSpec[AddressInfo]

  def primaryEmail: Option[String]

  def password: Option[String] // Though this is an Option, its required!

  def oldPassword: Option[String]

  def givenName: Option[String]

  def familyName: Option[String]

  def gender: Option[Gender.Value]

  def avatar: UpdateSpec[String]
}

class DefaultUserSpec extends UserSpec {

  lazy val contacts: Option[ContactsSpec] = None

  lazy val homeAddress = UpdateSpecImpl[AddressInfo]()

  lazy val workAddress = UpdateSpecImpl[AddressInfo]()

  lazy val primaryEmail: Option[String] = None

  lazy val password: Option[String] = None

  lazy val oldPassword: Option[String] = None

  lazy val givenName: Option[String] = None

  lazy val familyName: Option[String] = None

  lazy val gender: Option[Gender.Value] = None

  lazy val avatar: UpdateSpec[String] = UpdateSpecImpl[String]()
}

class AvatarWorkers(helper: ReactiveMongoHelper) extends akka.actor.Actor with akka.actor.ActorLogging {
  import akka.actor._, SupervisorStrategy._
  import akka.pattern._
  import scala.concurrent.duration._

  import context.dispatcher

  import reactivemongo._, bson._, api._, gridfs._, Implicits.DefaultReadFileReader

  lazy val gridFS = {
    val gfs = GridFS(helper.db)

    // let's build an index on our gridfs chunks collection if none
    gfs.ensureIndex().onComplete {
      case index =>
        log.info(s"Checked index, result is $index")
    }

    gfs
  }

  def uploadAvatar(filename: String, contentType: Option[String], data: Array[Byte], listener: ActorRef) = {
    import play.api.libs.iteratee.Enumerator

    log.debug("uploading new avatar . . .")

    gridFS.save(Enumerator(data), DefaultFileToSave(filename, contentType))
      .map(_.id.asInstanceOf[BSONObjectID].stringify) pipeTo listener

  }

  def purgeAvatar(id: String, listener: ActorRef) = gridFS.remove(BSONObjectID(id)) map (_.ok) pipeTo listener

  def getAvatar(id: String, listener: ActorRef) = {
    import play.api.libs.iteratee.Iteratee

    val fs = gridFS.find(BSONDocument("_id" -> BSONObjectID(id)))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f =>
      val en = gridFS.enumerate(f)

      for {
        bytes <- en run Iteratee.consume[Array[Byte]]()
      } yield (f.contentType, bytes)
    } pipeTo listener

  }

  def receive = {
    case Avatars.Save(filename, contentType, fdata) =>

      uploadAvatar(filename, contentType, fdata, sender)

    case Avatars.Get(id) =>

      getAvatar(id, sender)

    case Avatars.Purge(id) =>

      purgeAvatar(id, sender)
  }
}

class Avatars(config: MongoDBSettings) extends akka.actor.Actor with akka.actor.ActorLogging {

  import akka.actor._
  import akka.routing._

  import config._

  private val helper: ReactiveMongoHelper = {

    log.info("ReactiveMongo starting...")

    val _helper = try {
      Some(ReactiveMongoHelper(Database, Seq(Host), Nil, None))
    } catch {
      case ex: Throwable =>
        throw new RuntimeException("ReactiveMongo Initialization Error: An exception occurred while initializing the ReactiveMongo.", ex)
    }

    _helper map { h =>
      log.info("ReactiveMongo successfully started with db '%s'! Servers:\n\t\t%s"
        .format(
          h.dbName,
          h.servers.map { s => "[%s]".format(s) }.mkString("\n\t\t")))
    }

    _helper

  } getOrElse (throw new RuntimeException("ReactiveMongo error: no ReactiveMongoHelper available?"))

  override def postStop() {
    import scala.concurrent._, duration._
    import context.dispatcher

    import helper._

    log.info("ReactiveMongo stops, closing connections...")

    val fq = connection.askClose()(10 seconds)

    fq.onComplete {
      case ex =>
        log.info("ReactiveMongo Connections stopped. [" + ex + "]")

        driver.close()

        log.info("ReactiveMongo Driver stopped.")
    }

    Await.ready(fq, 10 seconds)
  }

  val workerPool = context.actorOf(
    props = Props(new AvatarWorkers(helper)).withRouter(RandomRouter(4)).withDeploy(Deploy.local), // TODO: change when you upgrade to 2.3
    name = "Avatars_upload-workers")

  def receive = {
    case msg: Avatars.Msg =>
      workerPool tell (msg, sender)
  }
}

object Avatars {

  sealed trait Msg

  case class Save(filename: String, contentType: Option[String], fdata: Array[Byte]) extends Msg

  case class Get(id: String) extends Msg

  case class Purge(id: String) extends Msg

}

private[utils] case class ReactiveMongoHelper(dbName: String, servers: Seq[String], auth: Seq[reactivemongo.core.nodeset.Authenticate], nbChannelsPerNode: Option[Int]) {
  import reactivemongo.api._

  lazy val driver = MongoDriver(system)

  lazy val connection = nbChannelsPerNode match {
    case Some(numberOfChannels) => driver.connection(servers, auth, nbChannelsPerNode = numberOfChannels)
    case _                      => driver.connection(servers, auth)
  }

  def db = DB(dbName, connection)
}

// ------------------------------------------------------------------------------------------------------------

object Mailer {

  private val log = org.clapper.avsl.Logger("oadmin.mailer")

  def sendPasswordResetEmail(username: String, key: String) {
    val subj = "[Schola] Password reset request"

    val hostname = "localhost" // TODO: change to normal host system
    val port = 3000

    val msg = s"""
      | Someone requested that the password be reset for the following account:\r\n\r\n
      | Username: $username \r\n\r\n
      | If this was a mistake, just ignore this email and nothing will happen. \r\n\r\n
      | To reset your password, visit the following address:\r\n\r\n
      | < http://$hostname${if (port == 80) "" else ":" + port}/RstPasswd?key=$key&login=${java.net.URLEncoder.encode(username, "UTF-8")} >\r\n""".stripMargin

    sendEmail(subj, username, (Some(msg), None))
  }

  def sendPasswordChangedNotice(username: String) {
    val subj = "[Schola] Password change notice"

    val msg = s"""
      | Someone just changed the password for the following account:\r\n\r\n
      | Username: $username \r\n\r\n
      | If this was you, congratulation! the change was successfull. \r\n\r\n
      | Otherwise, contact your administrator immediately.\r\n""".stripMargin

    sendEmail(subj, username, (Some(msg), None))
  }

  def sendWelcomeEmail(username: String, password: String) {
    val subj = "[Schola] Welcome to Schola!"

    val hostname = "localhost" // TODO: change to normal host system
    val port = 3000

    val msg = s"""
      | Congratulation, your account was successfully created.\r\n\r\n
      | Here are the details:\r\n\r\n
      | Username: $username \r\n\r\n
      | Username: $password \r\n\r\n
      | Sign in immediately at < http://$hostname${if (port == 80) "" else ":" + port}/Login > to reset your password and start using the service.\r\n\r\n
      | Thank you.\r\n""".stripMargin

    sendEmail(subj, username, (Some(msg), None))
  }

  val fromAddress = config.getString("smtp.from")

  private lazy val mock = config.getBoolean("smtp.mock")

  private lazy val mailer: MailerAPI = if (mock) {
    MockMailer
  } else {

    import scala.util.control.Exception.allCatch

    val smtpHost = config.getString("smtp.mailhub")
    val smtpPort = config.getInt("smtp.port")
    val smtpSsl = config.getBoolean("smtp.ssl")
    val smtpTls = config.getBoolean("smtp.tls")

    val smtpUser = allCatch.opt { config.getString("smtp.user") }
    val smtpPassword = allCatch.opt { config.getString("smtp.password") }

    new CommonsMailer(smtpHost, smtpPort, smtpSsl, smtpTls, smtpUser, smtpPassword)
  }

  private def sendEmail(subject: String, recipient: String, body: (Option[String], Option[String])) {
    import scala.concurrent.duration._
    import system.dispatcher

    if (log.isDebugEnabled) {
      log.debug("[oadmin] sending email to %s".format(recipient))
      log.debug("[oadmin] mail = [%s]".format(body))
    }

    system.scheduler.scheduleOnce(1 second) {

      mailer.setSubject(subject)
      mailer.setRecipient(recipient)
      mailer.setFrom(fromAddress)

      mailer.setReplyTo(fromAddress)

      // the mailer plugin handles null / empty string gracefully
      mailer.send(body._1 getOrElse "", body._2 getOrElse "")
    }
  }
}

// ---------------------------------------------------------------------------------------------------------------

import org.fusesource.scalate.{ TemplateEngine, Binding, DefaultRenderContext, RenderContext }
import unfiltered.request.{ Path, HttpRequest }
import unfiltered.response.ResponseWriter
import java.io.{ File, OutputStreamWriter, PrintWriter }

object Scalate {

  import libs.Flash

  private def flash[A](req: HttpRequest[A]) =
    unfiltered.request.Cookies(req).get(Flash.COOKIE_NAME).map(Flash.decodeFromCookie(_).data).getOrElse(Map[String, String]())

  private def agent[A](req: HttpRequest[A]) = req match {
    case unfiltered.request.AgentIs.Chrome(_)  => "chrome"
    case unfiltered.request.AgentIs.FireFox(_) => "moz"
    case _                                     => ""
  }

  def apply[A, B](request: HttpRequest[A],
                  template: String,
                  attributes: (String, Any)*)(implicit engine: TemplateEngine = defaultEngine,
                                              contextBuilder: ToRenderContext = defaultRenderContext,
                                              bindings: List[Binding] = Nil,
                                              additionalAttributes: Seq[(String, Any)] = Nil): ResponseWriter = apply(Path(request), template, Seq("agent" -> agent(request), "flash" -> flash(request)) ++ attributes: _*)

  /**
   * Constructs a ResponseWriter for Scalate templates.
   * Note that any parameter in the second, implicit set
   * can be overriden by specifying an implicit value of the
   * expected type in a pariticular scope.
   */
  def apply[A, B](path: String,
                  template: String,
                  attributes: (String, Any)*)(implicit engine: TemplateEngine = defaultEngine,
                                              contextBuilder: ToRenderContext = defaultRenderContext,
                                              bindings: List[Binding] = Nil,
                                              additionalAttributes: Seq[(String, Any)] = Nil) = new ResponseWriter {
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
  type ToRenderContext = (String, PrintWriter, TemplateEngine) => RenderContext

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

  import javax.crypto._, spec.PBEKeySpec
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

  val genMacKey = {

    val rounds = 65536

    val size = 256

    val alg = "PBKDF2WithHMACSHA1"

    (password: String) => {
      val spec = new PBEKeySpec(password.toCharArray, utils.randomBytes(16), rounds, size)
      val keys = SecretKeyFactory.getInstance(alg)
      val key = keys.generateSecret(spec)
      Hex.toHexString(key.getEncoded)
    }
  }

  val generateToken = {
    val bytes = new Array[Byte](12)

    () => bytes synchronized {
      random.nextBytes(bytes)
      Hex.toHexString(bytes)
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

object `package` {

  def genPasswd(key: String) = passwords.crypt(key)

  private val random = java.security.SecureRandom.getInstance("SHA1PRNG")

  def randomBytes(size: Int) = {
    val b = new Array[Byte](size)
    random.nextBytes(b)
    b
  }

  def randomString(size: Int) = {
    org.bouncycastle.util.encoders.Hex.toHexString(randomBytes(size))
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

  @inline def If[T](cond: Boolean, t: => T, f: => T) = if (cond) t else f

  @inline def option[T](cond: => Boolean, value: => T): Option[T] = if (cond) Some(value) else None

  // ---------------------------------------------------------------------------------------------------

  import org.json4s._

  def findFieldStr(json: JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _                             => false
    } collect {
      case org.json4s.JField(_, org.json4s.JString(s)) => s
    }

  def findFieldJArr(json: JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _                             => false
    } collect {
      case org.json4s.JField(_, a @ org.json4s.JArray(_)) => a
    }

  def findFieldJObj(json: JValue)(field: String) =
    json findField {
      case org.json4s.JField(`field`, _) => true
      case _                             => false
    } collect {
      case org.json4s.JField(_, o @ org.json4s.JObject(_)) => o
    }
}