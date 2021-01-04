package test

import java.io.File

import utils.Utils

import scala.io.Source

object test04 {

  def main(args: Array[String]): Unit = {

  //  val b = new BufferedRandomAccessFile(new File("I:\\colorectal_cancer\\public\\cBioPortal\\coadread_tcga_pan_can_atlas_2018/data_mutations_extended.txt"), "r")

    val time = System.currentTimeMillis()
    println(Utils.getTime(time))

    val names =
      """
        |姓名：薛为琪
        |出生日期：1994/2/5
        |性别：男
        |擅长技能：Scala，javascript,html,css
        |了解技能：Java,R,Perl,Python,Mysql,Linux操作,nginx
        |主要方向：网站开发，数据处理
        |所用框架：Playframework2
        |
        |主要项目经验：
        |       1.生物数据库网站（运用Playframework For Scala Web框架所构建的数据库网站）
        |           中国海洋大学藻类细胞器数据库（http://ogda.ytu.edu.cn）
        |           山东烟台大学软体动物线粒体数据库（http://modb.ytu.edu.cn）
        |           复旦BSL-3实验室致病菌数据库（http://biorisk.cn）
        |           复旦BSL-3生物安全网站（http://bio-safety.cn/）
        |           南农黄瓜数据库（http://www.cucumisgdb.cn/）
        |           浙江农科院杨梅数据库（http://www.bayberrybase.cn）
        |           上海辰山植物园蕨类数据库（http://ifern.cn/）
        |           水稻资源中心（http://113.54.11.180/）
        |           江南大学酿造数据库
        |           四川省农科院小麦-油菜数据库
        |       2.生物分析平台类
        |           浙江省湖州市中心人民医院结直肠癌诊疗系统（http://210.22.121.250:5888/）
        |           南昌大学生物分析平台
        |           甘肃中医药大学分析平台
        |           小黄鱼全基因组分子育种平台
        |       3.pdf报告生成系统（根据客户所提供资料，运用itext7 包，自动生成pdf的jar程序）
        |           凌恩生物报告系统
        |           凡迪全基因组报告系统
        |           英莱盾祖源报告
        |       4.纯展示类网站
        |           南京智慧农业研究院官网（http://47.110.146.222:8080/）
        |           盈飞生物微生物多样性分析软件官网（http://amplicon.vgenomics.cn/）
        |""".stripMargin
    println(names)

    /*
        (0 to 332611).foreach{x=>
         val q =b.readLine()
        }
    */


    val x = Source.fromFile(new File("I:\\colorectal_cancer\\public\\cBioPortal\\coadread_tcga_pan_can_atlas_2018/data_RNA_Seq_v2_expression_median.txt"))
    val y = x.getLines()

    y.slice(10000, 10020).foreach(println)

    // x.close()


    println(Utils.getTime(time))


  }


}
