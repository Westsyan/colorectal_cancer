package utils

import java.io.File

import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

object PdfToPng {

  def pdf2Png(pdfFile: File, pngFile: File): Unit = {
    val document = PDDocument.load(pdfFile)
    val renderer = new PDFRenderer(document)
    ImageIO.write(renderer.renderImage(0, 3), "png", pngFile)
    document.close()
  }
}
