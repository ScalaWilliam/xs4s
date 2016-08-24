package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent


import scala.xml.Elem

/**
  * Build 'Elem' out of 'XMLEvent'. See tests for examples.
  */
package object elementbuilder {


  implicit class eventReaderExtractors(eventReader: XMLEventReader) {

    import XmlEventIterator._

    def xmlBuilders = eventReader.toIterator.xmlBuilders


    def blockingElement = eventReader.toIterator.blockingElement
  }

  implicit class extractors(input: Iterator[XMLEvent]) {
    def xmlBuilders: Iterator[XmlElementBuilder] = {
      input.scanLeft(XmlElementBuilder.Scan.initial)(XmlElementBuilder.Scan.scan)
    }

    def blockingElement: Iterator[Elem] = {
      xmlBuilders.collect(XmlElementBuilder.Scan.collect)
    }
  }


}
