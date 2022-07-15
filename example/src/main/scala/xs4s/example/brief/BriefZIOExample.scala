package xs4s.example.brief

import java.io.IOException

/** An example that compiles but does not run - for use in the README */
object BriefZIOExample {
  import javax.xml.stream.events.XMLEvent
  import xs4s._
  import xs4s.ziocompat._
  import xs4s.syntax.zio._
  import zio._
  import zio.stream._

  import scala.xml.Elem

  /**
    *
    * @param byteStream Could be, for example, zio.stream.Stream.fromInputStream(inputStream)
    * @return
    */
  def extractAnchorTexts[R](byteStream: ZStream[R, IOException, Byte]): ZStream[R with Scope, Throwable, String] = {
    /** extract all elements called 'anchor' **/
    val anchorElementExtractor: XmlElementExtractor[Elem] =
      XmlElementExtractor.filterElementsByName("anchor")

    /** Turn into XMLEvent */
    val xmlEventStream: ZStream[R with Scope, Throwable, XMLEvent] =
      byteStream.viaFunction(byteStreamToXmlEventStream[R]()(_))

    /** Collect all the anchors as [[scala.xml.Elem]] */
    val anchorElements: ZStream[R with Scope, Throwable, Elem] =
      xmlEventStream.viaFunction(anchorElementExtractor.toZIOPipeThrowError)

    /** And finally extract the text contents for each Elem */
    anchorElements.map(_.text)
  }

}
