package config

import java.io.File

import org.apache.commons.io.FileUtils

import scala.collection.mutable
import scala.jdk.CollectionConverters._

trait MyFile {

  implicit class myFile(file: File)(implicit encoding: String = "UTF-8") {

    def readLines: mutable.Buffer[String] = {
      FileUtils.readLines(file, encoding).asScala
    }

    def readLines(encoding: String): mutable.Buffer[String] = {
      FileUtils.readLines(file, encoding).asScala
    }

    def readFileToString: String = {
      FileUtils.readFileToString(file, encoding)
    }

    def writeLines(buffer: mutable.Buffer[String]): Unit = {
      FileUtils.writeLines(file, buffer.asJava, encoding,false)
    }

    def writeStringToFile(text: String): Unit = {
      FileUtils.writeStringToFile(file, text, encoding)
    }

    def mkdir = {
      file.mkdir()
    }

    def mkdirs = {
      file.mkdirs()
    }

    def unixPath: String = {
      file.getAbsolutePath.replaceAll("\\\\", "/")
    }

    def moveToFile(dest: File): Unit = {
      FileUtils.moveFile(file, dest)
    }

    def delete: Unit = {
      if (file.isDirectory) {
        FileUtils.deleteDirectory(file)
      } else {
        file.delete()
      }
    }

  }

  implicit class myPath(path: String) extends myFile(new File(path)) {

    def toFile: File = {
      new File(path)
    }



  }


}
