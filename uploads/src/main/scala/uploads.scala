package ma.epsilon.schola
package impl

class MongoDBSettings(config: com.typesafe.config.Config) {
  val Host = config.getString("host")
  val Database = config.getString("db")
}

class UploadWorkers(helper: ReactiveMongoHelper) extends akka.actor.Actor with akka.actor.ActorLogging {
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

  def upload(id: String, filename: String, contentType: Option[String], data: Array[Byte], attributes: (String, String)*) = {
    import play.api.libs.iteratee.Enumerator

    log.debug("uploading new Upload . . .")

    gridFS.save(
      Enumerator(data),
      DefaultFileToSave(filename, contentType, metadata = BSONDocument(attributes map ({ case (k, s) => (k, BSONString(s)) })) ++ ("upload_id" -> BSONString(id))))
  }

  def purgeUpload(id: String) = {
    val fs = gridFS.find(BSONDocument("metadata" -> BSONDocument("upload_id" -> BSONString(id))))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f: ReadFile[BSONValue] =>
      gridFS.remove(f.id)
    }
  }

  def getUpload(id: String) = {
    import play.api.libs.iteratee.Iteratee

    val fs = gridFS.find(BSONDocument("metadata" -> BSONDocument("upload_id" -> BSONString(id))))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f: ReadFile[BSONValue] =>
      val en = gridFS.enumerate(f)

      for {
        bytes <- en run Iteratee.consume[Array[Byte]]()
      } yield domain.Upload(f.filename, f.contentType, bytes, f.metadata.elements collect { case (k, BSONString(s)) => (k, s) })
    }
  }

  def receive = {
    case Uploads.Save(id, filename, contentType, bytes, attributes) =>

      upload(id, filename, contentType, bytes, attributes: _*)

    case Uploads.Get(id) =>

      getUpload(id) pipeTo sender

    case Uploads.Purge(id) =>

      purgeUpload(id)
  }
}

class Uploads(config: MongoDBSettings) extends akka.actor.Actor with akka.actor.ActorLogging {

  import akka.actor._
  import akka.routing._

  import config._

  private val helper: ReactiveMongoHelper = {

    log.info("ReactiveMongo starting...")

    implicit def system = context.system

    val _helper = try {
      Some(ReactiveMongoHelper(Database, Seq(Host), Nil, None))
    } catch {
      case scala.util.control.NonFatal(ex) =>
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
    props = FromConfig.props(Props(classOf[UploadWorkers], helper)),
    // props = Props(classOf[UploadWorkers], helper).withRouter(RandomRouter(4)).withDeploy(Deploy.local), // TODO: change when you upgrade to 2.3
    name = "Uploads_upload-workers")

  def receive = {
    case msg: Uploads.Msg =>
      workerPool tell (msg, sender)
  }
}

object Uploads {

  sealed trait Msg

  case class Save(id: String, filename: String, contentType: Option[String], data: Array[Byte], attributes: Seq[(String, String)]) extends Msg

  case class Get(id: String) extends Msg

  case class Purge(id: String) extends Msg

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

trait UploadServicesComponentImpl extends UploadServicesComponent {
  this: UploadServicesRepoComponent =>

  class UploadServicesImpl extends UploadServices {

    def getUpload(id: String) = uploadServicesRepo.getUpload(id)

    def upload(id: String, filename: String, contentType: Option[String], bytes: Array[Byte], attributes: (String, String)*) =
      uploadServicesRepo.upload(id, filename, contentType, bytes, attributes: _*)

    def purgeUpload(id: String) =
      uploadServicesRepo.purgeUpload(id)
  }
}

trait UploadServicesRepoComponentImpl extends UploadServicesRepoComponent {
  this: AkkaSystemProvider =>

  lazy protected val uploadServicesRepo = new UploadServicesRepoImpl

  class UploadServicesRepoImpl extends UploadServicesRepo {

    private[this] val uploadService = system.actorOf(akka.actor.Props(classOf[Uploads], new MongoDBSettings(config getConfig "mongodb")), name = "Uploads")

    def getUpload(id: String) = {
      import scala.concurrent.duration._
      import akka.pattern._

      implicit val timeout = akka.util.Timeout(60 seconds) // needed for `?` below

      (uploadService ? Uploads.Get(id)).mapTo[domain.Upload]
    }

    def upload(id: String, filename: String, contentType: Option[String], bytes: Array[Byte], attributes: (String, String)*) =
      uploadService ! Uploads.Save(id, filename, contentType, bytes, attributes)

    def purgeUpload(id: String) =
      uploadService ! Uploads.Purge(id)
  }
}