package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

import scala.collection.mutable

object PcaOrPcoa extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String], tools: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = tools match {
      case "pca" => "PCA Success!"
      case "pcoa" => "PCOA Success!"
    }
    try {
      val showname = params("showname")
      val showarrow = params("showarrow")
      val isgroup = params("isgroup")
      val matrix = s"$path/matrix.txt"

      request.body.file("matrix").get.ref.moveTo(matrix.toFile)
      val exec = new ExecCommand()
      val dataCmd = s"Rscript ${Global.toolsPath}/$tools/${tools}_data.R -i $matrix -o $path -sca TRUE"

      val group = if (isgroup == "TRUE") {
        val g = "#SampleID\tGroup\n" + request.body.file("group").get.ref.path.toFile.readFileToString
        FileUtils.writeStringToFile(s"$path/group.txt".toFile, g)
        s" -g $path/group.txt"
      } else ""

      val groupName = showname match {
        case "TRUE" =>
          isgroup match {
            case "TRUE" =>
              val f = s"$path/group.txt".readLines
              val n = f.map(_.split("\t").last).distinct.drop(1).mkString(",")
              " -b " + n
            case "FALSE" => " -sl TRUE"
          }
        case "FALSE" => ""
      }

      val plotCmd = s"Rscript  ${Global.toolsPath}/$tools/${tools}_plot.R -i $path/$tools.x.xls -si $path/$tools.sdev.xls $group " +
        s"-o $path $groupName -if pdf -ss $showarrow -in $tools"
      exec.exect(Array(dataCmd, plotCmd), path)
      println(dataCmd,plotCmd)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      }else {
        PdfToImage.pdf2Png(tools,path)
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow, tools: String)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val path = Global.getToolsPath(request.userId, row.id, tools)

    val drawParams = if (row.drawparams == "") {
      val co = if (params("isgroup") == "TRUE") {
        "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#CAAA76" +
          ":#EEEE00:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F"
      } else "#48FF75"

      val dataN = tools match {
        case "pca" => "PC"
        case "pcoa" => "PCOA"
      }

      Map("xdata" -> (dataN + "1"), "ydata" -> (dataN + "2"), "width" -> "15", "length" -> "12", "showname2" -> params("showname"),
        "showarrow2" -> params("showarrow"), "color" -> co, "resolution" -> "300", "xts" -> "15", "yts" -> "15", "xls" -> "17", "yls" -> "17",
        "lts" -> "14", "lms" -> "15", "lmtext" -> "", "ms" -> "17", "mstext" -> "", "c" -> "FALSE", "big" -> "no",
        "xdamin" -> "", "xdamax" -> "", "ydamin" -> "", "ydamax" -> "")
    } else {
      Utils.jsonToMap(row.drawparams)
    }
    val group = if (params("isgroup") == "TRUE") {
      val f = s"$path/group.txt".readLines
      val g = f.map(_.split("\t").last).tail
      val sample = s"$path/matrix.txt".readLines.head.split("\t").tail
      if (g.length == sample.length) g.distinct else g.distinct :+ "nogroup"
    } else mutable.Buffer("nogroup")
    val col = s"$path/$tools.x.xls".readLines.head.replaceAll("\"", "").trim.split("\t").map(_.trim)
    val color = drawParams("color").split(":")
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
        s"-yls sans:plain:${data("yls")} -lts sans:plain:${data("lts")} -if pdf -ss ${data("showarrow2")} $groupName $lms $ms -c ${data("c")} $big -in $tools"

      val exec = new ExecCommand()
      exec.exect(Array(plotCmd), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png(tools,path)
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }


}
