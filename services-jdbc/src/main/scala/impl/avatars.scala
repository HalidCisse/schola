package ma.epsilon.schola

package impl

class MongoDBSettings(config: com.typesafe.config.Config) {
  val Host = config.getString("host")
  val Database = config.getString("db")
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

  def uploadAvatar(userId: String, filename: String, contentType: Option[String], data: Array[Byte]) = {
    import play.api.libs.iteratee.Enumerator

    log.debug("uploading new avatar . . .")

    gridFS.save(
      Enumerator(data),
      DefaultFileToSave(filename, contentType, metadata = BSONDocument("user_id" -> BSONString(userId))))
  }

  def purgeAvatar(userId: String) = {
    val fs = gridFS.find(BSONDocument("metadata" -> BSONDocument("user_id" -> BSONString(userId))))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f: ReadFile[BSONValue] =>
      gridFS.remove(f.id)
    }
  }

  def getAvatar(userId: String) = {
    import play.api.libs.iteratee.Iteratee

    val fs = gridFS.find(BSONDocument("metadata" -> BSONDocument("user_id" -> BSONString(userId))))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f: ReadFile[BSONValue] =>
      val en = gridFS.enumerate(f)

      for {
        bytes <- en run Iteratee.consume[Array[Byte]]()
      } yield (f.filename, f.contentType, bytes)
    }
  }

  def receive = {
    case Avatars.Save(userId, filename, contentType, bytes) =>

      uploadAvatar(userId, filename, contentType, bytes)

    case Avatars.Get(userId) =>

      getAvatar(userId) pipeTo sender

    case Avatars.Purge(userId) =>

      purgeAvatar(userId)
  }
}

class Avatars(config: MongoDBSettings) extends akka.actor.Actor with akka.actor.ActorLogging {

  import akka.actor._
  import akka.routing._

  import config._

  private val helper: ReactiveMongoHelper = {

    log.info("ReactiveMongo starting...")

    implicit def system = context.system

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

  override def postStop {
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
    props = FromConfig.props(Props(classOf[AvatarWorkers], helper)),
    name = "Avatars_upload-workers")

  def receive = {
    case msg: Avatars.Msg =>
      workerPool tell (msg, sender)
  }
}

object Avatars {

  sealed trait Msg

  case class Save(userId: String, filename: String, contentType: Option[String], fdata: Array[Byte]) extends Msg

  case class Get(userId: String) extends Msg

  case class Purge(userId: String) extends Msg

}

private[impl] case class ReactiveMongoHelper(dbName: String, servers: Seq[String], auth: Seq[reactivemongo.core.nodeset.Authenticate], nbChannelsPerNode: Option[Int])(implicit system: akka.actor.ActorSystem) {
  import reactivemongo.api._

  lazy val driver = MongoDriver(system)

  lazy val connection = nbChannelsPerNode match {
    case Some(numberOfChannels) => driver.connection(servers, auth, nbChannelsPerNode = numberOfChannels)
    case _                      => driver.connection(servers, auth)
  }

  def db = DB(dbName, connection)
}

trait AvatarServicesComponentImpl extends AvatarServicesComponent {
  this: AvatarServicesRepoComponent =>

  class AvatarServicesImpl extends AvatarServices {

    def getAvatar(id: String) = avatarServicesRepo.getAvatar(id)

    def uploadAvatar(userId: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
      avatarServicesRepo.uploadAvatar(userId, filename, contentType, bytes)

    def purgeAvatar(userId: String) =
      avatarServicesRepo.purgeAvatar(userId)
  }
}

trait AvatarServicesRepoComponentImpl extends AvatarServicesRepoComponent {
  this: UserServicesComponent with AkkaSystemProvider =>

  lazy protected val avatarServicesRepo = new AvatarServicesRepoImpl

  class AvatarServicesRepoImpl extends AvatarServicesRepo {

    private[this] val avatarService = system.actorOf(akka.actor.Props(classOf[Avatars], new MongoDBSettings(config getConfig "mongodb")), name = "avatars")

    def getAvatar(userId: String) = {
      import scala.concurrent.duration._
      import akka.pattern._

      implicit val timeout = akka.util.Timeout(60 seconds) // needed for `?` below

      (avatarService ? Avatars.Get(userId)).mapTo[(String, Option[String], Array[Byte])]
    }

    def uploadAvatar(userId: String, filename: String, contentType: Option[String], bytes: Array[Byte]) =
      avatarService ! Avatars.Save(userId, filename, contentType, bytes)

    def purgeAvatar(userId: String) =
      avatarService ! Avatars.Purge(userId)
  }
}