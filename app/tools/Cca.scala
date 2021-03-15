package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToPng, Utils}

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
      val group = if (isgroup == "TRUE") {
        request.body.file("group").get.ref.moveTo(s"$path/group.txt".toFile)
        s" -g $path/group.txt"
      }
      else ""
      val exec = new ExecCommand()
      val dataCmd = s"Rscript ${Global.toolsPath}/cca/rda_cca_data.R -pi $otu -ei $env -o $path -m $anatype"
      val plotCmd = s"Rscript ${Global.toolsPath}/cca/rda_cca_plot.R -sai $path/samples.xls -spi $path/species.xls  " +
        s"-ei $path/envi.xls  -pci $path/percent.xls -o $path -is 15:15 " + group
      exec.exect(Array(dataCmd, plotCmd), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        val pdf = s"$path/rdacca.pdf".toFile
        val png = s"$path/rdacca.png".toFile
        if (!Global.isWindow) {
          val exec2 = new ExecCommand
          val convert = s"convert -density 300 $pdf $png"
          exec2.exect(convert, path)
        }else{
          PdfToPng.pdf2Png(pdf,png)
        }
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val params = Utils.jsonToMap(row.params)
    val drawParams = if (row.drawparams == "") {
      val color = if (params("isgroup") == "TRUE") "#336666:#996633:#CCCC33:#336633:#990033:#FFCC99:#333366:#669999:#996600" else "#1E90FF"
      val anatype = params("anatype")
      Map("xdata" -> (anatype + "1"), "ydata" -> (anatype + "2"), "xaxis" -> "0", "yaxis" -> "0", "samsize" -> "6",
        "color" -> color, "showsname" -> "true", "samfont" -> "7", "showevi" -> "true", "envColor" -> "#E41A1C",
        "evifont" -> "7", "envLineColor" -> "#E41A1C", "showspecies" -> "true", "speciesfont" -> "7", "speciessize" -> "6",
        "speciesColor" -> "#FF8C00", "width" -> "15", "length" -> "15", "dpi" -> "300", "xts" -> "16", "yts" -> "16",
        "xls" -> "18", "yls" -> "18", "lts" -> "15", "lms" -> "19", "ms" -> "12", "mstext" -> "")
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
      if (g.length == sample.length) g.distinct else g.distinct :+ "nogroup"
    } else mutable.Buffer("nogroup")
    Json.obj("group" -> group, "cols" -> col, "color" -> color, "drawParams" -> drawParams, "title" -> row.name)
  }

  def ReDraw(implicit request: Request[AnyContent]): (Int, String, JsValue) = {
    var state = 0
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    val id = data("id").toInt
    val tools = data("tools")
    var msg = "CCA Success!"

    try {
      val path = Global.getToolsPath(request.userId, id, tools)

      val xyr = data("xdata").drop(3) + ":" + data("ydata").drop(3)
      val sname = s" -sspt TRUE:${data("showname").toUpperCase}"

      val groupF = s"$path/group.txt"
      val isgroup = groupF.toFile.exists()

      val sat =
        if (!isgroup && data("color") != "")
          " -sat " + data("color") + ":" + data("samfont")
        else " -sat #1E90FF:" + data("samfont")
      val gc = if (isgroup) " -gc " + data("color") + "" else ""
      val group =
        if (isgroup) " -g " + groupF else ""
      val showevi = data("showevi").toUpperCase
      val evi = s" -sepl $showevi:$showevi"
      val showspeci = data("showspecies").toUpperCase
      val speci = s" -sppt $showspeci:$showspeci"

      val ms = if (data("mstext") != "") s" -ms sans:bold.italic:${data("ms")}:${data("mstext")} " else ""

      val command =
        s"Rscript ${Global.toolsPath}/cca/rda_cca_plot.R -sai $path/samples.xls -spi $path/species.xls -ei $path/envi.xls " +
          s"-pci $path/percent.xls -o $path $group $gc -is ${data("width")}:${data("length")} -xyr $xyr -op " +
          s"${data("xaxis")}:${data("yaxis")} $sname -sap #000000:${data("samsize")} $sat $evi -ett " +
          s"${data("envColor")}:${data("evifont")} -elc ${data("envLineColor")} $speci -spp ${data("speciesColor")}:${data("speciessize")} " +
          s"-spt ${data("speciesColor")}:${data("speciesfont")} -dpi ${data("dpi")} -xts  sans:bold.italic:${data("xts")} " +
          s"-yts  sans:bold.italic:${data("yts")} -xls  sans:bold.italic:${data("xls")} -yls  sans:bold.italic:${data("yls")} " +
          s" -lts sans:bold.italic:${data("lts")}  -lms sans:bold.italic:${data("lms")} $ms"


      val exec = new ExecCommand()
      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        val pdf = s"$path/rdacca.pdf".toFile
        val png = s"$path/rdacca.png".toFile
        if (!Global.isWindow) {
          val exec2 = new ExecCommand
          val convert = s"convert -density 300 $pdf $png"
          exec2.exect(convert, path)
        } else {
          PdfToPng.pdf2Png(pdf, png)
        }
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data)
    (state, msg, newDrawParams)
  }


}
