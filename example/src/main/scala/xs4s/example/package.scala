package xs4s

import java.net.URL

import fs2.Pipe

import scala.xml.Elem
import zio.stream.ZStream

package object example {

  val anchorExtractor: XmlElementExtractor[Elem] =
    XmlElementExtractor.filterElementsByName("anchor")

  val wikipediaAbstractURL = new URL(
    s"https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-abstract.xml.gz")

  // A simple map-reduce strategy
  def mergeCountMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] =
    a ++ b ++ (for {
      k <- a.keySet & b.keySet
      v = a(k) + b(k)
    } yield k -> v)

  def countTopItemsFs2[F[_], I]: Pipe[F, I, List[(I, Int)]] =
    _.map(v => Map(v -> 1))
      .reduce(mergeCountMaps[I])
      .map(_.filter(_._2 > 1))
      .map(_.toList.sortBy { case (keyword, count) => -count })

  def countTopItemsZIO[R, I]: ZStream[R, Throwable, I] => ZStream[R, Throwable, List[(I, Int)]] = { s =>
    val res = s.runFold(Map.empty[I, Int].withDefaultValue(0)){ (acc, v) => acc.updated(v, acc(v)+1) }
      .map(_.filter(_._2 > 1))
      .map(_.toList.sortBy { case (keyword, count) => -count })

    ZStream.fromZIO(res)
  }

  def countTopItemsIterator[I]: Iterator[I] => List[(I, Int)] =
    _.map(v => Map(v -> 1))
      .reduce(mergeCountMaps[I])
      .filter(_._2 > 1)
      .toList
      .sortBy { case (keyword, count) => -count }

}
