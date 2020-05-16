import javax.xml.stream.{XMLEventReader, XMLInputFactory}
import javax.xml.stream.events.XMLEvent
import xs4s.additions.{XMLEventReaderMaker, XMLLoader}

import scala.language.higherKinds
import scala.xml.Elem
import xs4s.syntax.generic._
import xs4s.syntax.core._

package object xs4s {

  sealed trait XmlStreamError extends Exception

  object XmlStreamError {
    object InvalidSequenceOfParserEvents extends XmlStreamError
  }

  def xmlEventReaderToIterator(
      eventReader: XMLEventReader): Iterator[XMLEvent] =
    new Iterator[XMLEvent] {
      def hasNext: Boolean = eventReader.hasNext
      def next(): XMLEvent = eventReader.nextEvent()
    }

  /** Functionality to read an element fully
    * @throws java.util.NoSuchElementException if have been unable to construct an element
    **/
  def readElementFully(eventReader: XMLEventReader): Elem =
    eventReader.toIterator
      .through(XmlElementExtractor.captureRoot.scannerThrowingOnError)
      .lastOption
      .get

  private[xs4s] def defaultXmlInputFactory: XMLInputFactory =
    XMLInputFactory.newInstance()

  /**
    * Utilities to replicate [[scala.xml.XML]]
    */
  object XML extends XMLLoader {
    override def xmlInputFactory: XMLInputFactory = defaultXmlInputFactory
  }

  /**
    * Utilities to create an XMLEventReader from Scala more easily
    */
  object XMLStream extends XMLEventReaderMaker {
    override def xmlInputFactory: XMLInputFactory = defaultXmlInputFactory
  }

}
