package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

import scala.collection.mutable

/**
 * Created By Xwq
 * Lefse 分析
 */
object Lefse extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String])(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = "Lefse Success!"
    try {
      val otuBiom = s"$path/otu_taxa_table.biom"
      val otuTxt = s"$path/otu.txt"
      val otu = request.body.file("otu").get

      val cmd = if (otu.filename.endsWith("biom")) {
        otu.ref.moveTo(otuBiom.toFile)
        ""
      } else {
        otu.ref.moveTo(otuTxt.toFile)
        s"biom convert -i $otuTxt -o $otuBiom --table-type='OTU table' --to-json --process-obs-metadata taxonomy "
      }

      val group = s"$path/group.txt"
      request.body.file("group").get.ref.moveTo(group.toFile)

      val runlefse = s"${Global.toolsPath}/lefse2.0/lefse_to_export/run_lefse.py $path/lefse_format.txt $path " +
        s" -a ${params("a")} -w ${params("w")} -l ${params("l")} -y ${params("y")} "
      val sum_level = (1 to params("level").toInt).mkString(",")
      val toolPath = s"${Global.toolsPath}/lefse2.0/lefse_to_export"
      val command =
        s"""
           |export PATH=/usr/bin/:$$PATH
           |$cmd
           |chmod -R 777 $group
           |dos2unix $group
           |chmod -R 777 $otuBiom
           |summarize_taxa.py -i $otuBiom -o $path/tax_summary_a -L $sum_level -a
           |$toolPath/plot-lefse.pl -i $path/tax_summary_a/ -o $path -m $group -g Group -l ${params("level")}
           |$toolPath/format_input.py $path/lefse_input.txt $path/lefse_format.txt -f r -c 1 -u 2 -o 1000000
           |$runlefse
           |/mnt/sdb/xwq/tools/R-4.0.2/bin/Rscript $toolPath/ida_filter.R -i $path/lefse_LDA.xls -o $path/lefse_LDA_diff.xls
           |$toolPath/plot_res.py $path/lefse_LDA.xls $path/lefse_LDA.pdf  --dpi 300 --format pdf --width 10
           |$toolPath/plot_cladogram.py $path/lefse_LDA.xls $path/lefse_LDA.cladogram.pdf  --format pdf --dpi 300 --max_lev 6 --class_legend_font_size 8 --right_space_prop 0.15
           |$toolPath/plot_features.py $path/lefse_format.txt $path/lefse_LDA.xls $path/lefse_LDA.features.pdf --format pdf --dpi 300 --width 13
           |""".stripMargin
      val exec = new ExecCommand()

      exec.execShRun(List(command), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getLastStr
      }
    } catch {
      case e: Exception => state = 2; msg = e.toString
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow)(implicit request: Request[AnyContent]) = {
    val drawParams = Utils.jsonToMap(row.drawparams)

    val path = Global.getToolsPath(request.userId, row.id, "lefse")
    val res = Utils.jsonToMap(drawParams("res"))
    val cla = Utils.jsonToMap(drawParams("cla"))
    val fea = Utils.jsonToMap(drawParams("fea"))

    val fname = s"$path/lefse_LDA_diff.xls".readLines.map {
      _.replaceAll("\"", "").split("\t").head
    }.sorted

    Json.obj("res" -> res, "cla" -> cla, "fea" -> fea, "fname" -> fname, "title" -> row.name)
  }

  def ReDraw(path: String, row: ToolsRow)(implicit request: Request[AnyContent]): (Int, String, JsValue) = {
    var state = 0
    var msg = "Lefse Success!"
    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    try {
      val toolsPath = s"${Global.toolsPath}/lefse2.0/lefse_to_export"
      val command = data("img") match {
        case "res" =>
          val title = data("restitle") match {
            case "" => ""
            case x => s" --title $x "
          }
          val lsrs = data("resorientation") match {
            case "v" => " --left_space 0.02 --right_space 0.98 "
            case _ => ""
          }
          s"$toolsPath/plot_res.py $path/lefse_LDA.xls $path/lefse_LDA.pdf --format " +
            s" pdf --feature_font_size ${data("resffs")} --dpi ${data("resdpi")} $title --title_font_size " +
            s" ${data("restfs")} --width ${data("reswidth")} --height ${data("resheight")} --orientation " +
            s" ${data("resorientation")} $lsrs --subclades ${data("resnscl")} --class_legend_font_size " +
            s" ${data("rescfs")} --max_feature_len ${data("resmfl")}"
        case "cla" =>
          val clasc = data("clasc") match {
            case "" => ""
            case x => s" --sub_clade $x"
          }
          s"$toolsPath/plot_cladogram.py $path/lefse_LDA.xls $path/lefse_LDA.cladogram.pdf " +
            s" --format pdf $clasc --expand_void_lev ${data("claevl")} --max_lev ${data("claml")} --clade_sep ${data("clacs")} " +
            s" --radial_start_lev ${data("clarsl")} --labeled_start_lev ${data("clalstartl")}  --labeled_stop_lev ${data("clalstopl")} " +
            s" --abrv_start_lev ${data("claastartl")} --abrv_stop_lev ${data("claastopl")} --max_point_size  ${data("clamaxps")} " +
            s" --min_point_size ${data("claminps")} --alpha ${data("claalpha")} --point_edge_width ${data("clapew")}" +
            s" --siblings_connector_width  ${data("clascw")} --parents_connector_width  ${data("clapcw")} --right_space_prop " +
            s" ${data("clarsp")} --title  ${data("clatitle")}    --title_font_size ${data("clatfs")}  --label_font_size " +
            s" ${data("clalfs")} --class_legend_font_size  ${data("claclfs")} --dpi  ${data("cladpi")} --class_legend_vis " +
            s" ${data("claclv")} "
        case "fea" =>
          val fname = data("feaf") match {
            case "one" => s" --feature_name ${data("feafname")}"
            case _ => ""
          }
          s"$toolsPath/plot_features.py $path/lefse_format.txt $path/lefse_LDA.xls $path/lefse_LDA.features.pdf --format " +
            s" pdf -f ${data("feaf")} $fname --width ${data("feawidth")} --height ${data("feaheight")} --top ${data("featop")} " +
            s" --bot ${data("feabot")} --title_font_size ${data("featfs")} --class_font_size  ${data("feacfs")} --subcl_mean " +
            s" ${data("feasmean")} --subcl_median ${data("feasmedian")} --font_size  ${data("feafs")} --dpi  ${data("feadpi")} " +
            s" --class_angle ${data("feaca")}"
      }

      val cmd =
        s"""
           |export PATH=/usr/bin/:$$PATH
           |$command
           |""".stripMargin

      val exec = new ExecCommand()
      exec.execShRun(List(cmd), path)
      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        state = 1
      }
    }
    catch {
      case e: Exception => state = 2; msg = e.getMessage
    }

    val newDrawParams = Json.toJson(Utils.jsonToMap(row.drawparams) ++ Map(data("img") -> Utils.mapToJson(data)))
    (state, msg, newDrawParams)
  }


}
