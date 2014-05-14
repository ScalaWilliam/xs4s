package com.scalawilliam.xml4s

import javax.xml.stream.events.{XMLEvent, StartElement}
import java.io.InputStream
import javax.xml.stream.{XMLInputFactory, XMLEventReader}
import scala.xml.Elem


object ScalaXmlStreamSplitter {
  implicit class XMLEventIterator(eventReader: XMLEventReader) extends scala.collection.Iterator[XMLEvent] {
    def hasNext = eventReader.hasNext
    def next() = eventReader.nextEvent()
  }
}
case class ScalaXmlStreamSplitter(matchAtTag: StartElement => Boolean) {
  import ScalaXmlStreamSplitter._
  val inputFactory = XMLInputFactory.newInstance()
  def apply(inputStream: InputStream): Iterator[Elem] =
    apply(inputFactory.createXMLEventReader(inputStream))
  def apply(xmlEventReader: XMLEventReader): Iterator[Elem] = {
    var currentState: ProcessNext = StartElementNotFoundYet(xmlEventReader)
    xmlEventReader map {
      event =>
        currentState = currentState.process(event)
        currentState
    } collect {
      case ElementCollected(_, e) => e
    }
  }
  private sealed trait ProcessNext {
    type XMLEventToProcess = XMLEvent => ProcessNext
    def process: XMLEventToProcess
  }
  private case class StartElementNotFoundYet(xmlEventReader: XMLEventReader) extends ProcessNext {
    val process: XMLEventToProcess = {
      case startElement: StartElement if matchAtTag(startElement) =>
        ElementCollected(xmlEventReader, ElementBuilder.constructTree(startElement, xmlEventReader))
      case other => this
    }
  }
  private case class ElementCollected(xmlEventReader: XMLEventReader, e: Elem) extends ProcessNext {
    val process: XMLEventToProcess = StartElementNotFoundYet(xmlEventReader).process
  }
}
