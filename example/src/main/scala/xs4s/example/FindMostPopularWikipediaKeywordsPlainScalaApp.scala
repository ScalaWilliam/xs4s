package xs4s.example

import java.util.zip.GZIPInputStream

import xs4s._
import xs4s.syntax._
import javax.xml.stream.XMLInputFactory

object FindMostPopularWikipediaKeywordsPlainScalaApp extends App {

  private val xmlInputFactory = XMLInputFactory.newInstance()
  val inputStream             = wikipediaAbstractURL.openStream()

  try {
    val xmlStreamReader =
      xmlInputFactory.createXMLEventReader(new GZIPInputStream(inputStream))
    try {
      val anchorElements =
        xmlStreamReader.extractXml(anchorExtractor).map(_.text)
      val limitedAnchorElements =
        if (args.contains("full")) anchorElements else anchorElements.take(500)
      countTopItems(limitedAnchorElements).foreach {
        case (elem, count) => println(count, elem)
      }
    } finally xmlStreamReader.close()
  } finally inputStream.close()

  private def countTopItems[I]: Iterator[I] => List[(I, Int)] =
    _.map(v => Map(v -> 1))
      .reduce(mergeCountMaps[I])
      .filter(_._2 > 1)
      .toList
      .sortBy { case (keyword, count) => -count }

}
