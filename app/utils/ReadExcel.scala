package utils

import java.io.{File, FileInputStream}
import java.text.SimpleDateFormat

import org.apache.commons.lang3.StringUtils
import org.apache.poi.ss.usermodel.{Cell, DateUtil}
import org.apache.poi.xssf.usermodel.XSSFWorkbook

import scala.collection.mutable.ArrayBuffer

object ReadExcel {


  def getOrNull(seq: Array[String], num: Int) = {
    try {
      val r = seq(num)
      if (r == "NA") " " else r
    } catch {
      case e: Exception => " "
    }

  }

  def xlsx2Lines(xlsxFile: File) = {
    val is = new FileInputStream(xlsxFile.getAbsolutePath)
    val xssfWorkbook = new XSSFWorkbook(is)
    val xssfSheet = xssfWorkbook.getSheetAt(0)
    val lines = ArrayBuffer[String]()
    for (i <- 0 to xssfSheet.getLastRowNum) {
      val columns = ArrayBuffer[String]()
      val xssfRow = xssfSheet.getRow(i)
      for (j <- 0 until xssfSheet.getRow(0).getLastCellNum) {
        var value = "NA"

        try {
          val cell = xssfRow.getCell(j)
          if (cell != null) {
            cell.getCellType match {
              case Cell.CELL_TYPE_STRING =>
                value = cell.getStringCellValue
              case Cell.CELL_TYPE_NUMERIC =>
                if (DateUtil.isCellDateFormatted(cell)) {
                  val dateFormat = new SimpleDateFormat("yyyy/MM/dd")
                  value = dateFormat.format(cell.getDateCellValue)
                } else {
                  val doubleValue = cell.getNumericCellValue
                  value = if (doubleValue == doubleValue.toInt) {
                    doubleValue.toInt.toString
                  } else doubleValue.toString
                }
              case Cell.CELL_TYPE_BLANK =>
                value = "NA"
              case _ =>
                value = "NA"
            }
          }
        }catch {
          case e:Exception =>  value = "NA"
        }

        columns += value
      }
      val line = columns.mkString("\t")
      lines += line
    }
    xssfWorkbook.close()
    lines.filter(StringUtils.isNotBlank(_))
  }

}
