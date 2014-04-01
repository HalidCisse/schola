package controllers

import play.api.mvc._
import play.api.Play.current

import ma.epsilon.schola._, domain._, http.{ Façade, Secured, HttpHelpers }, utils._, conversions.json._
import com.typesafe.plugin._

object Tags extends Controller with Secured with HttpHelpers {

  def getTags =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>
              json[List[Label]](use[Façade].labelService.getLabels)
          }
    }

  def addTag(name: String, color: Option[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          render {
            case Accepts.Json() =>

              use[Façade].labelService.findOrNew(name, color) match {
                case Some(tag) => json[Label](tag)
                case _         => BadRequest
              }
          }
    }

  def updateTag(name: String, newName: String) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = use[Façade].labelService.updateLabel(name, newName)

          render {
            case Accepts.Json() =>
              json[Response](Response(success = result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }

  def updateTagColor(name: String, color: String) =
    addTag(name, Some(color))

  def purgeTags(labels: List[String]) =
    withAuth {
      _ =>
        implicit request: RequestHeader =>

          def result = tryo(use[Façade].labelService.remove(labels))

          render {
            case Accepts.Json() =>
              json[Response](Response(success = result))

            case _ =>

              if (result) Ok
              else BadRequest
          }
    }
}