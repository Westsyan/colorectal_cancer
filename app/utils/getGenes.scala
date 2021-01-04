package utils

import cn.edu.hfut.dmic.webcollector.model.{CrawlDatums, Page}
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler
import config.MyFile
import org.apache.commons.io.FileUtils


object getGenes extends MyFile {

  var fileName = ""

  class Crawler(crawlPath: String, autoParse: Boolean) extends BreadthCrawler(crawlPath, autoParse) {
    override def visit(page: Page, next: CrawlDatums): Unit = {

      try {

        val intro = page.selectText(".gene-header-about", 0)

        val fr = page.selectTextList("tr")

        val name = fr.get(0).split("：").last.trim
        val aname = fr.get(1).split("：").last.trim
        val geneid = fr.get(2).split("：").last.trim
        val position = fr.get(3).replaceAll("　", " ").split("：").last.split(":")
        val chr = position.head.trim.split(" ")(1)
        val start = position(1).trim.split(" ").head
        val end = position(2).trim.split(" ").head
        val strand = position(3).trim
        val acess = (fr.get(4)+" ").split("：").last

        val buffer = Array(name,aname,geneid,chr,start,end,strand,acess,intro).mkString("\t")

        FileUtils.writeStringToFile("D:\\结直肠癌诊疗系统\\cancer123\\genes/test.txt".toFile,buffer+"\n",true)
      } catch {
        case e: Exception => println(page.url())
      }

    }
  }


  def main(args: Array[String]): Unit = {
    val lines = "D:\\结直肠癌诊疗系统\\cancer123\\genes/genes.txt".readLines
    val code = new Crawler("crawle", false)
    code.setThreads(10)
    val filters = Array("/genes/CBFA2T3",
      "/genes/EPPK1",
    "/genes/FAN1",
    "/genes/GREM1",
    "/genes/IGF1R",
    "/genes/KDM5D",
    "/genes/KLLN",
    "/genes/KMT5A",
    "/genes/MB21D2",
    "/genes/MEIS1",
    "/genes/MYCNOS",
    "/genes/NR4A2",
    "/genes/NR3C1",
    "/genes/PDGFRB",
    "/genes/PRKCB",
    "/genes/PSMB2",
    "/genes/PTPRF",
    "/genes/RAD51C",
    "/genes/RASA1",
    "/genes/RBM10",
    "/genes/SMCHD1",
    "/genes/STAT5A",
    "/genes/ZNF521")

    filters.foreach { x =>

      code.addSeed("http://www.cancer123.com" + x)
    }

    code.start(1)
  }
}

