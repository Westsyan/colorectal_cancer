package tools

import config.{MyFile, MyRequest}
import models.Tables.ToolsRow
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContent, MultipartFormData, Request}
import utils.{ExecCommand, Global, PdfToImage, Utils}

object IgcOrItc extends MyFile with MyRequest {

  def Run(path: String, params: Map[String, String], tools: String)(implicit request: Request[MultipartFormData[TemporaryFile]]): (Int, String) = {
    var state = 1
    var msg = tools match {
      case "igc" => "Inner-group Correlation Analysis Success!"
      case "itc" => "Intraclass Correlation Analysis Success!"
    }
    try {
      val matrix = s"$path/matrix.txt"
      request.body.file("matrix").get.ref.moveTo(matrix.toFile)
      val cmdInput = tools match {
        case "igc" => s" -i1 $matrix"
        case "itc" =>
          val m2 = s"$path/matrix2.txt"
          request.body.file("matrix2").get.ref.moveTo(m2.toFile)
          s" -i1 $matrix -i2 $m2"
      }

      val cmd =
        s"""
           |Rscript ${Global.toolsPath}/igc/cor_pvalue_calculate.R $cmdInput -o $path -m ${params("anatype")}
           |Rscript ${Global.toolsPath}/igc/node_attr_calculate.R -t $path/pandv.xls -pt 0.1 -ct 0.5 -o $path
           |Rscript ${Global.toolsPath}/heatmap/heatMap.R -i $path/cor.xls -o $path -c "#E41A1C:#FFFF00:#1E90FF" -lfi $path/p_star.xls -if pdf
           |""".stripMargin

      println(cmd)
      val exec = new ExecCommand()
      exec.execShRun(List(cmd), path)

      if (!exec.isSuccess) {
        state = 2
        msg = exec.getErrStr
      } else {
        PdfToImage.pdf2Png("heatmap", path)
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    (state, msg)
  }

  def GetParams(row: ToolsRow, tools: String)(implicit request: Request[AnyContent]): JsObject = {
    val drawParams = Utils.jsonToMap(row.drawparams)
    val path = Global.getToolsPath(request.userId, row.id, tools)
    val net = Utils.jsonToMap(drawParams("net"))
    val heatmap = Utils.jsonToMap(drawParams("heatmap"))
    val (rows, selector) = getNetwork(net, path, tools)
    val head = s"$path/cor.xls".readFileToString.trim.split("\n")
    val rnum = head.length - 1
    val cnum = head(1).trim.split("\t").length - 1
    Json.obj("rows" -> rows, "selector" -> selector, "title" -> row.name, "net" -> net,
      "heatmap" -> heatmap, "rnum" -> rnum, "cnum" -> cnum)
  }

  def ReDraw(path: String, tools: String, row: ToolsRow)(implicit request: Request[AnyContent]) = {
    var state = 0
    var msg = tools match {
      case "igc" => "Inner-group Correlation Analysis Success!"
      case "itc" => "Intraclass Correlation Analysis Success!"
    }

    val data = request.body.asFormUrlEncoded.get.map(x => x._1 -> x._2.mkString(":"))
    try {
      val img = data("img")

      val command = img match {
        case "net" => s"Rscript ${Global.toolsPath}/igc/node_attr_calculate.R -t $path/pandv.xls -pt ${data("pthres")} -ct ${data("cthres")} -o $path"
        case "heatmap" =>
          val lfi = if (data("lfi") == "TRUE") s" -lfi $path/p_star.xls " else ""
          val rowclu = if (data("cluster_rows") == "TRUE") {
            s" -crw ${data("cluster_rows")} -crm ${data("crm")} -rp ${data("rp")}"
          } else {
            s" -crw ${data("cluster_rows")}"
          }
          val colclu = if (data("cluster_cols") == "TRUE") {
            s" -ccl ${data("cluster_cols")} -ccm ${data("ccm")} -cp ${data("cp")}"
          } else {
            s" -ccl ${data("cluster_cols")}"
          }

          val tableFile = s"$path/cor.xls"

          val color = if (data("color") == "#E41A1C:#FFFF00:#1E90FF" || data("color") == "#E41A1C:#FFFFFF:#1E90FF") data("color") else data("color[]")

          s"Rscript ${Global.toolsPath}/heatmap/heatMap.R -i $tableFile -o $path  -smt ${data("smt")} $lfi $rowclu " +
            s" $colclu  -sc ${data("sc")} -lg ${data("lg")} -c $color -cc ${data("cc")} -nc ${data("nc")} " +
            s" -cbc ${data("cbc")} -sn ${data("hasrname")}:${data("hascname")}:${data("hasnum")}  -th " +
            s" ${data("rtree")}:${data("ctree")} -fs ${data("yfs")}:${data("xfs")} -if pdf -cln TRUE -xfa ${data("xfa")} " +
            s" -fn ${data("fn")}"
      }

      val execCommand = new ExecCommand
      execCommand.exect(command, path)
      if (!execCommand.isSuccess) {
        state = 2
        msg = execCommand.getErrStr
      } else {
        if (img == "heatmap") {
          PdfToImage.pdf2Png("heatmap", path)
        }

        state = 1
      }
    } catch {
      case e: Exception => state = 2; msg = e.getMessage
    }
    val drawParams = Utils.jsonToMap(row.drawparams)
    val newDrawParams = drawParams ++ Map(data("img") -> Utils.mapToJson(data))
    (state, msg, Json.toJson(newDrawParams))
  }

  def getNetwork(drawParams: Map[String, String], path: String, tools: String) = {
    val matrix = s"$path/matrix.txt"
    val g = matrix.readLines.head.trim.split("\t").tail

    val e = tools match {
      case "igc" => g
      case "itc" =>
        val m2 = s"$path/matrix2.txt"
        m2.readLines.head.trim.split("\t").tail
    }
    val list = e ++ g

    val nodes = list.distinct.zipWithIndex.map { case (x, id) =>
      val xy = Json.obj("x" -> Math.random() * 500, "y" -> Math.random() * 500)
      val (group, score) =
        tools match {
          case "igc" => ("gene", drawParams("gsize").toDouble)
          case "itc" => if (id < e.drop(1).length) ("evi", drawParams("esize").toDouble)
          else ("gene", drawParams("gsize").toDouble)
        }
      val data = Json.obj("id" -> id, "name" -> x, "score" -> score, "group" -> group)
      Json.obj("data" -> data, "position" -> xy, "group" -> "nodes")
    }

    val result = s"$path/pandv.xls".readLines
    var eid = 0;

    var soutar: List[List[String]] = List(List(""))
    var resultFilter = Array("")
    result.drop(1).foreach { x =>
      val ei = x.split("\"").filter(_.trim != "")
      val source = ei(1)
      val target = ei(2)
      val w = ei(3).trim.split("\t").last.toDouble
      val c = ei(3).trim.split("\t").head.toDouble
      if ((!soutar.contains(List(source, target)) || !soutar.contains(List(target, source))) && source != target
        && w < drawParams("pthres").toDouble && Math.abs(c) < drawParams("cthres").toDouble) {
        soutar = soutar :+ List(source, target) :+ List(target, source)
        resultFilter = resultFilter :+ x
      }
    }

    val edges = resultFilter.drop(1).map { x =>
      val ei = x.split("\"").filter(_.trim != "")
      val source = list.indexOf(ei(1))
      val target = list.indexOf(ei(2))
      eid = eid + 1
      val id = "e" + eid
      val weight = ei(3).trim.split("\t").last.toDouble
      val cc = ei(3).trim.split("\t").head.toDouble
      val lab = "c=" + cc.formatted("%." + drawParams("dot") + "f") + "；p=" + weight.formatted("%." + drawParams("dot") + "f")
      val data = Json.obj("source" -> source, "target" -> target, "weight" -> weight, "label" -> lab)
      Json.obj("data" -> data, "group" -> "edges", "id" -> id)
    }

    val rows = nodes ++ edges
    val node = Json.obj("selector" -> "node", "style" -> Json.obj("width" -> "mapData(score, 0, 10, 10, 100)", "height" -> "mapData(score, 0, 10, 10, 100)", "content" -> "data(name)", "font-size" -> "12px", "text-valign" -> "center", "text-halign" -> "center", "text-outline-width" -> "2px"))
    val font1 = drawParams("gfont") + "px"
    val nodegene = Json.obj("selector" -> "node[group='gene']", "style" -> Json.obj("shape" -> drawParams("gshape"), "background-color" -> drawParams("netcolor1"), "text-outline-color" -> drawParams("netcolor1"), "opacity" -> drawParams("gopa"), "font-size" -> font1, "color" -> drawParams("netcolor2")))
    val font2 = drawParams("efont") + "px"
    val nodeevi = Json.obj("selector" -> "node[group='evi']", "style" -> Json.obj("shape" -> drawParams("eshape"), "background-color" -> drawParams("netcolor3"), "text-outline-color" -> drawParams("netcolor3"), "opacity" -> drawParams("eopa"), "font-size" -> font2, "color" -> drawParams("netcolor4")))
    val nodesele = Json.obj("selector" -> "node:selected", "style" -> Json.obj("border-width" -> "6px", "border-color" -> "#AAD8FF", "border-opacity" -> "0.5", "background-color" -> "#993399", "text-outline-color" -> "#993399"))
    val edgehigh = Json.obj("selector" -> "edge.highlighted", "style" -> Json.obj("line-color" -> "#2a6cd6", "target-arrow-color" -> "#2a6cd6", "opacity" -> 0.7, "label" -> "data(label)", "edge-text-rotation" -> "autorotate"))
    val edge = Json.obj("selector" -> "edge", "style" -> Json.obj("curve-style" -> "haystack", "haystack-radius" -> "0.5", "opacity" -> drawParams("opacity"), "line-color" -> drawParams("netcolor5"), "width" -> "mapData(weight, 0, 1, 1, 8)", "overlay-padding" -> "3px"))

    val selector = Array(node, nodegene, nodeevi, nodesele, edge, edgehigh)

    (rows, selector)
  }


}
