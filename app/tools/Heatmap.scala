package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToPng, Utils}

import scala.collection.mutable

object Heatmap extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "Heatmap Success!"
    try {
      val matrix = s"$path/matrix.txt"
      request.body.file("matrix").get.ref.moveTo(matrix.toFile)

      val rowclu = if (params("cluster_rows") != "file") {
        s" -crw ${params("cluster_rows")} -crm ${params("crm")}"
      } else {
        val file = s"$path/treer.txt"
        request.body.file("inputrow").get.ref.moveTo(file.toFile)
        s" -trf $file "
      }

      val colclu = if (params("cluster_cols") != "file") {
        s" -ccl ${params("cluster_cols")} -ccm ${params("ccm")}"
      } else {
        val file = s"$path/treec.txt"
        request.body.file("inputcol").get.ref.moveTo(file.toFile)
        s" -tcf $file "
      }

      val ari = getParams(params("isgroupr"), "groupr", path, "rowgroup", "ari")
      val aci = getParams(params("isgroupc"), "groupc", path, "colgroup", "aci")
      val lfi = getParams(params("istag"), "tag", path, "tag", "lfi")

      val inr = if (params("inr") != "") s" -inr ${params("inr")} " else ""
      val inc = if (params("inc") != "") s" -inc ${params("inc")} " else ""

      val command = s"Rscript ${Global.toolsPath}/heatmap/heatMap.R -i $matrix -o $path $rowclu $colclu " +
        s" $ari $aci $lfi $inr $inc  -lg ${params("lg")} -sc ${params("sc")} -sn ${params("hasrname")}:${params("hascname")}:${params("hasnum")} " +
        s" -c ${params("color")} -cbc ${params("hasborder")} -if pdf -cln TRUE -fn 8"

      val exec = new ExecCommand()

      exec.exect(command, path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      }else {
        val pdf = s"$path/heatmap.pdf".toFile
        val png = s"$path/heatmap.png".toFile
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

  def getParams(pos: String, filename: String, path: String, key: String, param: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): String = {
    if (pos == "TRUE") {
      val file = s"$path/$filename.txt"
      request.body.file(key).get.ref.moveTo(file.toFile)
      s" -$param $file "
    } else ""
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]): JsObject = {
    val params = Utils.jsonToMap(row.params)
    val path = Global.getToolsPath(request.userId, row.id, row.tool)
    val drawParams = if (row.drawparams == "") {
      val groupColor = if (s"$path/color_zhushi.xls".toFile.exists()) {
        s"$path/color_zhushi.xls".readLines.tail.map { x =>
          val line = x.replaceAll("\"", "").split("\t")
          line(1) + "-" + line(2) + ":" + line(3)
        }.mkString(",")
      } else ""
      Map("smt" -> "full", "cluster_rows" -> params("cluster_rows"), "crm" -> params("crm"),
        "rp" -> "1", "cluster_cols" -> params("cluster_cols"), "ccm" -> params("ccm"), "cp" -> "1", "inr" -> params("inr"),
        "inc" -> params("inc"), "sc" -> params("sc"), "lg" -> params("lg"), "color" -> params("color"),"color[]" -> params("color"), "cc" -> "30", "nc" -> "#DDDDDD",
        "hasborder" -> params("hasborder"), "cbc" -> "#ffffff", "hasnum" -> params("hasnum"), "hasrname" -> params("hasrname"),
        "hascname" -> params("hascname"), "rtree" -> "50", "ctree" -> "50", "xfs" -> "10", "yfs" -> "10", "xfa" -> "90",
        "fn" -> "8", "groupColor" -> groupColor)
    } else {
      Utils.jsonToMap(row.drawparams)
    }
    Json.obj("drawParams" -> drawParams, "title" -> row.name)
  }

  def ReDraw(path: String)(implicit request: Request[AnyContent]) = {
    var state = 0
    var msg = "Heatmap Success!"
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    var groupColor = ""
    try {
      val zhushi = if (s"$path/color_zhushi.xls".toFile.exists()) {
        val gcolor = data("gcolor[]").split(":")
        s"$path/color_zhushi.xls".readLines.tail.zipWithIndex.map { case (x, i) =>
          val line = x.replaceAll("\"", "").split("\t")
          (line(1) + "-" + line(2), gcolor(i))
        }
      } else mutable.Buffer()

      groupColor = zhushi.map(x => x._1 + ":" + x._2).mkString(",")

      val groupr = s"$path/groupr.txt"
      val (ari, acrs) = if (groupr.toFile.exists()) {
        val names = groupr.readLines.head.split("\t").tail.map(_.trim)
        val acr = names.map { x =>
          val gc = zhushi.filter(_._1.contains(x)).map(_._2).mkString(":")
          s"$x@$gc"
        }.mkString(",")
        (s" -ari $groupr ", s" -acrs $acr ")
      } else {
        ("", "")
      }

      val groupc = s"$path/groupc.txt"
      val (aci, accs) = if (groupc.toFile.exists()) {
        val names = groupc.readLines.head.split("\t").tail.map(_.trim)
        val acc = names.map { x =>
          val gc = zhushi.filter(_._1.contains(x)).map(_._2).mkString(":")
          s"$x@$gc"
        }.mkString(",")
        (s" -aci $groupc ", s" -accs $acc ")
      }
      else {
        ("", "")
      }

      val lfi = if (s"$path/tag.txt".toFile.exists()) s" -lfi $path/tag.txt " else ""
      val inr = if (data("inr") != "") s" -inr ${data("inr")} " else ""
      val inc = if (data("inc") != "") s" -inc ${data("inc")} " else ""

      val rowclu = if (data("cluster_rows") != "file") {
        s" -crw ${data("cluster_rows")} -crm ${data("crm")} -rp ${data("rp")}"
      } else if (data("cluster_rows") != "TRUE") {
        s" -trf $path/treer.txt -rp ${data("rp")}"
      } else {
        s" -crw ${data("cluster_rows")}"
      }

      val colclu = if (data("cluster_cols") != "file") {
        s" -ccl ${data("cluster_cols")} -ccm ${data("ccm")} -cp ${data("cp")}"
      } else if (data("cluster_cols") != "TRUE") {
        s" -tcf $path/treec.txt -cp ${data("cp")}"
      } else {
        s" -ccl ${data("cluster_cols")}"
      }

      val command = s"Rscript ${Global.toolsPath}/heatmap/heatMap.R -i $path/matrix.txt -o $path -smt ${data("smt")} $ari " +
        s"$aci $lfi $rowclu $colclu $inr $inc -sc ${data("sc")} -lg ${data("lg")} -c ${data("color[]")} -cc ${data("cc")} " +
        s"-nc ${data("nc")} -cbc ${data("cbc")} -sn ${data("hasrname")}:${data("hascname")}:${data("hasnum")} " +
        s" -th ${data("rtree")}:${data("ctree")} -fs ${data("yfs")}:${data("xfs")} -if pdf -cln TRUE -xfa ${data("xfa")} " +
        s"-fn ${data("fn")} $acrs $accs"

      val exec = new ExecCommand()
      exec.exect(Array(command), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        val pdf = s"$path/heatmap.pdf".toFile
        val png = s"$path/heatmap.png".toFile
        if (!Global.isWindow) {
          val exec2 = new ExecCommand
          val convert = s"convert -density 300 $pdf $png"
          exec2.exect(convert, path)
        }else{
          PdfToPng.pdf2Png(pdf,png)
        }
        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val newDrawParams = Json.toJson(data ++ Map("groupColor" -> groupColor))
    (state, msg, newDrawParams)
  }

}
