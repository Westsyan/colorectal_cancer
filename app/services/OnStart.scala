package services

import config.MyFile
import javax.inject.{Inject, Singleton}
import utils.{Global, TableUtils}

import scala.collection.mutable

@Singleton
class OnStart @Inject() extends MyFile {

  val seer: mutable.Buffer[Array[String]] = s"${Global.path}/public/seer_db.txt".readLines.map(_.split("\t"))
  val seerHead: Array[String] = seer.head

  TableUtils.seerRow = seer.tail.map { x =>
    x.zipWithIndex.map { y =>
      seerHead(y._2) -> y._1
    }.toMap
  }.toSeq

  println("[info] Seer 数据初始化成功！")


  val tcga: mutable.Buffer[Array[String]] = s"${Global.path}/public/TCGA_data.tsv".readLines.map(_.split("\t"))
  val tcgaHead: Array[String] = tcga.head.map(_.replaceAll("#", "").replaceAll(" ", "").trim)

  TableUtils.tcgaRow = tcga.tail.map { x =>
    x.zipWithIndex.map { y =>
      tcgaHead(y._2) -> y._1
    }.toMap
  }.toSeq

  println("[info] TCGA 数据初始化成功！")

  val ncbi: mutable.Buffer[Array[String]] = s"${Global.path}/public/ncbi.txt".readLines.map(_.split("\t"))

  val head: Array[String] = Array("id", "title", "code", "source","link")

  TableUtils.ncbiRow = ncbi.map { line =>
    line.zipWithIndex.map { case (l, i) =>
      head(i) -> l
    }.toMap
  }.toSeq

}
