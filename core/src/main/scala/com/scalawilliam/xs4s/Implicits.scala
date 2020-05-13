package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent
import scala.xml.Elem

/**
 * Implicit utilities for dealing with XMLEventReader, Iterators and Scanners.
 */
trait Implicits extends Scanner.Implicits {

  implicit class RichXMLEventIterator(input: Iterator[XMLEvent]) {
    /**
     * We must assume the input iterator is finite
     */
    def buildElement: Option[Elem] = {
      input.scanCollect(XmlElementBuilder.Scanner).toStream.lastOption
    }
  }

  implicit class RichXMLEventReader(eventReader: XMLEventReader) extends scala.collection.Iterator[XMLEvent] {
    def hasNext: Boolean = eventReader.hasNext

    def next(): XMLEvent = eventReader.nextEvent()

    def buildElement: Option[Elem] = eventReader.toIterator.buildElement
  }

}

object Implicits extends Implicits
