package xs4s.example

import java.util.zip.GZIPInputStream

import xs4s.syntax.core._
import xs4s.syntax.generic._
import javax.xml.stream.XMLInputFactory

object FindMostPopularWikipediaKeywordsPlainScalaApp extends App {

  private val xmlInputFactory = XMLInputFactory.newInstance()
  val inputStream             = wikipediaAbstractURL.openStream()

  try {
    val xmlStreamReader =
      xmlInputFactory.createXMLEventReader(new GZIPInputStream(inputStream))
    try {
      val anchorElements =
        xmlStreamReader.toIterator
          .through(anchorExtractor.scannerThrowingOnError)
          .map(_.text)
      val limitedAnchorElements =
        if (args.contains("full")) anchorElements else anchorElements.take(500)
      countTopItemsIterator(limitedAnchorElements).foreach {
        case (elem, count) => println(s"$count × $elem")
      }
    } finally xmlStreamReader.close()
  } finally inputStream.close()

}
