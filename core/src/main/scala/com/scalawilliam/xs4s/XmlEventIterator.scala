package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent

trait XmlEventIterator {

  implicit class XMLEventIterator(eventReader: XMLEventReader) extends scala.collection.Iterator[XMLEvent] {
    def hasNext = eventReader.hasNext

    def next() = eventReader.nextEvent()
  }

}

object XmlEventIterator extends XmlEventIterator