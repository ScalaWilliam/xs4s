package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent

import scala.xml.Elem

/**
  * Created by me on 25/08/2016.
  */
trait Implicits extends Scanner.Implicits {

  implicit class NodeSeqExtensions(nodeSeq: scala.xml.NodeSeq) {
    def ===(hasValue: String): Boolean =
      nodeSeq.text == hasValue

    def !==(notValue: String): Boolean =
      nodeSeq.text != notValue
  }

  implicit class RichXMLEventIterator(input: Iterator[XMLEvent]) {
    def buildXml: Iterator[XmlElementBuilder] = {
      input.scan(XmlElementBuilder.Scanner)
    }

    /**
      * Note this is unsafe. If the iterator is infinite then we'll never return.
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
