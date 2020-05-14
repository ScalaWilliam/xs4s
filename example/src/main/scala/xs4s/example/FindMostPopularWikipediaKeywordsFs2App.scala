package xs4s.example

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import javax.xml.stream.XMLInputFactory
import xs4s._
import xs4s.syntax._

object FindMostPopularWikipediaKeywordsFs2App extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { blocker =>
      wikipediaXmlBytes(blocker)
        .through(
          byteStreamToXmlEventStream(XMLInputFactory.newInstance(), blocker))
        .through(anchorExtractor.pipeThrowError)
        .through(stream => // by default, get a sub-set of the stream
          if (args.contains("full")) stream else stream.take(500))
        .map(_.text)
        .through(countTopItemsFs2)
        .evalMap(list =>
          list.traverse_ {
            case (elem, count) => IO.delay(println(count, elem))
        })
        .compile
        .drain
        .as(ExitCode.Success)
    }

  /**
    * Can be any of your own XML byte stream
    */
  private def wikipediaXmlBytes(blocker: Blocker): fs2.Stream[IO, Byte] =
    fs2.io
      .readInputStream(
        fis = IO.delay(wikipediaAbstractURL.openStream()),
        chunkSize = 1024,
        blocker = blocker,
        closeAfterUse = true
      )
      .through(fs2.compress.gunzip(bufferSize = 1024))

}
