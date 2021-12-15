package xs4s

import cats.effect.{Async, Resource, Sync}
import fs2._
import xs4s.syntax.fs2._

import javax.xml.stream.{XMLInputFactory, XMLOutputFactory}
import javax.xml.stream.events.XMLEvent
import scala.language.higherKinds

package object fs2compat {

  /**
    * Turns an FS2 Byte Stream into a stream of XMLEvent.
    * It turns the source stream into an input stream,
    * then creates a reader, and then creates a further stream
    * from the Iterator that was obtained.
    * */
  def byteStreamToXmlEventStream[F[_]: Async](
      xmlInputFactory: XMLInputFactory = defaultXmlInputFactory,
      chunkSize: Int = 1)(implicit F: Sync[F]): Pipe[F, Byte, XMLEvent] =
    byteStream =>
      Stream
        .resource(io.toInputStreamResource(byteStream))
        .flatMap(
          inputStream =>
            Stream.xmlEventStream(
              Resource.make(
                F.delay(xmlInputFactory.createXMLEventReader(inputStream)))(
                xmlEventReader => F.delay(xmlEventReader.close())),
              chunkSize))

  /**
    * Turns an FS2 XMLEvent Stream into a stream of Bytes.
    **/
  def xmlEventStreamToByteStream[F[_] : Async](
      xmlOutputFactory: XMLOutputFactory = defaultXmlOutputFactory,
      chunkSize: Int)(implicit F: Sync[F]): Pipe[F, XMLEvent, Byte] =
    (xmlEventStream: Stream[F, XMLEvent]) =>
      io.readOutputStream(chunkSize)(
        outputStream =>
          xmlEventStream.writeXmlEventStream(
            Resource.make(
              F.delay(xmlOutputFactory.createXMLEventWriter(outputStream)))(
              XMLEventWriter => F.delay(XMLEventWriter.close()))
          ))
}
