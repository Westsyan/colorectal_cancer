package utils

import config.MyFile
import javax.imageio.ImageIO
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer

object PdfToImage extends MyFile {

  def pdf2PngAndTiff(pdf: String, path: String): Unit = {
    pdf2Png(pdf, path)
    pdf2Tiff(pdf, path)
  }

  def pdf2Png(pdf: String, path: String): Unit = {
    val pdfF = s"$path/$pdf.pdf"
    val pngF = s"$path/$pdf.png"

    if (Global.isWindow) {
      windowPdf2Image(pdfF, pngF, "png")
    } else {
      unxiPdf2Image(pdfF, pngF, path)
    }
  }

  def pdf2Tiff(pdf: String, path: String): Unit = {
    val pdfF = s"$path/$pdf.pdf"
    val pngF = s"$path/$pdf.tiff"

    if (Global.isWindow) {
      windowPdf2Image(pdfF, pngF, "tiff")
    } else {
      unxiPdf2Image(pdfF, pngF, path)
    }
  }

  def windowPdf2Image(pdfF: String, pngF: String, suffix: String): Unit = {
    val document = PDDocument.load(pdfF.toFile)
    val renderer = new PDFRenderer(document)
    ImageIO.write(renderer.renderImage(0, 3), suffix, pngF.toFile)
    document.close()
  }

  def unxiPdf2Image(pdfF: String, pngF: String, path: String): Unit = {
    val exec2 = new ExecCommand
    val convert = s"convert -density 300 $pdfF $pngF"
    exec2.exect(convert, path)
  }


}
