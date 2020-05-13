package xs4s.examples

import java.net.URL

import cats.effect.{Blocker, ExitCode, IO, IOApp, Resource}
import javax.xml.stream.{XMLEventReader, XMLInputFactory}
import javax.xml.stream.events.XMLEvent
import cats.implicits._
import xs4s._
import xs4s.syntax._

import scala.xml.Elem

object FindMostPopularWikipediaKeywords extends IOApp {

  private val anchorSplitter: XmlElementExtractor[Elem] =
    XmlElementExtractor.filterElementsByName("anchor")

  def run(args: List[String]): IO[ExitCode] =
    Blocker[IO].use { blocker =>
      val anchors: fs2.Stream[IO, Elem] = wikipediaXmlBytes(blocker)
        .through(byteStreamToXmlEventStream(blocker))
        .through(anchorSplitter.Scan.toFs2Pipe)
        .through(stream =>
          if (args.contains("full")) stream else stream.take(500))

      val groupedAnchors = anchors
        .map(_.text)
        .filter(_.nonEmpty)
        .map(n => Map(n -> 1))

      // compute top keywords
      val topKeywords = groupedAnchors
        .reduce(mergeCountMaps[String])
        .map(_.filter(_._2 > 1))
        .map(_.toList.sortBy { case (keyword, count) => -count })

      topKeywords
        .evalMap(list => list.traverse_(item => IO.delay(println(item))))
        .compile
        .drain
        .as(ExitCode.Success)

    }

  private def wikipediaXmlBytes(blocker: Blocker): fs2.Stream[IO, Byte] =
    fs2.io
      .readInputStream(
        fis = IO.delay(
          new URL(
            s"https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-abstract.xml.gz")
            .openStream()),
        chunkSize = 1024,
        blocker = blocker,
        closeAfterUse = true
      )
      .through(fs2.compress.gunzip(bufferSize = 1024))

  // A simple map-reduce strategy
  private def mergeCountMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] =
    a ++ b ++ (for {
      k <- a.keySet & b.keySet
      v = a(k) + b(k)
    } yield k -> v)

}
