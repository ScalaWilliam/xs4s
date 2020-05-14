import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Sync}
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import scala.language.higherKinds

package object xs4s {

  object syntax extends Syntax

  import syntax._

  def byteStreamToXmlEventStream[F[_]: ConcurrentEffect: ContextShift](
      blocker: Blocker)(implicit F: Sync[F]): fs2.Pipe[F, Byte, XMLEvent] =
    byteStreamToXmlEventStream(XMLInputFactory.newInstance(), blocker)

  def byteStreamToXmlEventStream[F[_]: ConcurrentEffect: ContextShift](
      xmlInputFactory: XMLInputFactory,
      blocker: Blocker)(implicit F: Sync[F]): fs2.Pipe[F, Byte, XMLEvent] =
    s => {
      fs2.Stream
        .resource(fs2.io.toInputStreamResource(s))
        .flatMap(
          is =>
            fs2.Stream
              .bracket(F.delay(xmlInputFactory.createXMLEventReader(is)))(r =>
                F.delay(r.close()))
              .flatMap(xer =>
                fs2.Stream
                  .fromBlockingIterator[F]
                  .apply(blocker, xer.toIterator)))
    }

  sealed trait XmlStreamError extends Exception
  object XmlStreamError {
    object InvalidSequenceOfParserEvents extends XmlStreamError
  }

}
