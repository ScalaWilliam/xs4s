package xs4s.fs2compat

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.{Chunk, Stream}
import org.scalatest.freespec.AnyFreeSpec
import xs4s.XmlElementExtractor
import xs4s.syntax.fs2.RichXmlElementExtractor

import scala.xml.Elem

final class Fs2CompatSpec extends AnyFreeSpec {
  private val input =
    s"""
       |<items>
       |<embedded><item>Embedded</item></embedded>
       |<item>General</item>
       |<embedded-twice><embedded-once><item>Doubly embedded</item></embedded-once></embedded-twice>
       |<item><item>Nested</item></item>
       |</items>
       |
       """.stripMargin

  "It works" in {
    val anchorElementExtractor: XmlElementExtractor[Elem] =
      XmlElementExtractor.filterElementsByName("item")

    val textStream: Stream[IO, String] = fs2.Stream
      .apply[IO, String](input)
      .flatMap(str => fs2.Stream.emits(str.getBytes().toList))
      .through(byteStreamToXmlEventStream[IO](chunkSize = 10240))
      .through(anchorElementExtractor.toFs2PipeThrowError)
      .map(_.text)

    assert(
      textStream.compile.toList.unsafeRunSync() == List("Embedded",
                                                        "General",
                                                        "Doubly embedded",
                                                        "Nested"))
  }

  "Roundtrip" in {
    val stringStream: Stream[IO, String] = fs2.Stream
      .apply[IO, String](input)
      .flatMap(str => fs2.Stream.emits(str.getBytes().toList))
      .through(byteStreamToXmlEventStream[IO](chunkSize = 10240))
      .through(xmlEventStreamToByteStream[IO](chunkSize = 10240))
      .through(fs2.text.utf8Decode)

    val string = stringStream.compile.string.unsafeRunSync()
      .replaceAll("<\\?xml.*?>", "")
      .replaceAll("\r?\n", "\n")

    assert(
      string == input.trim.replaceAll("\r?\n", "\n"))
  }
}
