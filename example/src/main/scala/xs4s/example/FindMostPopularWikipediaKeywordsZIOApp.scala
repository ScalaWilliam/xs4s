package xs4s.example

import zio._
import zio.stream._

import java.io.{IOException}
import xs4s.ziocompat._
import xs4s.syntax.zio._

object FindMostPopularWikipediaKeywordsZIOApp extends zio.ZIOAppDefault {

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {

    getArgs.flatMap { args =>
      wikipediaXmlBytes
        .viaFunction((byteStream: ZStream[Scope, Throwable, Byte]) => byteStreamToXmlEventStream()(byteStream))
        .viaFunction(anchorExtractor.toZIOPipeThrowError(_))
        .viaFunction(stream => // by default, get a sub-set of the stream
          if (args.contains("full")) stream else stream.take(500)
        )
        .map(_.text)
        .viaFunction(countTopItemsZIO(_))
        .flatMap(counts => ZStream.fromIterable(counts))
        .foreach { case (elem: String, count: Int) => zio.Console.printLine((count, elem).toString) }
        .orDie
    }
  }

  /**
    * Can be any of your own XML byte stream
    */
  private val wikipediaXmlBytes: ZStream[Scope, IOException, Byte] = {
    ZStream.fromInputStream(wikipediaAbstractURL.openStream(),1024).via(ZPipeline.gunzip().orDie)
  }
}
