package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, Utils}

import scala.collection.mutable

object Cca extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "CCA Success!"
    try {
      val isgroup = params("isgroup")
      val otu = s"$path/otu.txt"
      val env = s"$path/env.txt"
      val anatype = params("anatype")
      request.body.file("otu").get.ref.moveTo(otu.toFile)
      request.body.file("env").get.ref.moveTo(env.toFile)
      val group = if (isgroup == "TRUE"){
        request.body.file("group").get.ref.moveTo(s"$path/group.txt".toFile)
        s" -g $path/group.txt"
      }
      else ""
      val exec = new ExecCommand()
      val dataCmd =  s"Rscript ${Global.toolsPath}/cca/rda_cca_data.R -pi $otu -ei $env -o $path -m $anatype"
      val plotCmd = s"Rscript ${Global.toolsPath}/cca/rda_cca_plot.R -sai $path/samples.xls -spi $path/species.xls  " +
        s"-ei $path/envi.xls  -pci $path/percent.xls -o $path -is 15:15 " + group
      exec.exect(Array(dataCmd, plotCmd), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {

      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val drawParams = if (row.drawparams == "") {
      val color= if(params("isgroup") == "TRUE") "#336666:#996633:#CCCC33:#336633:#990033:#FFCC99:#333366:#669999:#996600" else "#1E90FF"
      val anatype = params("anatype")
      Map("xdata" -> (anatype + "1"), "ydata" -> (anatype + "2"),"xaxis"->"0","yaxis"->"0","samsize"->"6",
        "color"->color,"showsname"->"true","samfont"->"7","showevi"->"true","color1"->"#E41A1C",
        "evifont"->"7","color2"->"#E41A1C","showspecies"->"true","speciesfont"->"7","speciessize"->"6",
        "color3"->"#FF8C00","width"->"15","height"->"15","dpi"->"300","xts"->"16","yts"->"16",
        "xls"->"18","yls"->"18","lts"->"15","lms"->"19","ms"->"12","mstext"->"")
    } else {
      Utils.jsonToMap(row.drawparams)
    }

    val path = Global.getToolsPath(request.userId, row.id, "cca")
    val col = s"$path/samples.xls".readLines.head.replaceAll("\"", "").trim.split("\t").map(_.trim)

    val color = drawParams("color").split(":")
    val group = if (params("isgroup") == "TRUE") {
      val f = s"$path/group.txt".readLines
      val g = f.map(_.split("\t").last).tail
      val sample = s"$path/otu.txt".readLines.head.split("\t").tail
      if(g.length == sample.length) g.distinct else g.distinct :+ "nogroup"
    } else mutable.Buffer("nogroup")
    Json.obj("group" -> group, "cols" -> col, "color" -> color, "drawParams" -> drawParams, "title" -> row.name)
  }

  def ReDraw(implicit request: Request[AnyContent]) = {
    var state = 0
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":")) ++ Map("width" -> "15", "length" -> "12")
    val id = data("id").toInt
    val tools = data("tools")
    var msg = tools match {
      case "pca" => "PCA Success!"
      case "pcoa" => "PCOA Success!"
    }
    try {
      val path = Global.getToolsPath(request.userId, id, tools)
      val group = s"$path/group.txt"
      val isgroup = group.toFile.exists()
      val c =
        if (!isgroup) " -oc \"" + data("color") + "\""
        else " -cs \"" + data("color") + "\""

      val groupdata = if (isgroup) " -g " + group else ""
      val groupName = data("showname2") match {
        case "TRUE" =>
          if (isgroup) {
            val f = group.readLines
            val n = f.map(_.split("\t").last).distinct.drop(1).mkString(",")
            " -b " + n
          } else " -sl TRUE"
        case "FALSE" => ""
      }
      val big = data("big") match {
        case "no" => ""
        case "x" => " -da x:" + data("xdamin") + "," + data("xdamax")
        case "y" => " -da y:" + data("ydamin") + "," + data("ydamax")
      }

      val lms = if (data("lmtext") != "") " -lms sans:bold.italic:" + data("lms") + ":\"" + data("lmtext") + "\"" else ""
      val ms = if (data("mstext") != "") " -ms sans:plain:" + data("ms") + ":\"" + data("mstext") + "\"" else ""
      val script = s"Rscript ${Global.toolsPath}/$tools/${tools}_plot.R -i $path/$tools.x.xls -si $path/$tools.sdev.xls"
      val plotCmd = s"$script $groupdata -o $path -pxy ${data("xdata")}:${data("ydata")} -is ${data("width")}:${data("length")} " +
        s"$c -dpi ${data("resolution")} -xts sans:plain:${data("xts")} -yts sans:plain:${data("yts")} -xls sans:plain:${data("xls")} " +
        s"-yls sans:plain:${data("yls")} -lts sans:plain:${data("lts")} -if png -ss ${data("showarrow2")} $groupName $lms $ms -c ${data("c")} $big -in $tools"

      val exec = new ExecCommand()
      exec.exect(Array(plotCmd), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else state = 1
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }


}
