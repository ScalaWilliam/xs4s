package xs4s

import zio._
import zio.stream._
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import syntax.zio._
import java.io.InputStream

package object ziocompat {

  type Pipe[R, -I, +O] = ZStream[R, Throwable, I] => ZStream[R, Throwable, O]

  /**
    * Turns an ZIO Byte Stream into a stream of XMLEvent.
    * It turns the source stream into an input stream,
    * then creates a reader, and then creates a further stream
    * from the Iterator that was obtained.
    * */
  def byteStreamToXmlEventStream[R](
    xmlInputFactory: XMLInputFactory = defaultXmlInputFactory
  ): Pipe[R with Scope, Byte, XMLEvent] = { (byteStream: ZStream[R with Scope,Throwable,Byte]) =>

    val reader = byteStream.toInputStream.mapAttempt { (is: InputStream) =>
      xmlInputFactory.createXMLEventReader(is)
    }

    ZStream.xmlEventStream(reader)
  }
}
