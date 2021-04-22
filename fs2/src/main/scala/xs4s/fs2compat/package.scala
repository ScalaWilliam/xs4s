package xs4s

import fs2._
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource, Sync}
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import syntax.fs2._

import scala.language.higherKinds

package object fs2compat {

  /**
    * Turns an FS2 Byte Stream into a stream of XMLEvent.
    * It turns the source stream into an input stream,
    * then creates a reader, and then creates a further stream
    * from the Iterator that was obtained.
    * */
  def byteStreamToXmlEventStream[F[_]: ConcurrentEffect: ContextShift](
      blocker: Blocker,
      xmlInputFactory: XMLInputFactory = defaultXmlInputFactory,
      chunkSize: Int = 1)(
      implicit F: Sync[F]): Pipe[F, Byte, XMLEvent] =
    byteStream =>
      Stream
        .resource(io.toInputStreamResource(byteStream))
        .flatMap(
          inputStream =>
            Stream.xmlEventStream(
              blocker,
              Resource.make(
                F.delay(xmlInputFactory.createXMLEventReader(inputStream)))(
                xmlEventReader => F.delay(xmlEventReader.close())),
              chunkSize))
}
