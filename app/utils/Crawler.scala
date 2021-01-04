package utils

import cn.edu.hfut.dmic.webcollector.model.{CrawlDatums, Page}
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler
import config.MyFile
import org.apache.commons.io.FileUtils

object Crawler extends MyFile {

  var fileName = ""

  class Crawler(crawlPath: String, autoParse: Boolean) extends BreadthCrawler(crawlPath, autoParse) {
    override def visit(page: Page, next: CrawlDatums): Unit = {

      try {

        val html = page.select(".article-content-wraper", 0) + "\n"
        val text = page.selectText(".article-content-wraper", 0) + "\n"

        FileUtils.writeStringToFile(s"D:/结直肠癌诊疗系统/cancer123/drug/html/$fileName.txt".toFile, html, true)
        FileUtils.writeStringToFile(s"D:/结直肠癌诊疗系统/cancer123/drug/text/$fileName.txt".toFile, text, true)
      } catch {
        case e: Exception => println(fileName)
      }

    }
  }


  def main(args: Array[String]): Unit = {


    val lines = "D:\\结直肠癌诊疗系统\\cancer123\\genes/genes.txt".readLines
    lines.take(1).foreach { x =>
      val line = x.split("\t")
      fileName = line.head
      s"D:/结直肠癌诊疗系统/cancer123/genes/html/$fileName.txt".delete
      s"D:/结直肠癌诊疗系统/cancer123/genes/text/$fileName.txt".delete

      val code = new Crawler("crawle", false)
      code.setThreads(10)
      code.addSeed("http://www.cancer123.com" + line.last)
      code.start(1)


    }

  }


  def getDrug = {
    val filter = Array(
      "Bexarotene",
      "Pictilisib",
      "Saridegib",
      "Tositumomab")

    val lines = "D:\\结直肠癌诊疗系统/cancer2.txt".readLines
    lines.filter(x=> filter.contains(x.split("\t").head)).foreach { x =>
      val line = x.split("\t")
      fileName = line.head
      s"D:/结直肠癌诊疗系统/cancer123/drug/html/$fileName.txt".delete
      s"D:/结直肠癌诊疗系统/cancer123/drug/text/$fileName.txt".delete

      val code = new Crawler("crawle", false)
      code.setThreads(10)
      code.addSeed("http://www.cancer123.com" + line.last)
      code.addSeed("http://www.cancer123.com" + line.last + "/Drug-Dispensatory.html")
      code.addSeed("http://www.cancer123.com" + line.last + "/Drug-Clinical-guide.html")
      code.addSeed("http://www.cancer123.com" + line.last + "/Drug-Resistance.html")
      code.addSeed("http://www.cancer123.com" + line.last + "/Drug-Side-effect.html")
      code.addSeed("http://www.cancer123.com" + line.last + "/Drug-Encyclopedia.html")
      code.start(1)


    }
  }

}

