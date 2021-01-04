package utils

import cn.edu.hfut.dmic.webcollector.model.{CrawlDatums, Page}
import cn.edu.hfut.dmic.webcollector.plugin.berkeley.BreadthCrawler

object getGeneCards {

  var fileName = ""

  class Crawler(crawlPath: String, autoParse: Boolean) extends BreadthCrawler(crawlPath, autoParse) {

    override def visit(page: Page, next: CrawlDatums): Unit = {



        val intro = page.url()

        println(intro)


    }
  }


  def main(args: Array[String]): Unit = {
    val code = new Crawler("crawle", false)
    code.setThreads(10)
    code.setResumable(true)
    println("start")
    code.addSeed("https://www.genecards.org")
    code.start(5)
    println("end")

  }


}
