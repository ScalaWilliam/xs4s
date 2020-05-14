package xs4s

import java.net.URL

import fs2.Pipe

import scala.xml.Elem

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

  def countTopItems[F[_], I]: Pipe[F, I, List[(I, Int)]] =
    _.map(v => Map(v -> 1))
      .reduce(mergeCountMaps[I])
      .map(_.filter(_._2 > 1))
      .map(_.toList.sortBy { case (keyword, count) => -count })

}
