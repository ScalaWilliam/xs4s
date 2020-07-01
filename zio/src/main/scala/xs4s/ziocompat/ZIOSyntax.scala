package xs4s.ziocompat

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent
import xs4s.XmlElementExtractor
import xs4s.generic.Scanner
import xs4s.syntax.core._
import zio.stream.ZStream
import zio.Runtime.Managed
import zio.ZManaged
import java.io.IOException
import zio.blocking.Blocking

/**
  * Utilities to enhance xs4s interaction with ZIO
  */
trait ZIOSyntax {
  implicit class RichScanner[In, State, Out](scanner: Scanner[In, State, Out]) {
    /** Create an ZIO-FS2-style Pipe from the scanner */
    def zioPipe[R]: Pipe[R, In, Out] =
      _.mapAccum(scanner.initial)((s: State, i: In) => (scanner.scan(s, i), scanner.collect(s))).collectSome
  }

  implicit class RichZIOZStreamObj(obj: ZStream.type) {
    /** Create an XMLEvent Stream from an XMLEventReader */
    def xmlEventStream[R](xmlEventReader: ZManaged[R, Throwable, XMLEventReader]): ZStream[R, Throwable, XMLEvent] = {
      ZStream.apply(xmlEventReader)
        .flatMap{ reader =>
          ZStream.fromIteratorManaged[R, XMLEvent](reader.map(_.toIterator))
        }
    }
  }

  implicit class RichXmlElementExtractor[O](xmlElementExtractor: XmlElementExtractor[O]) {
    def toZIOPipeThrowError[R <: Blocking]: Pipe[R, XMLEvent, O] =
      xmlElementExtractor.scannerThrowingOnError.zioPipe[R]
    def toZIOPipeIncludeError[R <: Blocking]: Pipe[R, XMLEvent, Either[xs4s.XmlStreamError, O]] =
      xmlElementExtractor.scannerEitherOnError.zioPipe[R]
  }
}
