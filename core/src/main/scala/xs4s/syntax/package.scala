package xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent
import xs4s.generic.Scanner.ScannerSyntaxes

import scala.xml.Elem

/** Various supported implicit conversions and enhancements */
package object syntax {
  object generic extends ScannerSyntaxes
  trait CoreSyntax {
    import generic._
    implicit class RichXMLEventReaderIterator(eventReader: XMLEventReader) {
      def toIterator: Iterator[XMLEvent] = xmlEventReaderToIterator(eventReader)

      def extractWith[T](
          xmlElementExtractor: XmlElementExtractor[T]): Iterator[T] =
        toIterator.through(xmlElementExtractor.scannerThrowingOnError)

      def readElementFully: Elem =
        xs4s.readElementFully(eventReader)
    }
  }
  object core extends CoreSyntax

}
