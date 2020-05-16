package xs4s.example.brief

/** An example that compiles but does not run - for use in the README */
object BriefFS2Example {
  import cats.effect._
  import fs2._
  import javax.xml.stream.events.XMLEvent
  import xs4s._
  import xs4s.fs2compat._
  import xs4s.syntax.fs2._

  import scala.xml.Elem

  /**
    *
    * @param byteStream Could be, for example, fs2.io.readInputStream(inputStream)
    * @param blocker obtained with Blocker[IO]
    * @return
    */
  def extractAnchorTexts(byteStream: Stream[IO, Byte], blocker: Blocker)(
      implicit cs: ContextShift[IO]): Stream[IO, String] = {

    /** extract all elements called 'anchor' **/
    val anchorElementExtractor: XmlElementExtractor[Elem] =
      XmlElementExtractor.filterElementsByName("anchor")

    /** Turn into XMLEvent */
    val xmlEventStream: Stream[IO, XMLEvent] =
      byteStream.through(byteStreamToXmlEventStream(blocker))

    /** Collect all the anchors as [[scala.xml.Elem]] */
    val anchorElements: Stream[IO, Elem] =
      xmlEventStream.through(anchorElementExtractor.toFs2PipeThrowError)

    /** And finally extract the text contents for each Elem */
    anchorElements.map(_.text)
  }

}
