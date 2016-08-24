package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent

import com.scalawilliam.xs4s.elementbuilder.XmlBuilder.{FinalElement, NoElement}

import scala.xml.Elem

/**
  * Build 'Elem' out of 'XMLEvent'. See tests for examples.
  */
package object elementbuilder {


  implicit class eventReaderExtractors(eventReader: XMLEventReader) {

    import XmlEventIterator._

    def xmlBuilders = eventReader.toIterator.xmlBuilders

    def blockingFinal = eventReader.toIterator.blockingFinal

    def blockingElement = eventReader.toIterator.blockingElement
  }

  implicit class extractors(input: Iterator[XMLEvent]) {
    def xmlBuilders: Iterator[XmlBuilder] = {
      input.scanLeft(NoElement: XmlBuilder)(_.process(_))
    }

    def blockingFinal: Iterator[FinalElement] = {
      xmlBuilders.collect {
        case f: FinalElement => f
      }
    }

    def blockingElement: Iterator[Elem] = {
      blockingFinal.collect {
        case FinalElement(e) => e
      }
    }
  }


}
