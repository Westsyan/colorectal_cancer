package tools

import config.MyFile
import models.Tables.ToolsRow
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

/**
 * 频率直方图
 */
object Fh extends MyFile {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "FH Success!"
    try {
      val matrix = s"$path/matrix.txt"
      request.body.file("matrix").get.ref.moveTo(matrix.toFile)
      val bw = if (params("bw") == "") "" else " -bw " + params("bw")
      val exec = new ExecCommand()
      val cmd = s"Rscript ${Global.toolsPath}/fh/Frequency.R -i $matrix -o $path  -if pdf -c #0000FF $bw"
      exec.exect(cmd, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("Frequency_bar", path)
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val drawParams = if (row.drawparams == "") {
      Map("bw"->params("bw"), "color"->"#0000FF", "xtpangle"->"60", "xtphjust"->"1", "xts"->"13", "yts"->"12",
        "xls"->"16", "yls"->"16","ms"->"16","xtext"->"", "ytext"->"count", "mstext"->"",
        "dpi"->"300", "width" -> "7", "height"->"7")
    } else {
      Utils.jsonToMap(row.drawparams)
    }
    Json.obj("drawParams" -> drawParams, "title" -> row.name)
  }

  def ReDraw(path: String)(implicit request: Request[AnyContent]): (Int, String, JsValue) = {
    var state = 0
    var msg = "FH Success!"
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    try {
      val bw = if (data("bw") == "") "" else " -bw " + data("bw")
      val xtext=  data.getOrElse("xtext"," ")
      val ytext= data.getOrElse("ytext"," ")
      val mstext= data.getOrElse("mstext"," ")

      val command = s"Rscript ${Global.toolsPath}/fh/Frequency.R  -i $path/matrix.txt -o $path -if pdf " +
        s" -c ${data("color")} $bw -xts sans:bold.italic:${data("xts")} " +
        s"-xls sans:bold.italic:${data("xls")}:$xtext:black -yts sans:bold.italic:${data("yts")} " +
        s"-yls sans:bold.italic:${data("yls")}:$ytext:black -xtp ${data("xtpangle")}:${data("xtphjust")} " +
        s"-ms sans:bold.italic:${data("ms")}:$mstext:black -ls ${data("width")}:${data("height")} -dpi ${data("dpi")}"

      val exec = new ExecCommand()
      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("Frequency_bar", path)
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }



}
