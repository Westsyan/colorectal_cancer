package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

import scala.collection.mutable

object Volcano extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "Volcano Success!"
    try {
      val matrix = s"$path/matrix.txt"

      request.body.file("matrix").get.ref.moveTo(matrix.toFile)

      val command = s"Rscript ${Global.toolsPath}/volcano/volcano.R -i $matrix -o $path -pcl ${params("pcl")} -fcl ${params("fcl")} -if pdf -in volcano"
      val exec = new ExecCommand()

      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getLastStr
      } else {
        PdfToImage.pdf2Png("volcano", path)
      }
    } catch {
      case e: Exception => state = 2; msg = e.toString
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val drawParams = if (row.drawparams == "") {
      Map("pcl" -> params("pcl"), "fcl" -> params("fcl"), "xrmin" -> "", "xrmax" -> "",
        "yrmin" -> "", "yrmax" -> "", "sp" -> "TRUE", "color4" -> "black", "color0" -> "blue",
        "color1" -> "red","color2" -> "grey",
        "xts" -> "16", "yts" -> "16", "xls" -> "18", "yls" -> "18", "ts" -> "20", "lts" -> "16", "ltes" -> "12",
        "xtext" -> "log2(FC)", "ytext" -> "-log10(pvalue)", "tstext" -> "", "ltstext" -> "", "width" -> "15",
        "height" -> "15", "dpi" -> "300")
    } else {
      Utils.jsonToMap(row.drawparams)
    }
    Json.obj("drawParams" -> drawParams, "title" -> row.name)
  }

  def ReDraw(path: String)(implicit request: Request[AnyContent]): (Int, String, JsValue) = {
    var state = 0
    var msg = "Volcano Success!"
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    try {
      val xr = if (data("xrmin") == "" || data("xrmax") == "") "" else s" -xr ${data("xrmin")}:${data("xrmax")}"
      val yr = if (data("yrmin") == "" || data("yrmax") == "") "" else s" -yr ${data("yrmin")}:${data("yrmax")}"
      val command = s"Rscript ${Global.toolsPath}/volcano/volcano.R -i $path/matrix.txt -o $path -pcl ${data("pcl")}" +
        s" -fcl ${data("fcl")} -sp  ${data("sp")} $xr $yr -lc ${data("color4")}" +
        s" -cs ${data("color0")}:${data("color1")}:${data("color2")} -xts sans:bold.italic:${data("xts")} " +
        s"-xls sans:bold.italic:${data("xls")}:${data("xtext")} -yts sans:bold.italic:${data("yts")} " +
        s"-yls sans:bold.italic:${data("yls")}:${data("ytext")} -ts sans:bold.italic:${data("ts")}:${data("tstext")} " +
        s"-lts sans:bold.italic:${data("lts")}:${data("ltstext")} -ltes sans:bold.italic:${data("ltes")} " +
        s"-is ${data("width")}:${data("height")} -dpi ${data("dpi")} -if pdf -in volcano"

      println(command)
      val exec = new ExecCommand()
      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("volcano", path)
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }


}
