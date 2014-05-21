package com.scalawilliam.examples.xml4s

import java.io._
import com.scalawilliam.xml4s.ScalaXmlStreamSplitter

object FindMostPopularKeywords2 extends App {

  val doc = <woot>
    {(1 to 5).map{i => <num>{i}</num>}}
  </woot>

  def mergeCountMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] = {
    if ( b.size == 1 ) {
      val (k, v) = b.head
      val result = a.updated(k, a.getOrElse(k, 0) + v)
      if ( result.size % 5000 == 0 && a.size != result.size ) {
        println(s"Merging ${a.size} + ${b.size} => ${result.size}")
      }
      result
    } else {
      val result = a ++ b ++ (for {
        k <- a.keySet & b.keySet
        v = a(k) + b(k)
      } yield k -> v)
      if (result.size > 5000) {
        println(s"Merging ${a.size} + ${b.size} => ${result.size}")
      }
      result
    }
  }

  // Wikipedia abstracts - 4GB
  val anchor = new File("enwiki-20140402-abstract.xml")
  val anchors = ScalaXmlStreamSplitter(matchAtTag = _.getName.getLocalPart == "anchor")(new FileInputStream(anchor))
  val keywordCounts = anchors.map(_.text).filter(_.nonEmpty).map(n => Map(n -> 1)).reduce(mergeCountMaps[String])
  val topKeywords = keywordCounts.toList.sortBy{case (keyword, count) => -count}
  println(topKeywords mkString "\n")

}

