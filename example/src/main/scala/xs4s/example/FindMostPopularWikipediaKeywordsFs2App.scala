package xs4s.example

import cats.effect.{Blocker, ExitCode, IO, IOApp}
import cats.implicits._
import xs4s.fs2compat._
import xs4s.syntax.fs2._

object FindMostPopularWikipediaKeywordsFs2App extends IOApp {

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { blocker =>
      wikipediaXmlBytes(blocker)
        .through(byteStreamToXmlEventStream(blocker))
        .through(anchorExtractor.toFs2PipeThrowError)
        .through(stream => // by default, get a sub-set of the stream
          if (args.contains("full")) stream else stream.take(500))
        .map(_.text)
        .through(countTopItemsFs2)
        .evalMap(list =>
          list.traverse_ {
            case (elem, count) => IO.delay(println(s"$count × $elem"))
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
      .through(fs2.compression.gunzip(bufferSize = 1024))
      .flatMap(_.content)

}
