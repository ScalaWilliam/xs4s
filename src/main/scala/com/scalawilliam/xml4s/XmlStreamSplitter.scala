package com.scalawilliam.xml4s

import java.io.{ByteArrayOutputStream, ByteArrayInputStream, InputStream}
import javax.xml.stream.events.{EndElement, XMLEvent, StartElement}
import javax.xml.stream.{XMLInputFactory, XMLEventWriter, XMLOutputFactory, XMLEventReader}
import javax.xml.transform.stream.StreamResult


object XmlStreamSplitter {
  implicit class XMLEventIterator(eventReader: XMLEventReader) extends scala.collection.Iterator[XMLEvent] {
    def hasNext = eventReader.hasNext
    def next() = eventReader.nextEvent()
  }
}
case class XmlStreamSplitter(matchAtTag: StartElement => Boolean) {
  val inputFactory = XMLInputFactory.newInstance()
  val outputFactory = XMLOutputFactory.newInstance()
   def apply(xmlEventReader: XMLEventReader): Iterator[InputStream] = {
     import XmlStreamSplitter.XMLEventIterator
     /**
      * Process next step when a 'next' is requested
      */
     var currentState: ProcessNext = StartElementNotFoundYet
     xmlEventReader.map{
       event =>
         currentState = currentState.process(event)
         currentState
     } collect {
       case WriterFinished(_, outputStream) =>
         outputStream.flush()
         val bais = new ByteArrayInputStream(outputStream.toByteArray)
         outputStream.close()
         bais
     }
   }

   def apply(inputStream: InputStream): Iterator[InputStream] =
     apply(inputFactory.createXMLEventReader(inputStream))


   /** Finite state machine to move through the processing - perfect for pull streaming **/
   private sealed trait ProcessNext {
     type XMLEventToProcess = XMLEvent => ProcessNext
     def process: XMLEventToProcess
   }

   private case object StartElementNotFoundYet extends ProcessNext {
     val process: XMLEventToProcess = {
       case startElement: StartElement if matchAtTag(startElement) =>
         val outputStream = new ByteArrayOutputStream()
         val streamResult = new StreamResult(outputStream)
         val eventWriter = outputFactory.createXMLEventWriter(streamResult)
         NewWriter(eventWriter, outputStream, startElement)
       case other => this
     }
   }

   private case class NewWriter(writer: XMLEventWriter, outputStream: ByteArrayOutputStream, processThis: XMLEvent) extends ProcessNext {
     val process: XMLEventToProcess = {
       case event => PerformingOutput(writer, outputStream, 0).process(processThis).process(event)
     }
   }

   private case class WriterFinished(writer: XMLEventWriter, outputStream: ByteArrayOutputStream) extends ProcessNext {
     val process: XMLEventToProcess = StartElementNotFoundYet.process
   }

   private case class PerformingOutput(writer: XMLEventWriter, outputStream: ByteArrayOutputStream, level: Int) extends ProcessNext {
     val process: XMLEventToProcess = {
       case startElement: StartElement =>
         writer.add(startElement)
         this.copy(level = level + 1)
       case chunkEndElement: EndElement if level == 1 =>
         writer.add(chunkEndElement)
         WriterFinished(writer, outputStream)
       case endElement: EndElement =>
         writer.add(endElement)
         this.copy(level = level - 1)
       case event =>
         writer.add(event)
         this
     }
   }

 }
