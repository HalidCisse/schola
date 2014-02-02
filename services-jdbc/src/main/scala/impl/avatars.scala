package schola
package oadmin

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

  def uploadAvatar(userId: String, filename: String, contentType: Option[String], data: Array[Byte], listener: ActorRef) = {
    import play.api.libs.iteratee.Enumerator

    log.debug("uploading new avatar . . .")

    gridFS.save(Enumerator(data), DefaultFileToSave(filename, contentType, metadata = BSONDocument("user_id" -> BSONString(userId))))
      .map(_.id.asInstanceOf[BSONObjectID].stringify) pipeTo listener

  }

  def purgeAvatar(id: String, listener: ActorRef) = gridFS.remove(BSONObjectID(id)) map (_.ok) pipeTo listener

  def purgeAvatarForUser(userId: String, listener: ActorRef) = {
    val fs = gridFS.find(BSONDocument("metadata" -> BSONDocument("user_id" -> BSONString(userId))))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f: ReadFile[BSONValue] =>
      gridFS.remove(f.id) map (_.ok)
    } pipeTo listener
  }

  def getAvatar(id: String, listener: ActorRef) = {
    import play.api.libs.iteratee.Iteratee

    val fs = gridFS.find(BSONDocument("_id" -> BSONObjectID(id)))

    fs.headOption.filter(_.isDefined).map(_.get).flatMap { f: ReadFile[BSONValue] =>
      val en = gridFS.enumerate(f)

      for {
        bytes <- en run Iteratee.consume[Array[Byte]]()
      } yield (f.filename, f.contentType, bytes)
    } pipeTo listener

  }

  def receive = {
    case Avatars.Save(userId, filename, contentType, fdata) =>

      uploadAvatar(userId, filename, contentType, fdata, sender)

    case Avatars.Get(id) =>

      getAvatar(id, sender)

    case Avatars.PurgeForUser(userId) =>

      purgeAvatarForUser(userId, sender)

    case Avatars.Purge(id) =>

      purgeAvatar(id, sender)
  }
}

class Avatars(config: MongoDBSettings)(implicit system: akka.actor.ActorSystem) extends akka.actor.Actor with akka.actor.ActorLogging {

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
    props = Props(new AvatarWorkers(helper)).withRouter(RandomRouter(4)).withDeploy(Deploy.local), // TODO: change when you upgrade to 2.3
    name = "Avatars_upload-workers")

  def receive = {
    case msg: Avatars.Msg =>
      workerPool tell (msg, sender)
  }
}

object Avatars {

  sealed trait Msg

  case class Save(userId: String, filename: String, contentType: Option[String], fdata: Array[Byte]) extends Msg

  case class Get(id: String) extends Msg

  case class Purge(id: String) extends Msg

  case class PurgeForUser(userId: String) extends Msg

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