package controllers

import java.nio.file.Files

import config.{MyConfig, MyMatrix}
import dao.MlDao
import javax.inject._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, AnyContent, ControllerComponents, MultipartFormData, Request}
import utils.{EncodingDetect, ExecCommand, Global, Utils}
import models.Tables._
import org.apache.commons.io.FileUtils
import play.api.libs.Files.TemporaryFile

import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.concurrent.{ExecutionContext, Future}
import utils.EncodingDetect._

import scala.collection.immutable.Range.BigDecimal
import scala.util.parsing.json.JSON

@Singleton
class MlController @Inject()(mlDao: MlDao, cc: ControllerComponents)
                            (implicit execu: ExecutionContext) extends AbstractController(cc) with MyConfig with MyMatrix {

  def getUserId[T](request: Request[T]): Int = {
    request.session.get("id").get.toInt
  }

  def mlIndex = Action { implicit request =>
    Ok(views.html.cn.ml.index())
  }

  def mlIndexEn = Action { implicit request =>
    Ok(views.html.en.ml.index())
  }

  def modelPage(ml: String) = Action { implicit request =>
    Ok(views.html.cn.ml.modelPage(ml))
  }

  def modelPageEn(ml: String) = Action { implicit request =>
    Ok(views.html.en.ml.modelPage(ml))
  }

  def modelResult(ml: String) = Action { implicit request =>
    Ok(views.html.cn.ml.modelResult(ml))
  }

  def modelResultEn(ml: String) = Action { implicit request =>
    Ok(views.html.en.ml.modelResult(ml))
  }

  def modelPredicted(ml: String) = Action { implicit request =>
    Ok(views.html.cn.ml.modelPredicted(ml))
  }

  def modelPredictedEn(ml: String) = Action { implicit request =>
    Ok(views.html.en.ml.modelPredicted(ml))
  }

  case class nameData(name: String)

  val nameForm = Form(
    mapping(
      "name" -> text
    )(nameData.apply)(nameData.unapply)
  )


  def checkName(ml: String) = Action { implicit request =>
    val id = getUserId(request)
    val data = nameForm.bindFromRequest.get
    val valid = mlDao.checkName(ml, id, data.name).toAwait
    Ok(Json.obj("valid" -> (!valid).toString))
  }

  case class naPerData(naPer: Option[String])

  val naPerForm = Form(
    mapping(
      "naPer" -> optional(text)
    )(naPerData.apply)(naPerData.unapply)
  )


  def getAllMl(ml: String) = Action.async { implicit request =>
    mlDao.getByMlAndUserid(getUserId(request), ml).map { x =>
      val json = x.map { y =>
        Utils.getJsonByT(y)
      }
      Ok(Json.toJson(json))
    }
  }

  def getMlDir(row: MlRow): String = {
    s"${Global.path}/data/${row.userid}/ml/${row.ml}/${row.id}"
  }

  def openLog(id: Int) = Action { implicit request =>
    val row = mlDao.getById(id).toAwait
    val dir = getMlDir(row)
    val log = s"$dir/log.txt".readLines
    var html =
      """
        |<style>
        |   .logClass{
        |       font-size : 16px;
        |       font-weight:normal;
        |   }
        |</style>
      """.stripMargin
    html += "<b class='logClass'>" + log.mkString("</b><br><b class='logClass'>") + "</b>"
    Ok(Json.toJson(html))
  }


  def deleteModelById(id: Int) = Action { implicit request =>
    try {
      val row = mlDao.getById(id).toAwait
      val userid = getUserId(request)
      if (userid == row.userid) {
        val dir = getMlDir(row)
        mlDao.deleteById(id)
        dir.delete
      }
    } catch {
      case e: Exception =>
    }
    Ok(Json.toJson("success"))
  }


  def getAllNameByMl(ml: String) = Action { implicit request =>
    val id = getUserId(request)
    val names = mlDao.getByMlAndUserid(id, ml).toAwait.map { x =>
      Json.obj("text" -> x.name, "id" -> x.id.toString)
    }
    Ok(Json.toJson(names))
  }

  def getSuccessByMl(ml: String) = Action { implicit request =>
    val id = getUserId(request)
    val names = mlDao.getSuccessByMlAndUserid(id, ml).toAwait.map { x =>
      Json.obj("text" -> x.name, "id" -> x.id.toString)
    }
    Ok(Json.toJson(names))
  }


  def isDouble(x: String) = {
    try {
      x.toDouble
      true
    } catch {
      case e: Exception => false
    }
  }

  def getModelById(id: Int) = Action { implicit request =>
    val row = mlDao.getById(id).toAwait
    val dir = getMlDir(row)
    val mean = s"$dir/meanDecreaseGini.txt".readLines.asHtmlTableNoHeader
    val dirs = s"/data/${row.userid}/ml/${row.ml}/${row.id}"
    val pre = s"$dir/pre_sum.txt".readLines.drop(7)
    val conf = pre.indexOf("Confusion matrix:")
    val rf = pre.indexOf("RF_prediction")

    val conft = "<table class='table mean'>" + pre.slice(conf + 1, rf - 1).zipWithIndex.map { x =>
      if (x._2 == 0) {
        "\t" + x._1.trim.split(" ").filter(_ != "").mkString("\t")
      } else {
        x._1.trim.split(" ").filter(_ != "").mkString("\t")
      }
    }.asHtmlTableNoHeader + "</table>"
    val rft = "<table class='table mean'>" + pre.drop(rf + 1).map(x => "\t" + x.trim.split(" ").filter(_ != "").mkString("\t")).asHtmlTableNoHeader + "</table>"
    val pret = "<h5>" + pre.take(2).mkString("</h5><h5>") + "</h5>" + conft + "<h5>RF_prediction</h5>" + rft

    Ok(Json.obj("mean" -> mean, "pre" -> pret, "path" -> dirs, "name" -> row.name))
  }


  def getTitleById(id: Int) = Action { implicit request =>
    val row = mlDao.getById(id).toAwait
    val dir = getMlDir(row)
    val json = Utils.jsonToMapSeq(s"$dir/json.txt".readFileToString).init.map { x =>
      if (x("type") == "num") {
        x("name") -> mutable.Buffer("input")
      } else {
        x("name") -> Utils.jsonToMap(x("data")).keys.toBuffer
      }
    }
    Ok(Json.toJson(json))
  }


  def checkMatrixReturnParameter = Action(parse.multipartFormData) { implicit request =>
    val file = request.body.file("file").get
    val f = file.ref.getAbsolutePath
    val encoding = EncodingDetect.getJavaEncode(f)
    val matrix = f.readLines(encoding)
    val head = matrix.head.trim.split("\t").map(disposeTitle(_))
    val check = matrix.count(_.trim.split("\t").length == head.length) == matrix.length
    f.delete
    if (check) {
      Ok(Json.obj("valid" -> "true", "para" -> head))
    } else {
      Ok(Json.obj("valid" -> "false"))
    }

  }

  case class PredictData(id: Int, data: String)

  val PredictForm = Form(
    mapping(
      "id" -> number,
      "data" -> text
    )(PredictData.apply)(PredictData.unapply)
  )

  def runPredict = Action { implicit request =>
    val data = PredictForm.bindFromRequest.get
    val row = mlDao.getById(data.id).toAwait

    val tmpDir = Files.createTempDirectory("tmpDir").toString.unixPath
    val dir = getMlDir(row)

    FileUtils.copyFile(s"$dir/rf_ntree.txt".toFile, s"$tmpDir/rf_ntree.txt".toFile)

    val jsonF = s"$tmpDir/json.txt".toFile
    FileUtils.copyFile(s"$dir/json.txt".toFile, jsonF)

    val jsonSeq = Utils.jsonToMapSeq(jsonF.readFileToString)
    val json = jsonSeq.map { x =>
      x.filter { case (k, v) => !Array("name", "data", "type").contains(k) }.head
    }
    val head = json.map(_._1)

    val d = data.data.trim.split("\t").zipWithIndex.map { x =>
      val j = jsonSeq(x._2)
      if (j("type") == "num") {
        x._1
      } else {
        Utils.jsonToMapDouble(j("data"))(x._1).toString
      }
    }.mkString("\t")


    val predictRow = row.ml match {
      case i if i == "gb" || i == "neural" => mutable.Buffer(head.mkString("\t"), d + "\t-1")
      case _ => mutable.Buffer(head.init.mkString("\t"), d)
    }


    FileUtils.writeLines(s"$tmpDir/predict.txt".toFile, predictRow.asJava)
    val length = data.data.trim.split("\t").length
    val rf_pre = row.ml match {
      case i if i == "gb" || i == "neural" => "prediction = predict(rf_ntree, in_data,type = 'prob')"
      case _ => s"prediction = predict(rf_ntree, in_data[,1:$length],type = 'prob')"
    }

    val writeTable = row.ml match {
      case i if i == "gb" || i == "logistics" => "write.table(t(prediction),file='result.txt',sep='\t',row.names =FALSE, col.names =TRUE,quote=FALSE)"
      case _ => "write.table(prediction,file='result.txt',sep='\t',row.names =FALSE, col.names =TRUE,quote=FALSE)"
    }

    val r =
      s"""|setwd("$tmpDir")
          |library('rminer')
          |in_data <- read.table('predict.txt',header=T,sep="\t",check.names=FALSE,quote="",encoding="UTF-8")
          |load('$tmpDir/rf_ntree.txt')
          |$rf_pre
          |$writeTable
       """.stripMargin
    FileUtils.writeStringToFile(s"$tmpDir/cmd2.r".toFile, r)

    val exec = new ExecCommand

    println(tmpDir)
    exec.exect(s"Rscript $tmpDir/cmd2.r", dir)

    val result = s"$tmpDir/result.txt".readLines.take(2).zipWithIndex.map {
      case (line, index) =>
        if (index == 0) {
          val dependent = Utils.jsonToMapSeq(jsonF.readFileToString).last

          if (dependent("type") == "character") {
            val dataMap = Utils.jsonToMapDouble(dependent("data")).map { case (str, d) => (d -> str) }
            json.last._2 + "\t" + line.split("\t").map(l => dataMap(l.toDouble)).mkString("\t")
          } else {
            json.last._2 + "\t" + line
          }
        } else {
          "Probability\t" + line
        }
    }
    val r1 = result.map(_.split("\t")).transpose

    val r2 = (r1.head +: r1.tail.sortBy(d => scala.math.BigDecimal(d(1))).reverse).map(_.mkString("\t")).asHtmlTableNoHeader
    val re = "<p class=\"card-title\">Result:</p><table id='table' class='table table-border table-hover' style='width:50%;text-align:center;'>" + r2 + "</table>"

      tmpDir.delete
    Ok(Json.toJson(re))

  }

  def runRminer[T](request: Request[MultipartFormData[TemporaryFile]], name: String, ml: String, dependent: String, independent: Seq[String], rCmd: String, naPer: Option[String],fData:filterData) = {
    var valid = "true"
    var msg = ""

    try {
      var state = 1
      var log = ""
      val mlRow = MlRow(0, getUserId(request), name, ml, Utils.date, "", 0)
      val userid = getUserId(request)
      val id = mlDao.insertReturnId(mlRow).toAwait
      val dir = s"${Global.path}/data/$userid/ml/$ml/$id"
      //过滤参数，用于判断是数值类型还是字符类型
      val threshold = 0.7
      Future {
        try {
          dir.mkdirs
          val rf = dir + "/matrix.txt"
          val sourceFile = request.body.file("file").get.ref.getAbsolutePath
          val encoding = getJavaEncode(sourceFile)
          val lines = sourceFile.readLines(encoding)

          var head = lines.head.trim.split("\t").map(_.trim).zipWithIndex

          val headIndex = head.map { x =>
            if (independent.contains(disposeTitle(x._1))) {
              x._2
            } else {
              -1
            }
          }.filter(_ != -1)
          val dependentIndex = head.find(_._1 == dependent).get._2

          val filterData = lines.tail.map(_.split("\t")).transpose.map { x =>
            if (x.count(_.isDouble) / x.length >= threshold) {
              x.map { y =>
                if (y.isDouble) y else "NA"
              }
            } else {
              x.map { y =>
                if (y.trim == "" || y.trim == "-") "NA" else y.replaceAll("\"", "")
              }
            }
          }

          val filterMap = filterData.zipWithIndex.map { case (datas, i) =>
            val m = if (datas.count(_.trim.isDouble) / datas.length >= threshold) {
              Map("type" -> "num")
            } else {
              Map("type" -> "character",
                "data" -> Json.toJson(datas.distinct.zipWithIndex.map { case (data, replaceData) =>
                  data -> replaceData
                }.toMap).toString)
            }
            head(i)._1 -> m
          }.toMap

          val lastData = head.map(_._1).mkString("\t") +: filterData.zipWithIndex.map { case (datas, i) =>
            if (datas.count(_.isDouble) / datas.length >= threshold) {
              datas
            } else {
              val w = scala.util.parsing.json.JSON.parseFull(filterMap(head(i)._1)("data")).get.asInstanceOf[Map[String, Double]]
              datas.map(y => w(y))
            }
          }.transpose.map(_.mkString("\t"))

          val naData = if (naPer.nonEmpty) {
            if (naPer.get.toDouble <= 1 && naPer.get.toDouble >= 0) {
              naPer.get.toDouble
            } else {
              0.7
            }
          } else {
            0.7
          }

          val ll =if(fData.is0 == "yes"){
            lastData.map(_.split("\t").map{y=>
              if(y == "0") "NA" else y
            }.mkString("\t"))
          }else {
            lastData
          }

          val matrix = ll.map(_.split("\t")).transpose.filter { l=>
              l.count(_ != "NA").toDouble / l.length.toDouble >= naData
          }.transpose.map(_.mkString("\t")).map { x =>
            val line = x.split("\t").map(_.trim)
            val line2 = line.zipWithIndex.filter { y =>
              headIndex.contains(y._2)
            }.map(_._1)
            val dependent = line(dependentIndex)
            line2.mkString("\t") + "\t" + dependent
          }

          FileUtils.writeLines(rf.toFile, matrix.asJava)

          head = matrix.head.split("\t").zipWithIndex

          val filterJson = Json.toJson(matrix.map(_.split("\t")).transpose.zipWithIndex.map { case (datas, i) =>
            if (filterMap(datas.head)("type") == "num") {
              Json.obj(("col_" + (i + 1)) -> datas.head, "name" -> datas.head, "type" -> "num")
            } else {
              Json.obj(("col_" + (i + 1)) -> datas.head, "name" -> datas.head, "type" -> "character",
                "data" -> filterMap(datas.head)("data"))
            }
          })

          FileUtils.writeStringToFile(s"$dir/json.txt".toFile, filterJson.toString())

          sourceFile.delete
          val lastName = "col_" + head.length
          val cmd =
            s"""
               |setwd("$dir")
               |library('rminer')
               |library("hash")
               |library("impute")
               |data <- read.table("$dir/matrix.txt", header = T, sep = "\t", check.names = FALSE,quote="",encoding="UTF-8")
               |knn_data <- impute.knn(as.matrix(data))
               |data <- as.data.frame(knn_data$$data)
               |colnames <- colnames(data)
               |nameO2S <- hash()
               |nameS2O <- hash()
               |newColnames <- c()
               |
               |for(c in colnames){
               |newN <- paste("col" , which(colnames == c),sep="_")
               |nameO2S[newN] = c
               |nameS2O[c] = newN
               |newColnames = c(newColnames,newN)
               |}
               |
               |colnames(data) <- newColnames
               |
               |data$$$lastName=as.factor(as.numeric(data$$$lastName))
               |${rCmd.replaceAll(dependent + "~.", lastName + "~.")}
               |C=mmetric(data$$$lastName,M$$cv.fit,metric="CONF")
               |write.table(C$$conf,file='$dir/conf.txt',sep="\t",row.names =TRUE, col.names =TRUE,quote=FALSE)
               |
               |predict <- predict(rf_ntree, data,type = 'prob')
               |predictValue <- apply(predict,1,function(t) as.numeric(colnames(predict)[which.max(t)]))
               |linePlotData <-data.frame(Examples = c(1:length(data$$$lastName)),Values =as.numeric(as.vector(data$$$lastName)),predictValue)
               |write.table(linePlotData,file="reg.txt",sep="\t",row.names =FALSE, col.names =FALSE,quote=FALSE)
               |
               |library(ggplot2)
               |
               |pdf("reg.pdf",width=10,height=10)
               |ggplot(linePlotData) +
               |geom_point(aes(Examples,Values),colour="#00BFC4") +
               |geom_point(aes(Examples,predictValue),colour="#F8766D")
               |dev.off()
               |
               |Imp <- Importance(rf_ntree,data,method="1D-SA")
               |L =list(runs=1,sen=t(Imp$$imp),sresponses=Imp$$sresponses)
               |mgraph(L,graph="IMP",leg=colnames,col="gray",Grid=-1,PDF="imp")
               |
               |library(pROC)
               |roc <- roc(data$$$lastName,predict[,2],ci=TRUE)
               |
               |pdf("roc.pdf",width=10,height=10)
               |plot(roc, print.auc=TRUE, auc.polygon=TRUE, grid=c(0.1, 0.2),ci=TRUE,
               |     grid.col=c("green", "red"), max.auc.polygon=TRUE,
               |     auc.polygon.col="lightblue", print.thres=TRUE)
               |dev.off()
               |
               |write.table(data.frame(colnames[-length(colnames)],Imp$$imp[-length(Imp$$imp)]),file="imp.txt",sep="\t",row.names =FALSE, col.names =FALSE,quote=FALSE)
               |save(rf_ntree,file="$dir/rf_ntree.txt")
       """.stripMargin

          FileUtils.writeStringToFile(s"$dir/cmd.r".toFile, cmd)

          val exec = new ExecCommand

          val convertRoc = s"convert -density 300 $dir/roc.pdf $dir/roc.png"
          val convertRef = s"convert -density 300 $dir/reg.pdf $dir/reg.png"
          val convertImp = s"convert -density 300 $dir/imp.pdf $dir/imp.png"

          if (Global.isWindow) {
            exec.exect(Array(s"Rscript $dir/cmd.r"), dir)
          } else {
            exec.exect(Array(s"Rscript $dir/cmd.r", convertRoc, convertRef, convertImp), dir)
          }


          if (exec.isSuccess) {
            if (filterMap(dependent).contains("data")) {

              val dataMap = Utils.jsonToMapDouble(filterMap(dependent)("data")).map { case (originalValue, replaceValue) => replaceValue -> originalValue }

              val reg = s"$dir/reg.txt".readLines.map { x =>
                val line = x.split("\t")
                val value = dataMap(line(1).toDouble)
                val pValue = dataMap(line(2).toDouble)
                line.head + "\t" + value + "\t" + pValue
              }
              FileUtils.writeLines(s"$dir/reg.txt".toFile, reg.asJava)

              val cv = s"$dir/conf.txt".readLines.zipWithIndex.map { case (conf, index) =>
                val line = conf.split("\t")
                if (index == 0) {
                  line.map(cvName => dataMap(cvName.toDouble)).mkString("\t")
                } else {
                  dataMap(line.head.toDouble) + "\t" + line.tail.mkString("\t")
                }
              }
              FileUtils.writeLines(s"$dir/conf.txt".toFile, cv.asJava)
            }


            state = 1
            log = exec.getOutStr
            mlDao.updateStateById(id, state)
          } else {
            state = 2
            log = exec.getErrStr
          }
        } catch {
          case e: Exception =>
            state = 2
            log = e.getMessage
        } finally {
          mlDao.updateStateById(id, state)
          FileUtils.writeStringToFile(s"$dir/log.txt".toFile, log)
          println(dir)
          // s"$dir/cmd.r".delete
        }
      }
    } catch {
      case e: Exception => valid = "false"
        msg = e.getMessage
    }
    (valid, msg)
  }

  /**
   * 处理标题格式问题
   *
   * @param title
   * @return
   */
  def disposeTitle(title: String) = {
    title.replaceAll("\"", "").trim
  }

  case class filterData(is0:String)

  val filterForm = Form(
    mapping(
      "is0" -> text
    )(filterData.apply)(filterData.unapply)
  )

  case class logisticData(name: String, dependent: String, independent: Seq[String],is0:String)

  val logisticForm = Form(
    mapping(
      "name" -> text,
      "dependent" -> text,
      "independent" -> seq(text),
      "is0" -> text
    )(logisticData.apply)(logisticData.unapply)
  )

  def logisticRun = Action(parse.multipartFormData) { implicit request =>
    var valid = "true"
    var msg = ""
    try {
      val data = logisticForm.bindFromRequest.get
      val filterData = filterForm.bindFromRequest.get

      val cmd =
        s"""
           |rf_ntree <- fit(${data.dependent}~.,data=data,model='multinom',fdebug=TRUE)
           |M=crossvaldata(${data.dependent}~.,data,fit,predict,ngroup=3,model="multinom",task="prob")
           |""".stripMargin
      val naPer = naPerForm.bindFromRequest().get.naPer
      val result = runRminer(request, data.name, "logistics", data.dependent, data.independent, cmd, naPer,filterData)
      valid = result._1
      msg = result._2
    } catch {
      case e: Exception =>
        valid = "false"
        msg = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> msg))
  }

  case class randomForestData(name: String, dependent: String, independent: Seq[String], ntree: String,
                              mtry: Option[String], replace: String, nodesize: Option[String], maxnodes: Option[String],
                              naPer: Option[String])

  val randomForestForm = Form(
    mapping(
      "name" -> text,
      "dependent" -> text,
      "independent" -> seq(text),
      "ntree" -> text,
      "mtry" -> optional(text),
      "replace" -> text,
      "nodesize" -> optional(text),
      "maxnodes" -> optional(text),
      "naPer" -> optional(text)
    )(randomForestData.apply)(randomForestData.unapply)
  )

  def fitRandomForest = Action(parse.multipartFormData) { implicit request =>
    var valid = "true"
    var msg = ""
    try {
      val data = randomForestForm.bindFromRequest.get
      val filterData = filterForm.bindFromRequest.get

      val mtry = if (data.mtry.nonEmpty) s",mtry=${data.mtry.get}" else ""
      val nodesize = if (data.nodesize.nonEmpty) s",nodesize=${data.nodesize.get}" else ""
      val maxnodes = if (data.maxnodes.nonEmpty) s",maxnodes=${data.maxnodes.get}" else ""
      val cmd =
        s"""
           |search=list(ntree=${data.ntree},replace=${data.replace}$mtry$nodesize$maxnodes)
           |rf_ntree <- fit(${data.dependent}~.,data=data,model='randomForest',search=search,fdebug=TRUE)
           |M=crossvaldata(${data.dependent}~.,data,fit,predict,ngroup=3,model="randomForest",task="prob",search=search)
           |""".stripMargin
      val naPer = naPerForm.bindFromRequest().get.naPer
      val result = runRminer(request, data.name, "rf", data.dependent, data.independent, cmd, naPer,filterData)
      valid = result._1
      msg = result._2
    } catch {
      case e: Exception =>
        valid = "false"
        msg = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> msg))
  }


  case class neuralData(name: String, dependent: String, independent: Seq[String], size: String = "3",
                        decay: String = "0", maxit: String = "100", rang: String = "0.7", abstol: String = "1.0e-4",
                        reltol: String = "1.0e-8",maxNWts:String = "1000")

  val neuralForm = Form(
    mapping(
      "name" -> text,
      "dependent" -> text,
      "independent" -> seq(text),
      "size" -> text,
      "decay" -> text,
      "maxit" -> text,
      "rang" -> text,
      "abstol" -> text,
      "reltol" -> text,
      "maxNWts" -> text
    )(neuralData.apply)(neuralData.unapply)
  )

  def neuralRun = Action(parse.multipartFormData) { implicit request =>
    var valid = "true"
    var msg = ""
    try {
      val data = neuralForm.bindFromRequest.get
      val filterData = filterForm.bindFromRequest.get

      val cmd =
        s"""
           |search=list(search=mparheuristic('mlp'),size=${data.size},decay=${data.decay},maxit=${data.maxit},rang=${data.rang},abstol=${data.abstol},reltol=${data.reltol},MaxNWts=${data.maxNWts})
           |rf_ntree <- fit(${data.dependent}~.,data=data,model='mlp',search=search)
           |M=crossvaldata(${data.dependent}~.,data,fit,predict,ngroup=3,model="mlp",task="prob",search=search)
           |""".stripMargin
      val naPer = naPerForm.bindFromRequest().get.naPer
      val result = runRminer(request, data.name, "neural", data.dependent, data.independent, cmd, naPer,filterData)
      valid = result._1
      msg = result._2
    } catch {
      case e: Exception =>
        valid = "false"
        msg = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> msg))
  }

  case class svmData(name: String, dependent: String, independent: Seq[String],
                     kernel: String, c: String = "1", epsilon: String = "0.1", tol: String = "0.01")

  val svmForm = Form(
    mapping(
      "name" -> text,
      "dependent" -> text,
      "independent" -> seq(text),
      "kernel" -> text,
      "c" -> text,
      "epsilon" -> text,
      "tol" -> text
    )(svmData.apply)(svmData.unapply)
  )

  def svmRun = Action(parse.multipartFormData) { implicit request =>
    var valid = "true"
    var msg = ""
    try {
      val data = svmForm.bindFromRequest.get
      val filterData = filterForm.bindFromRequest.get

      val cmd =
        s"""
           |rf_ntree <- fit(${data.dependent}~.,data=data,model='svm',kernel='${data.kernel}', C = ${data.c}, epsilon = ${data.epsilon}, tol = ${data.tol})
           |M=crossvaldata(${data.dependent}~.,data,fit,predict,ngroup=3,model="mlp",task="prob",kernel='${data.kernel}', C = ${data.c}, epsilon = ${data.epsilon}, tol = ${data.tol})
           |""".stripMargin
      val naPer = naPerForm.bindFromRequest().get.naPer
      val result = runRminer(request, data.name, "svm", data.dependent, data.independent, cmd, naPer,filterData)
      valid = result._1
      msg = result._2
    } catch {
      case e: Exception =>
        valid = "false"
        msg = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> msg))
  }

  case class gbData(name: String, dependent: String, independent: Seq[String], eta: String = "0.3",
                    max_depth: String = "6", min_child_weight: String = "1", subsample: String = "1", nrounds: String = "2")

  val gbForm = Form(
    mapping(
      "name" -> text,
      "dependent" -> text,
      "independent" -> seq(text),
      "eta" -> text,
      "max_depth" -> text,
      "min_child_weight" -> text,
      "subsample" -> text,
      "nrounds" -> text
    )(gbData.apply)(gbData.unapply)
  )

  def gbRun = Action(parse.multipartFormData) { implicit request =>
    var valid = "true"
    var msg = ""
    try {
      val data = gbForm.bindFromRequest.get
      val filterData = filterForm.bindFromRequest.get

      val cmd =
        s"""
           |search=list(eta=${data.eta},max_depth=${data.max_depth},min_child_weight=${data.min_child_weight},subsample=${data.subsample})
           |rf_ntree <- fit(${data.dependent}~.,data=data,model='xgboost',nrounds = ${data.nrounds},verbose = 1,search=search)
           |M=crossvaldata(${data.dependent}~.,data,fit,predict,ngroup=3,model="xgboost",task="prob",nrounds = ${data.nrounds},verbose = 1)
           |
           |""".stripMargin
      val naPer = naPerForm.bindFromRequest().get.naPer
      val result = runRminer(request, data.name, "gb", data.dependent, data.independent, cmd, naPer,filterData)
      valid = result._1
      msg = result._2
    } catch {
      case e: Exception =>
        valid = "false"
        msg = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> msg))
  }

  def getRminerModelById(id: Int) = Action { implicit request =>
    val row = mlDao.getById(id).toAwait
    val dir = getMlDir(row)

    val dirs = s"/data/${row.userid}/ml/${row.ml}/${row.id}"

    val mean = s"$dir/imp.txt".readLines.map(_.split("\t")).sortBy(_ (1).toDouble).map(_.mkString("\t")).reverse.asHtmlTableNoHeader

    val reg = s"$dir/reg.txt".readLines.map(_.split("\t"))
    val rate = (reg.count(x => x(1) == x(2)).toDouble / reg.length.toDouble) * 100
    val regTable = reg.map { line =>
      Json.obj("orgin" -> line(1), "predict" -> line(2))
    }

    val conf = if (row.ml != "catboost") {

      s"$dir/conf.txt".readLines.zipWithIndex.map { x =>
        if (x._2 == 0) {
          "Target\t" + x._1
        } else {
          x._1
        }
      }.asHtmlTable
    } else {
      ""
    }

    Ok(Json.obj("mean" -> mean, "name" -> row.name, "path" -> dirs, "reg" -> regTable,
      "rate" -> rate.formatted("%.2f"), "conf" -> conf))
  }

  case class catboostData(name: String, dependent: String, independent: Seq[String],loss_function:String, iterations: String = "100",
                          depth:String="6",learning_rate:Option[String],l2_leaf_reg:String = "3.0")

  val catboostForm = Form(
    mapping(
      "name" -> text,
      "dependent" -> text,
      "independent" -> seq(text),
      "loss_function" -> text,
      "iterations" -> text,
      "depth" -> text,
      "learning_rate" -> optional(text),
      "l2_leaf_reg" -> text
    )(catboostData.apply)(catboostData.unapply)
  )


  def trainCatboost = Action(parse.multipartFormData) { implicit request =>
    var valid = "true"
    var msg = ""
    try {
      var state = 1
      var log = ""
      val data = catboostForm.bindFromRequest.get
      val filterData = filterForm.bindFromRequest.get

      val mlRow = MlRow(0, getUserId(request), data.name, "catboost", Utils.date, "", 0)
      val userid = getUserId(request)
      val id = mlDao.insertReturnId(mlRow).toAwait
      val dir = s"${Global.path}/data/$userid/ml/catboost/$id"
      val threshold = 0.7

      val naPer = naPerForm.bindFromRequest().get.naPer
      Future {
        try {
          dir.mkdirs
          val sourceFile = request.body.file("file").get.ref.getAbsolutePath
          val encoding = getJavaEncode(sourceFile)
          val lines = sourceFile.readLines(encoding)

          val head = lines.head.trim.split("\t").map(_.trim).zipWithIndex
          val headIndex = head.map { x =>
            if (data.independent.contains(x._1)) {
              x._2
            } else {
              -1
            }
          }.filter(_ != -1)
          val dependentIndexOrgin = head.find(_._1 == data.dependent).get._2

          val filterData = lines.tail.map(_.split("\t")).transpose.map { x =>
            if (x.count(_.isDouble) / x.length >= threshold) {
              x.map { y =>
                if (y.isDouble) y else "NA"
              }
            } else {
              x.map { y =>
                if (y.trim == "" || y.trim == "-") "NA" else y.replaceAll("\"", "")
              }
            }
          }

          val filterMap = filterData.zipWithIndex.map { case (datas, i) =>
            val m = if (datas.count(_.trim.isDouble) / datas.length >= threshold) {
              Map("type" -> "num")
            } else {
              Map("type" -> "character",
                "data" -> Json.toJson(datas.distinct.zipWithIndex.map { case (data, replaceData) =>
                  data -> replaceData
                }.toMap).toString)
            }
            head(i)._1 -> m
          }.toMap

          val lastData = head.map(_._1).mkString("\t") +: filterData.zipWithIndex.map { case (datas, i) =>
            if (datas.count(_.isDouble) / datas.length >= threshold) {
              datas
            } else {
              val w = scala.util.parsing.json.JSON.parseFull(filterMap(head(i)._1)("data")).get.asInstanceOf[Map[String, Double]]
              datas.map(y => w(y))
            }
          }.transpose.map(_.mkString("\t"))

          val naData = if (naPer.nonEmpty) {
            if (naPer.get.toDouble <= 1 && naPer.get.toDouble >= 0) {
              naPer.get.toDouble
            } else {
              0.7
            }
          } else {
            0.7
          }

          val matrixData = lastData.filter { x =>
            val l = x.split("\t")
            l.count(_ != "NA").toDouble / l.length.toDouble >= naData
          }.map { x =>
            val line = x.split("\t").map(_.trim)
            val line2 = line.zipWithIndex.filter { y =>
              headIndex.contains(y._2)
            }.map(_._1)
            val dependent = line(dependentIndexOrgin)
            line2.mkString("\t") + "\t" + dependent
          }

          val matrix = dir + "/matrix.txt"

          FileUtils.writeLines(matrix.toFile, matrixData.asJava)

          val filterJson = Json.toJson(matrixData.map(_.split("\t")).transpose.zipWithIndex.map { case (datas, i) =>
            if (filterMap(datas.head)("type") == "num") {
              Json.obj(("col_" + (i + 1)) -> datas.head, "name" -> datas.head, "type" -> "num")
            } else {
              Json.obj(("col_" + (i + 1)) -> datas.head, "name" -> datas.head, "type" -> "character",
                "data" -> filterMap(datas.head)("data"))
            }
          })

          FileUtils.writeStringToFile(s"$dir/json.txt".toFile, filterJson.toString())


          sourceFile.delete

          val header = matrixData.head.split("\t").map(_.trim)
          val cat_matrix = matrixData.tail.map(_.split("\t")).transpose
          val cat_index = cat_matrix.zipWithIndex.filter { x =>
            !x._1.forall(_.isDouble)
          }.map(_._2 + 1)
          val cat_features = "c(" + cat_index.mkString(",") + ")"
          val length = cat_matrix.length
          val dependentIndex = header.indexOf(data.dependent) + 1
          val label = if (cat_index.contains(dependentIndex)) {
            s"""
               |dependentFactor <- factor(train[,$dependentIndex])
               |dependentLevels <- levels(dependentFactor)
               |label <- as.numeric(dependentFactor)
               |train[$dependentIndex] <- label
               |""".stripMargin
          } else {
            s"""
               |label <- train[,$dependentIndex]
               |dependentLevels <- levels(factor(label))
               |""".stripMargin
          }

          val learning_rate = if(data.learning_rate.nonEmpty){
            s",learning_rate=${data.learning_rate}"
          }else{
            ""
          }

          val cmd =
            s"""
               |setwd("$dir")
               |library(catboost)
               |library("hash")
               |library("impute")
               |column_description_vector = rep('numeric', $length)
               |cat_features <- $cat_features
               |
               |
               |for (i in cat_features)
               |  column_description_vector[i] <- 'factor'
               |train <- read.table("$matrix", head = T, sep = "\t", colClasses = column_description_vector, na.strings='NAN')
               |
               |knn_data <- impute.knn(as.matrix(train))
               |train <- as.data.frame(knn_data$$data)
               |colnames <- colnames(train)
               |nameO2S <- hash()
               |nameS2O <- hash()
               |newColnames <- c()
               |
               |for(c in colnames){
               |newN <- paste("col" , which(colnames == c),sep="_")
               |nameO2S[newN] = c
               |nameS2O[c] = newN
               |newColnames = c(newColnames,newN)
               |}
               |
               |colnames(train) <- newColnames
               |$label
               |write.table(data.frame(dependentLevels),file='$dir/level.txt',sep="\t",row.names =FALSE, col.names =FALSE,quote=FALSE)
               |train_pool <- catboost.load_pool(data=train[,-$dependentIndex], label = label)
               |fit_params <- list(iterations = ${data.iterations},loss_function = '${data.loss_function}',depth=${data.depth},l2_leaf_reg=${data.l2_leaf_reg}$learning_rate)
               |model <- catboost.train(train_pool, params = fit_params)
               |library(rminer)
               |mgraph(model$$feature_importances[,1],graph="IMP",leg=colnames,col="gray",Grid=-1,PDF="imp")
               |
               |write.table(data.frame(colnames[-$length],model$$feature_importance[,1]),file='$dir/imp.txt',sep="\t",row.names =FALSE, col.names =FALSE,quote=FALSE)
               |catboost.save_model(model,"$dir/model.cbm")
               |predict <- catboost.predict(model,train_pool,prediction_type = 'RawFormulaVal')
               |
               |predictValue <- apply(predict,1,function(t) as.numeric(dependentLevels[which.max(t)]))
               |linePlotData <-data.frame(Examples = c(1:length(train[,$length])),Values =as.numeric(as.vector(train[,$length])),predictValue)
               |write.table(linePlotData,file="reg.txt",sep="\t",row.names =FALSE, col.names =FALSE,quote=FALSE)
               |
               |
               |
               |library(ggplot2)
               |
               |pdf("reg.pdf",width=10,height=10)
               |ggplot(linePlotData) +
               |geom_point(aes(Examples,Values),colour="#00BFC4") +
               |geom_point(aes(Examples,predictValue),colour="#F8766D")
               |dev.off()
               |
               |library(pROC)
               |roc <- roc(train[,$length],predict[,2],ci=TRUE)
               |
               |pdf("roc.pdf"  ,width=10,height=10)
               |plot(roc, print.auc=TRUE, auc.polygon=TRUE, grid=c(0.1, 0.2),ci=TRUE,
               |     grid.col=c("green", "red"), max.auc.polygon=TRUE,
               |     auc.polygon.col="lightblue", print.thres=TRUE)
               |dev.off()
               |
               |
               |""".stripMargin


          FileUtils.writeStringToFile(s"$dir/cmd.r".toFile, cmd)

          val exec = new ExecCommand
          val convertImp = s"convert -density 300 $dir/imp.pdf $dir/imp.png"
          val convertRoc = s"convert -density 300 $dir/roc.pdf $dir/roc.png"
          val convertReg = s"convert -density 300 $dir/reg.pdf $dir/reg.png"

          if (Global.isWindow) {
            exec.exect(Array(s"Rscript $dir/cmd.r"), dir)
          } else {
            exec.exect(Array(s"Rscript $dir/cmd.r", convertRoc, convertReg, convertImp), dir)
          }

          if (exec.isSuccess) {
            if (filterMap(data.dependent).contains("data")) {

              val dataMap = Utils.jsonToMapDouble(filterMap(data.dependent)("data")).map { case (originalValue, replaceValue) => replaceValue -> originalValue }

              val reg = s"$dir/reg.txt".readLines.map { x =>
                val line = x.split("\t")
                val value = dataMap(line(1).toDouble)
                val pValue = dataMap(line(2).toDouble)
                line.head + "\t" + value + "\t" + pValue
              }
              FileUtils.writeLines(s"$dir/reg.txt".toFile, reg.asJava)

            }


            state = 1
            log = exec.getOutStr
            mlDao.updateStateById(id, state)
          } else {
            state = 2
            log = exec.getErrStr
          }
        } catch {
          case e: Exception =>
            state = 2
            log = e.getMessage
        } finally {
          mlDao.updateStateById(id, state)
          FileUtils.writeStringToFile(s"$dir/log.txt".toFile, log)
          // s"$dir/cmd.r".delete
        }
      }

    } catch {
      case e: Exception => valid = "false"
        msg = e.getMessage
    }
    Ok(Json.obj("valid" -> valid, "msg" -> msg))
  }

  def predictCatboost = Action { implicit request =>
    val data = PredictForm.bindFromRequest.get
    val row = mlDao.getById(data.id).toAwait

    val tmpDir = Files.createTempDirectory("tmpDir").toString.unixPath
    val dir = getMlDir(row)

    val encoding = getJavaEncode(s"$dir/matrix.txt")

    val matrixF = s"$tmpDir/matrix.txt".toFile
    FileUtils.copyFile(s"$dir/matrix.txt".toFile, matrixF)

    val jsonF = s"$tmpDir/json.txt".toFile
    FileUtils.copyFile(s"$dir/json.txt".toFile, jsonF)

    val jsonSeq = Utils.jsonToMapSeq(jsonF.readFileToString)
    val json = jsonSeq.map { x =>
      x.filter { case (k, v) => !Array("name", "data", "type").contains(k) }.head
    }
    val head = json.map(_._1).init

    val d = data.data.trim.split("\t").zipWithIndex.map { x =>
      val j = jsonSeq(x._2)
      if (j("type") == "num") {
        x._1
      } else {
        Utils.jsonToMapDouble(j("data"))(x._1).toString
      }
    }.mkString("\t")

    FileUtils.copyFile(s"$dir/model.cbm".toFile, s"$tmpDir/model.cbm".toFile)
    val matrix = matrixF.readLines(encoding)

    val length = matrix.head.split("\t").length - 1
    val cat_matrix = matrix.tail.map(_.split("\t")).transpose
    val cat_index = cat_matrix.zipWithIndex.filter { x =>
      !x._1.forall(_.isDouble)
    }.map(_._2 + 1)
    val cat_features = "c(" + cat_index.mkString(",") + ")"
    val predict = s"$tmpDir/predict.txt"
    FileUtils.writeLines(predict.toFile, mutable.Buffer(head.mkString("\t"), d).asJava)


    val r =
      s"""|setwd("$tmpDir")
          |library('catboost')
          |column_description_vector = rep('numeric', $length)
          |cat_features <- $cat_features
          |for (i in cat_features)
          |  column_description_vector[i] <- 'factor'
          |predict_data <- read.table("$predict", head = T, sep = "\t", colClasses = column_description_vector, na.strings='NAN')
          |predict_pool <- catboost.load_pool(data=predict_data)
          |model <- catboost.load_model("$tmpDir/model.cbm")
          |predict <- catboost.predict(model,predict_pool,prediction_type = 'RawFormulaVal')
          |write.table(data.frame(predict),file='$tmpDir/result.txt',sep="\t",row.names =FALSE, col.names =TRUE,quote=FALSE)
       """.stripMargin
    FileUtils.writeStringToFile(s"$tmpDir/cmd2.r".toFile, r)

    val exec = new ExecCommand
    exec.exect(s"Rscript $tmpDir/cmd2.r", dir)
    val level = s"$dir/level.txt".readLines.map(_.trim).toArray

    println(tmpDir)
    val result = s"$tmpDir/result.txt".readLines.take(2).zipWithIndex.map {
      case (line, index) =>
        if (index == 0) {
          val dependent = Utils.jsonToMapSeq(jsonF.readFileToString).last

          if (dependent("type") == "character") {
            val dataMap = Utils.jsonToMapDouble(dependent("data")).map { case (str, d) => (d -> str) }
            json.last._2 + "\t" + line.split("\t").zipWithIndex.map(l => dataMap(l._2.toDouble)).mkString("\t")
          } else {
            json.last._2 + "\t" + line
          }
        } else {
          "Probability\t" + line
        }
    }
    val r1 = result.map(_.split("\t")).transpose

    val r2 = (r1.head +: r1.tail.sortBy(d => scala.math.BigDecimal(d(1))).reverse).map(_.mkString("\t")).asHtmlTableNoHeader
    val re = "<p class=\"card-title\">Result:</p><table id='table' class='table table-border table-hover' style='width:50%;text-align:center;'>" + r2 + "</table>"

    tmpDir.delete
    Ok(Json.toJson(re))
  }

  def getCatboostModelById(id: Int) = Action { implicit request =>
    val row = mlDao.getById(id).toAwait
    val dir = getMlDir(row)
    val mean = s"$dir/importance.txt".readLines.map(_.split("\t")).sortBy(_ (1).toDouble).map(_.mkString("\t")).reverse.asHtmlTableNoHeader
    val dirs = s"/data/${row.userid}/ml/${row.ml}/${row.id}"

    Ok(Json.obj("mean" -> mean, "name" -> row.name, "path" -> dirs))
  }


}
