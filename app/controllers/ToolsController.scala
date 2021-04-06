package controllers

import java.io.File

import akka.stream.IOResult
import akka.stream.scaladsl.FileIO
import config.MyConfig
import dao.ToolsDao
import javax.inject.Inject
import models.Tables.ToolsRow
import org.apache.commons.io.FileUtils
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents, MultipartFormData, Request}
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}
import tools.{Cca, Heatmap, IgcOrItc, PcaOrPcoa, Tax4, Volcano}
import utils.{ExecCommand, Global, Utils}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class ToolsController @Inject()(toolsDao: ToolsDao, cc: ControllerComponents)(implicit exec: ExecutionContext) extends AbstractController(cc) with MyConfig {


  def toolsPage(tools: String) = Action { implicit request =>
    val title = tools match {
      case "pca" => "主成分分析(PCA)"
      case "pcoa" => "PCoA"
      case "cca" => "CCA/RDA"
      case "heatmap" => "热图(Heatmap)"
      case "igc" => "组内相关性分析"
      case "itc" => "组间相关性分析"
      case "tax4" => "Tax4Fun功能预测"
      case "volcano" => "火山图(Volcano)"
      case "fh" => "频率直方图"
      case "ternary" => "三元图"
      case "treemap" => "树图(Treemap)"
      case "lefse" => "lefse分析"
    }
    Ok(views.html.cn.tools.toolsPage(tools, title))
  }

  def getAllInfoByTools(tools: String) = Action.async { implicit request =>
    toolsDao.getByToolsAndUserId(request.userId, tools).map { x =>
      Ok(Json.toJson(x.sortBy(_.start).reverse.map(_.getJsonByT())))
    }
  }

  def toolsRun(tools: String): Action[MultipartFormData[Files.TemporaryFile]] = Action(parse.multipartFormData) { implicit request =>
    val params = request.body.asFormUrlEncoded.map { case (key, value) => key -> value.mkString(";") }
    val row = ToolsRow(0, request.userId, params("name"), tools, Utils.date, "", 0, Json.toJson(params).toString, "")
    val id = toolsDao.addToolReturnId(row).toAwait
    val path = Global.getToolsPath(request.userId, id, tools)
    path.mkdirs
    Future {
      val result = tools match {
        case x if x == "pca" || x == "pcoa" => PcaOrPcoa.Run(path, params, tools)
        case "cca" => Cca.Run(path, params)
        case "heatmap" => Heatmap.Run(path, params)
        case x if x == "igc" || x == "itc" => IgcOrItc.Run(path, params, tools)
        case "tax4" => Tax4.Run(path, params)
        case "volcano" => Volcano.Run(path, params)
      }


      toolsDao.updateState(id, result._1).toAwait
      FileUtils.writeStringToFile(s"$path/log.txt".toFile, result._2)
    }
    Ok(Json.toJson("success"))
  }

  val toolsMap = Map(
    "pca" -> Array(("pca.pdf", "PCA结果图"), ("pca.sdev.xls", "PCA值表格"), ("pca.rotation.xls", "特征向量矩阵表格"), ("pca.zip", "结果打包文件")),
    "pcoa" -> Array(("pcoa.pdf", "PCOA结果图"), ("pcoa.sdev.xls", "PCOA值表格"), ("pcoa.zip", "结果打包文件")),
    "cca" -> Array(("rdacca.pdf", "CCA/RDA结果图"), ("percent.xls", "百分比表"), ("samples.xls", "样本坐标表"),
      ("species.xls", "物种坐标表"), ("envi.xls", "环境因子坐标表"), ("rdacca.zip", "结果打包文件")),
    "heatmap" -> Array(("heatmap.pdf", "热图结果")),
    "igc" -> Array(("cor.xls", "相关性系数矩阵"), ("pvalue.xls", "p值矩阵"), ("pandv.xls", "相关性系数c值和p值分析结果"),
      ("p_star.xls", "根据p值生成的星星矩阵"), ("heatmap.pdf", "热图")),
    "tax4" -> Array(("ko_table.xls", "kegg丰度表"), ("kegg_L1.txt", "kegg pathway 第一个层级丰度表"),
      ("kegg_L2.txt", "kegg pathway 第二个层级丰度表"), ("kegg_L3.txt", "kegg pathway 第三个层级丰度表"),
      ("kegg_enzyme.txt", "kegg enzyme丰度表"), ("kegg_pathway.txt", "kegg pathway丰度表"), ("pca.pdf", "PCA图"),
      ("kegg_L1.pdf", "第一个层级箱线图"), ("kegg_L2.pdf", "第二个层级箱线图"), ("kegg_L3.pdf", "第三个层级箱线图")),
    "volcano" -> Array(("volcano.pdf", "火山图"))
  )


  def getRedrawParams(id: Int, tools: String) = Action { implicit request =>
    val row = toolsDao.getById(id).toAwait
    val tableData = toolsMap(tools)
    val params = tools match {
      case x if x == "pca" || x == "pcoa" => PcaOrPcoa.GetParams(row, tools)
      case "cca" => Cca.GetParams(row)
      case "heatmap" => Heatmap.GetParams(row)
      case x if x == "igc" || x == "itc" => IgcOrItc.GetParams(row, tools)
      case "tax4" => Json.obj("title" -> row.name)
      case "volcano" => Volcano.GetParams(row)
    }

    Ok(params ++ Json.obj("tableData" -> tableData))
  }

  def reDrawRun = Action { implicit request =>
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))

    val id = data("id").toInt
    val tools = data("tools")
    val path = Global.getToolsPath(request.userId, id, tools)
    toolsDao.updateState(id, 0).toAwait
    val result = tools match {
      case x if x == "pca" || x == "pcoa" => PcaOrPcoa.ReDraw
      case "cca" => Cca.ReDraw
      case "heatmap" => Heatmap.ReDraw(path)
      case "volcano" => Volcano.ReDraw(path)
    }
    toolsDao.updateDrawParamsAndStateById(id, result._1, result._3.toString).toAwait
    FileUtils.writeStringToFile(s"$path/log.txt".toFile, result._2)
    Ok(Json.toJson("success"))
  }

  def openLog(id: Int, tools: String): Action[AnyContent] = Action { implicit request =>
    val dir = Global.getToolsPath(request.userId, id, tools)
    val html = Global.getLogByPath(dir)
    Ok(Json.toJson(html))
  }

  def deleteById(id: Int, tools: String): Action[AnyContent] = Action.async { implicit request =>
    toolsDao.deleteById(id).map { _ =>
      Global.getToolsPath(request.userId, id, tools).delete
      Ok(Json.obj("code" -> 200))
    }
  }

  def reDrawIgcOrItc = Action { implicit request =>
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    val id = data("id").toInt
    val tools = data("tools")
    val path = Global.getToolsPath(request.userId, id, tools)
    toolsDao.updateState(id, 0).toAwait
    Ok(Json.toJson("1"))
  }

  case class NameData(name: String, tools: String)

  val NameForm = Form(
    mapping(
      "name" -> text,
      "tools" -> text
    )(NameData.apply)(NameData.unapply)
  )

  def checkName = Action { implicit request =>
    val data = NameForm.bindFromRequest.get
    Ok(Json.obj("valid" -> !(toolsDao.checkName(request.userId, data.tools, data.name).toAwait)))
  }


}
