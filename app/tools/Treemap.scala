package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

/**
 * Created By Xwq
 * 树图
 */
object Treemap extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "Treemap Success!"
    try {
      val matrix = s"$path/tree.tre"
      val group = s"$path/group.txt"
      request.body.file("tree").get.ref.moveTo(matrix.toFile)
      val groupF = if (params("isgroup") == "TRUE") {
        request.body.file("group").get.ref.moveTo(group.toFile)
        s" -g $group"
      } else " "
      val exec = new ExecCommand()
      val cmd = s"Rscript ${Global.toolsPath}/tree/tree2.0.R -i $matrix $groupF -o $path  -if pdf -in treemap"
      exec.exect(cmd, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("treemap", path)
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val drawParams = if (row.drawparams == "") {
      Map( "width" -> "20", "height" -> "23", "dpi" -> "300", "color" -> "#000000:#3A89CC:#769C30:#CD0000:#D99536:#7B0078:#BFBC3B:#6E8B3D:#00688B:#C10077:#CAAA76:#474700:#458B00:#8B4513:#008B8B:#6E8B3D:#8B7D6B:#7FFF00:#CDBA96:#ADFF2F",
        "bw" -> "2", "fs" -> "12", "ss" -> "1", "lsa_width" -> "2", "lsa_height" -> "1", "lfs" -> "26", "ln" -> "1", "sl" -> "TRUE",
        "ssb" -> "FALSE")
    } else {
      Utils.jsonToMap(row.drawparams)
    }

    val groups = if (params("isgroup") == "TRUE") {
      val path = Global.getToolsPath(request.userId, row.id, "treemap")
      "默认," + s"$path/group.txt".readLines.tail.map(_.split("\t").last.trim).distinct.mkString(",")
    } else {
      "默认"
    }

    Json.obj("drawParams" -> drawParams, "title" -> row.name,"groups" -> groups)
  }

  def ReDraw(path: String)(implicit request: Request[AnyContent]): (Int, String, JsValue) = {
    var state = 0
    var msg = "Treemap Success!"
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    try {
      val groupF = s"$path/group.txt"
      val group = if (groupF.toFile.exists()) s" -g $groupF " else ""

      val command = s"Rscript ${Global.toolsPath}/tree/tree2.0.R -i $path/tree.tre $group -o $path -if pdf -in treemap " +
        s" -is ${data("width")}:${data("height")} -cs ${data("color")} -dpi ${data("dpi")} -bw ${data("bw")} " +
        s" -fs ${data("fs")} -ss ${data("ss")} -lsa ${data("lsa_width")}:${data("lsa_height")} -lfs ${data("lfs")} " +
        s" -ln ${data("ln")} -sl ${data("sl")} -ssb ${data("ssb")}"

      val exec = new ExecCommand()
      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("treemap", path)
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }


}
