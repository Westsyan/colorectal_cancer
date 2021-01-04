package utils

import java.io.File

import config.MyFile

object Global extends MyFile{

  val cbioportalMap = Map("Colon Adenocarcinoma (CaseCCC, PNAS 2015)" -> "coad_caseccc_2015",
    "Colon Cancer (CPTAC-2 Prospective, Cell 2019)" -> "coad_cptac_2019",
    "Colorectal Adenocarcinoma (DFCI, Cell Reports 2016)" -> "coadread_dfci_2016",
    "Colorectal Adenocarcinoma (Genentech, Nature 2012)" -> "coadread_genentech",
    "Colorectal Adenocarcinoma (TCGA, Firehose Legacy)" -> "coadread_tcga",
    "Colorectal Adenocarcinoma (TCGA, Nature 2012)" -> "coadread_tcga_pub",
    "Colorectal Adenocarcinoma (TCGA, PanCancer Atlas)" -> "coadread_tcga_pan_can_atlas_2018",
    "Colorectal Adenocarcinoma Triplets (MSKCC, Genome Biol 2014)" -> "coadread_mskcc",
    "Metastatic Colorectal Cancer (MSKCC, Cancer Cell 2018)" -> "crc_msk_2017",
    "Rectal Cancer (MSK,Nature Medicine 2019)" -> "rectal_msk_2019")


  val isWindow: Boolean = {
    System.getProperties.getProperty("os.name").toUpperCase().indexOf("WINDOWS") != -1
  }

  val windowsPath = "I:/colorectal_cancer"
  val linuxPath = "/mnt/sdb/xwq/projects/colorectal_cancer"
  val path: String = {
    if (isWindow) windowsPath else linuxPath
  }

  val suffix: String = {
    if (new File(windowsPath).exists()) ".exe" else " "
  }

  def getToolsPath(userId:Int,id:Int,tools:String): String = {
    s"$path/data/$userId/tools/$tools/$id"
  }

  def getLogByPath(path:String):String = {
    val log = s"$path/log.txt".readLines
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
    html
  }

  def random :String = {
    val source = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890"
    var value = ""
    for (i <- 0 to 20){
      val ran = Math.random()*62
      val char = source.charAt(ran.toInt)
      value += char
    }
    value
  }

  val samtools: String = if (isWindow) windowsPath + "/tools/samtools-0.1.19/samtools.exe " else "samtools "

  val tmpPath: String = path + "/tmp"
  val toolsPath: String = path + "/tools"

  val enrichPath: String = path + "/enrichData/"



}
