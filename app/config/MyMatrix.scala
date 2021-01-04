package config

import scala.collection.mutable

trait MyMatrix {

  implicit class myLines(lines:mutable.Buffer[String]) {


    def asHtmlTable:String = {
      val thead = lines.head.split("\t").map{x=>
        "<th>" + x + "</th>"
      }.mkString
      val tbody = lines.tail.map{x=>
        val l = x.split("\t")
        val head = "<th>" + l.head + "</th>"
        val body = l.tail.map(y=> "<td>" + y + "</td>").mkString
        head + body
      }.mkString("</tr><tr>")
      "<thead><tr>" + thead + "</tr></thead>" + "<tbody><tr>" + tbody + "</tr></tbody>"
    }

    def asHtmlTableNoHeader:String = {
      val tbody = lines.map{x=>
        val l = x.split("\t")
        val head = "<th>" + l.head + "</th>"
        val body = l.tail.map(y=> "<td>" + y + "</td>").mkString
        head + body
      }.mkString("</tr><tr>")
      "<tbody><tr>" + tbody + "</tr></tbody>"
    }

    def asHtmlTableNoHeader2:String = {
      println(lines)
      "<thead><tr>" + lines.head.split("\t").map(x=> "<th>" + x + "</th>").mkString + "</tr></thead><tbody>" +
      lines.tail.map(_.split("\t").map(x=> "<td>" + x+"</td>")).map(x=> "<tr>" +x +"</tr>") + "</tbody>"
    }

  }

}
