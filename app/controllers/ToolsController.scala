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
import tools._
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

  def toolsPageEn(tools: String) = Action { implicit request =>
    val title = tools match {
      case "pca" => "PCA"
      case "pcoa" => "PCoA"
      case "cca" => "CCA/RDA"
      case "heatmap" => "Heatmap"
      case "igc" => "Intra-group Correlation analysis"
      case "itc" => "Inner-group Correlation  analysis"
      case "tax4" => "Tax4Fun"
      case "volcano" => "Volcano"
      case "fh" => "Frequency histogram"
      case "ternary" => "Ternary"
      case "treemap" => "Treemap"
      case "lefse" => "Lefse"
    }
    Ok(views.html.en.tools.toolsPage(tools, title))
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
        case x if x == "igc" || x == "itc" =>
          val drawParams = Map("net" -> Json.obj("gshape" -> "ellipse", "netcolor1" -> "#555555", "gopa" -> "1", "gsize" -> "5",
            "gfont" -> "20", "netcolor2" -> "#ffffff", "eshape" -> "diamond", "netcolor3" -> "#5da5fb", "eopa" -> "1",
            "esize" -> "10", "efont" -> "20", "netcolor4" -> "#ffffff", "netcolor5" -> "#737373", "opacity" -> "0.6",
            "dot" -> "3", "pthres" -> "0.1", "cthres" -> "0.5").toString(),
            "heatmap" -> Json.obj("smt" -> "full", "cluster_rows" -> "FALSE", "crm" -> "complete",
              "rp" -> "1", "cluster_cols" -> "FALSE", "ccm" -> "complete", "cp" -> "1", "sc" -> "none", "lg" -> "none",
              "color" -> "#E41A1C:#FFFF00:#1E90FF", "cc" -> "30", "nc" -> "#DDDDDD", "hasborder" -> "white",
              "cbc" -> "#ffffff", "hasnum" -> "FALSE", "hasrname" -> "TRUE", "hascname" -> "TRUE",
              "rtree" -> "50", "ctree" -> "50", "xfs" -> "10", "yfs" -> "10", "xfa" -> "90", "fn" -> "8", "lfi" -> "TRUE").toString())
          toolsDao.updateDrawParamsById(id, Utils.mapToJson(drawParams)).toAwait
          IgcOrItc.Run(path, params, tools)
        case "tax4" => Tax4.Run(path, params)
        case "volcano" => Volcano.Run(path, params)
        case "fh" => Fh.Run(path, params)
        case "ternary" => Ternary.Run(path, params)
        case "treemap" => Treemap.Run(path, params)
        case "lefse" =>
          val drawParams = Map("res" -> Json.obj("resmfl" -> "60", "resffs" -> "7",
            "rescfs" -> "7", "resdpi" -> "300", "restitle" -> "", "restfs" -> "12", "reswidth" -> "10",
            "resheight" -> "4", "resorientation" -> "h", "resnscl" -> "1").toString,
            "cla" -> Json.obj("clasc" -> "", "claevl" -> "1", "claml" -> "6", "clacs" -> "1.5",
              "clarsl" -> "1", "clalstartl" -> "2", "clalstopl" -> "5", "claastartl" -> "3", "claastopl" -> "5",
              "clamaxps" -> "6", "claminps" -> "1", "claalpha" -> "0.2", "clapew" -> "0.25", "clascw" -> "2",
              "clapcw" -> "0.75", "clarsp" -> "0.15", "clatitle" -> "Cladogram", "clatfs" -> "14", "clalfs" -> "6",
              "claclfs" -> "8", "cladpi" -> "300", "claclv" -> "1").toString,
            "fea" -> Json.obj("feaf" -> "diff", "feafname" -> "", "feawidth" -> "13", "feaheight" -> "6",
              "featop" -> "-1", "feabot" -> "0", "featfs" -> "14", "feacfs" -> "10", "feasmean" -> "y", "feasmedian" -> "y",
              "feafs" -> "10", "feadpi" -> "300", "feaca" -> "0").toString)

          toolsDao.updateDrawParamsById(id, Utils.mapToJson(drawParams)).toAwait
          Lefse.Run(path, params)
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
    "itc" -> Array(("cor.xls", "相关性系数矩阵"), ("pvalue.xls", "p值矩阵"), ("pandv.xls", "相关性系数c值和p值分析结果"),
      ("p_star.xls", "根据p值生成的星星矩阵"), ("heatmap.pdf", "热图")),
    "tax4" -> Array(("ko_table.xls", "kegg丰度表"), ("kegg_L1.txt", "kegg pathway 第一个层级丰度表"),
      ("kegg_L2.txt", "kegg pathway 第二个层级丰度表"), ("kegg_L3.txt", "kegg pathway 第三个层级丰度表"),
      ("kegg_enzyme.txt", "kegg enzyme丰度表"), ("kegg_pathway.txt", "kegg pathway丰度表"), ("pca.pdf", "PCA图"),
      ("kegg_L1.pdf", "第一个层级箱线图"), ("kegg_L2.pdf", "第二个层级箱线图"), ("kegg_L3.pdf", "第三个层级箱线图")),
    "volcano" -> Array(("volcano.pdf", "火山图")),
    "fh" -> Array(("Frequency_bar.pdf", "频率直方图")),
    "ternary" -> Array(("ternary.pdf", "三原图")),
    "treemap" -> Array(("treemap.pdf", "树图")),
    "lefse" -> Array(("lefse_LDA.xls", "LDA判别分析结果"), ("lefse_LDA_diff.xls", "LDA判别分析结果（仅含差异显著）"),
      ("lefse_LDA.pdf", "LDA分析柱图"), ("lefse_LDA.cladogram.pdf", "进化分支图"), ("lefse_LDA.features.pdf", "差异特征图"))
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
      case "fh" => Fh.GetParams(row)
      case "ternary" => Ternary.GetParams(row)
      case "treemap" => Treemap.GetParams(row)
      case "lefse" => Lefse.GetParams(row)
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
      case x if x == "igc" || x == "itc" =>
        val row = toolsDao.getById(id).toAwait
        IgcOrItc.ReDraw(path, x, row)
      case "volcano" => Volcano.ReDraw(path)
      case "fh" => Fh.ReDraw(path)
      case "ternary" => Ternary.ReDraw(path)
      case "treemap" => Treemap.ReDraw(path)
      case "lefse" =>
        val row = toolsDao.getById(id).toAwait
        Lefse.ReDraw(path, row)
    }
    toolsDao.updateDrawParamsAndStateById(id, result._1, result._3.toString).toAwait
    FileUtils.writeStringToFile(s"$path/log.txt".toFile, result._2)
    Ok(Json.obj("state" -> result._1, "msg" -> result._2))
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
