import cats.effect.{Blocker, ConcurrentEffect, ContextShift, IO, Resource, Sync}
import fs2.Pipe
import javax.xml.stream.{XMLEventReader, XMLInputFactory}
import javax.xml.stream.events.XMLEvent

import scala.xml.Elem

package object xs4s {

  import syntax._

  def byteStreamToXmlEventStream[F[_]: ConcurrentEffect: ContextShift](
      blocker: Blocker)(implicit F: Sync[F]): fs2.Pipe[F, Byte, XMLEvent] =
    s => {
      val xmlInputFactory = XMLInputFactory.newInstance()
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

  /**
    * A set of functions that allow a streaming scanLeft,
    * where the collection extracts a final result
    */
  trait Scanner[In, State, Out] {
    def initial: State

    def scan(state: State, element: In): State

    def collect(state: State): Option[Out]
  }

  /**
    * Implicit utilities for dealing with XMLEventReader, Iterators and Scanners.
    */
  trait Syntax {

    implicit class RichXMLEventIterator(input: Iterator[XMLEvent]) {

      /**
        * We must assume the input iterator is finite
        */
      def buildElement: Option[Elem] =
        input.scanCollect(ScalaXmlElemBuilder.Scanner).toStream.lastOption
    }

    implicit class RichXMLEventReader(eventReader: XMLEventReader)
        extends scala.collection.Iterator[XMLEvent] {
      def hasNext: Boolean = eventReader.hasNext

      def next(): XMLEvent = eventReader.nextEvent()

      def buildElement: Option[Elem] = eventReader.toIterator.buildElement
    }

    implicit class RichIterator[T](iterator: Iterator[T]) {
      def scanCollect[S, O](scanner: Scanner[T, S, O]): Iterator[O] =
        iterator
          .scanLeft(scanner.initial)(scanner.scan)
          .flatMap(scanner.collect)
    }

    implicit class RichFs2StreamObj(obj: fs2.Stream.type) {
      def xmlEventStream[F[_]: ContextShift: Sync](
          blocker: Blocker,
          xmlEventReader: Resource[F, XMLEventReader])
        : fs2.Stream[F, XMLEvent] =
        fs2.Stream
          .resource(xmlEventReader)
          .flatMap(
            reader =>
              fs2.Stream
                .fromBlockingIterator[F]
                .apply[XMLEvent](blocker, reader.toIterator))
    }

    implicit class RichScanner[I, S, O](scanner: Scanner[I, S, O]) {
      def toFs2Pipe[F[_]]: Pipe[F, I, O] =
        _.scan(scanner.initial)(scanner.scan).map(scanner.collect).unNone
    }

  }

  object syntax extends Syntax

}
