package tools

import java.io.File

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

/**
 * Created By Xwq
 * 三原图
 */
object Ternary extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "Ternary Success!"
    try {
      val otu = s"$path/otu.txt"
      val group = s"$path/group.txt"
      val tag = s"$path/tag.txt"
      request.body.file("otu").get.ref.moveTo(otu.toFile)
      request.body.file("group").get.ref.moveTo(group.toFile)
      request.body.file("tag").get.ref.moveTo(tag.toFile)

      val exec = new ExecCommand()
      val cmd = s"Rscript ${Global.toolsPath}/ternary/ternary.R -i $otu -g $group -t $tag -o $path -in ternary  -if pdf -psz ${params("psz")}"
      exec.exect(cmd, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("ternary", path)
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val drawParams = if (row.drawparams == "") {
      Map("color" -> "#CD0000:#3A89CC:#769C30:#D99536:#7B0078:#BFBC3B:#E2609F:#00688B:#C10077:#4daf4a:#984ea3:#a65628:#999999",
        "psz" -> params("psz"), "ml" -> "", "mlpos" -> "0.5", "xl" -> "", "yl" -> "", "zl" -> "", "ll" -> "", "width" -> "7",
        "height" -> "7", "dpi" -> "300")
    } else {
      Utils.jsonToMap(row.drawparams)
    }
    val path = Global.getToolsPath(request.userId, row.id, "ternary")
    val group = s"$path/tag.txt".readLines.map(_.split("\t").last).tail.distinct.sorted

    Json.obj("drawParams" -> drawParams, "title" -> row.name, "group" -> group)
  }

  def ReDraw(path: String)(implicit request: Request[AnyContent]): (Int, String, JsValue) = {
    var state = 0
    var msg = "FH Success!"
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    try {

      val xl = if (data("xl") == "") " " else s" -xl ${data("xl")} "
      val yl = if (data("yl") == "") " " else s" -yl ${data("yl")}"
      val zl = if (data("zl") == "") " " else s" -zl ${data("zl")} "
      val ml = if (data("ml") == "") " " else data("ml")
      val ll = if (data("ll") == "") " " else s" -ll ${data("ll")} "

      val command = s"Rscript ${Global.toolsPath}/ternary/ternary.R  -i $path/otu.txt -g $path/group.txt -t $path/tag.txt" +
        s" -o $path -in ternary -if pdf -pcs ${data("color")}  -psz ${data("psz")} -ml $ml:${data("mlpos")} $xl $yl $zl $ll " +
        s" -is ${data("width")}:${data("height")} -dpi ${data("dpi")}"

      val exec = new ExecCommand()
      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("ternary", path)
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }


}
