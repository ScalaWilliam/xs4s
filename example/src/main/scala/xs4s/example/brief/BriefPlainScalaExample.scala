package xs4s.example.brief

import java.io.File

/** An example that compiles but does not run - for use in the README */
object BriefPlainScalaExample {
  import xs4s._
  import xs4s.syntax.core._
  import scala.xml.Elem

  def extractAnchorTexts(sourceFile: File): Unit = {
    val anchorElementExtractor: XmlElementExtractor[Elem] =
      XmlElementExtractor.filterElementsByName("anchor")
    val xmlEventReader = XMLStream.fromFile(sourceFile)
    try {
      val elements: Iterator[Elem] =
        xmlEventReader.extractWith(anchorElementExtractor)
      val text: Iterator[String] = elements.map(_.text)
      text.foreach(println)
    } finally xmlEventReader.close()
  }

}
