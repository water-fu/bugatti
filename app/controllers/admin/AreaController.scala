package controllers.admin

import actor.ActorUtils
import actor.salt._
import controllers.BaseController
import enums.{ModEnum, RoleEnum}
import exceptions.UniqueNameException
import models.conf.{Area, AreaEnvironmentRelHelper, AreaHelper, AreaInfo}
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json.Json
import play.api.mvc._

/**
 * 区域管理
 * @author of557
 */
object AreaController extends BaseController {

  def msg(user: String, ip: String, msg: String, data: Area) =
    Json.obj("mod" -> ModEnum.area.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  implicit val areaFormat = Json.format[Area]
  implicit val areaInfoFormat = Json.format[AreaInfo]

  val areaForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText(maxLength = 30),
      "syndicName" -> nonEmptyText(maxLength = 30),
      "syndicIp" -> nonEmptyText(maxLength = 30)
    )(Area.apply)(Area.unapply)
  )

  def all = Action {
    Ok(Json.toJson(AreaHelper.allInfo))
  }

  def get(id: Int) = Action {
    Ok(Json.toJson(AreaHelper.findInfoById(id)))
  }

  def list(envId: Int) = Action {
    Ok(Json.toJson(AreaEnvironmentRelHelper.findAreasByEnvId(envId)))
  }

  def save = AuthAction(RoleEnum.admin) { implicit request =>
    areaForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      area => {
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增区域", area))
          val areaId = AreaHelper.create(area)
          val newArea = area.copy(id = Option(areaId))
          Ok(Json.toJson(areaId))
        } catch {
          case un: UniqueNameException => Ok(resultUnique(un.getMessage))
        }
      }
    )
  }

  def update = AuthAction(RoleEnum.admin) { implicit request =>
    areaForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      area => {
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改区域", area))
          Ok(Json.toJson(AreaHelper.update(area)))
        } catch {
          case un: UniqueNameException => Ok(resultUnique(un.getMessage))
        }
      }
    )
  }

  def delete(id: Int) = AuthAction(RoleEnum.admin) { implicit request =>
    AreaHelper.findById(id) match {
      case Some(area) =>
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除区域", area))
        Ok(Json.toJson(AreaHelper.delete(id)))
      case None => NotFound
    }
  }

  def refresh(id: Int) = AuthAction(RoleEnum.admin) { implicit request =>
    AreaHelper.findById(id) match {
      case Some(area) => {
        ActorUtils.spiritsRefresh ! RefreshSpiritsActor.RefreshHosts(id)
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "刷新区域", area))
        Ok(Json.toJson(AreaHelper.findInfoById(id)))
      }
      case None => NotFound
    }
  }

}
