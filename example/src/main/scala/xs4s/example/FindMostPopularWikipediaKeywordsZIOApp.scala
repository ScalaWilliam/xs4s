package xs4s.example

import zio._
import zio.console._
import zio.stream._
import zio.blocking.Blocking
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import java.util.zip.GZIPInputStream
import scala.collection.JavaConverters._
import xs4s.ziocompat._
import xs4s.syntax.zio._

object FindMostPopularWikipediaKeywordsZIOApp extends zio.App {

  def run(args: List[String]): URIO[ZEnv, ExitCode] = {

    wikipediaXmlBytes
      .via(byteStream => byteStreamToXmlEventStream()(byteStream))
      .via(anchorExtractor.toZIOPipeThrowError(_))
      .via(stream => // by default, get a sub-set of the stream
        if (args.contains("full")) stream else stream.take(500)
      )
      .map(_.text)
      .via(countTopItemsZIO(_))
      .flatMap(counts => ZStream.fromIterable(counts))
      .foreach{ case (elem: String, count: Int) => putStrLn((count, elem).toString) }
      .as(ExitCode.success)
      .orDie
  }

  /**
    * Can be any of your own XML byte stream
    */  
  private val wikipediaXmlBytes: ZStream[Blocking, IOException, Byte] = {
    val is: ZIO[Blocking, IOException, InputStream] = blocking.effectBlockingIO {
      // TODO: Use ZIO gzip transducer after it ships:
      //   https://github.com/zio/zio/issues/3759
      new GZIPInputStream(wikipediaAbstractURL.openStream())
    }

    Stream.fromInputStreamEffect(is, 1024)
  }
}
