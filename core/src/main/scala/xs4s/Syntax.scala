package xs4s

import cats.effect.{Blocker, ContextShift, Resource, Sync}
import fs2.Pipe
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent
import xs4s.generic.GenericSyntax

import scala.language.higherKinds

trait Syntax extends GenericSyntax {

  implicit class RichXMLEventReader(eventReader: XMLEventReader) {
    def toIterator: Iterator[XMLEvent] = new Iterator[XMLEvent] {
      def hasNext: Boolean = eventReader.hasNext
      def next(): XMLEvent = eventReader.nextEvent()
    }
  }

  implicit class RichFs2StreamObj(obj: fs2.Stream.type) {
    def xmlEventStream[F[_]: ContextShift: Sync](
        blocker: Blocker,
        xmlEventReader: Resource[F, XMLEventReader]): fs2.Stream[F, XMLEvent] =
      fs2.Stream
        .resource(xmlEventReader)
        .flatMap(
          reader =>
            fs2.Stream
              .fromBlockingIterator[F]
              .apply[XMLEvent](blocker, reader.toIterator))
  }

  implicit class RichXmlElementExtractor[O](
      xmlElementExtractor: XmlElementExtractor[O]) {
    def pipeThrowError[F[_]]: Pipe[F, XMLEvent, O] =
      xmlElementExtractor.scannerThrowingOnError.fs2Pipe[F]
    def pipeWithError[F[_]]: Pipe[F, XMLEvent, Either[Throwable, O]] =
      xmlElementExtractor.scannerEitherOnError.fs2Pipe[F]
  }

}
