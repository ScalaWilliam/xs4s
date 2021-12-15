package xs4s.fs2compat

import _root_.fs2.{Pipe, Stream}
import cats.effect.{Resource, Sync}
import xs4s.XmlElementExtractor
import xs4s.generic.Scanner
import xs4s.syntax.core._

import javax.xml.stream.{XMLEventReader, XMLEventWriter}
import javax.xml.stream.events.XMLEvent
import scala.language.higherKinds

/**
  * Utilities to enhance xs4s interaction with FS2
  */
trait Fs2Syntax {

  implicit class RichScanner[In, State, Out](scanner: Scanner[In, State, Out]) {

    /** Create an FS2 Pipe from the scanner */
    def fs2Pipe[F[_]]: Pipe[F, In, Out] =
      _.scan(scanner.initial)(scanner.scan).map(scanner.collect).unNone
  }

  implicit class RichFs2StreamObj(obj: Stream.type) {

    /** Create an XMLEvent Stream from an XMLEventReader */
    def xmlEventStream[F[_]: Sync](
        xmlEventReader: Resource[F, XMLEventReader],
        chunkSize: Int = 1): Stream[F, XMLEvent] =
      Stream
        .resource(xmlEventReader)
        .flatMap(
          reader =>
            Stream
              .fromBlockingIterator[F]
              .apply[XMLEvent](reader.toIterator, chunkSize))
  }

  implicit class RichFs2XmlEventStream[F[_] : Sync](stream: Stream[F, XMLEvent]) {

    /** Writes an XMLEvent Stream to an XMLEventWriter */
    def writeXmlEventStream(
        xmlEventWriter: Resource[F, XMLEventWriter]): F[Unit] =
      Stream
        .resource(xmlEventWriter)
        .flatMap(
          stream
            .chunks
            .fold(_) { (writer, events) =>
              events.foreach(writer.add)
              writer.flush()
              writer
            })
        .compile
        .drain
  }

  implicit class RichXmlElementExtractor[O](
      xmlElementExtractor: XmlElementExtractor[O]) {
    def toFs2PipeThrowError[F[_]]: Pipe[F, XMLEvent, O] =
      xmlElementExtractor.scannerThrowingOnError.fs2Pipe[F]
    def toFs2PipeIncludeError[F[_]]: Pipe[F, XMLEvent, Either[Throwable, O]] =
      xmlElementExtractor.scannerEitherOnError.fs2Pipe[F]
  }
}
