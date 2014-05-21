package com.scalawilliam.examples.xml4s

import java.io._
import com.scalawilliam.xml4s.XmlStreamSplitter

object FindMostPopularKeywords extends App {

  val doc = <woot>
    {(1 to 5).map{i => <num>{i}</num>}}
  </woot>

  def mergeCountMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] = {
    val result = a ++ b ++ (for {
      k <- a.keySet & b.keySet
      v = a(k) + b(k)
    } yield k -> v)

    if ( result.size > 5000 ) {
      println(s"Merging ${a.size} + ${b.size} => ${result.size}")
    }

    result
  }
  // Wikipedia abstracts - 4GB
  val anchor = new File("enwiki-20140402-abstract.xml")
  val anchors = XmlStreamSplitter(matchAtTag = _.getName.getLocalPart == "anchor")(new FileInputStream(anchor))
  val groupedAnchors = anchors.map(scala.xml.XML.load).map(_.text).filter(_.nonEmpty).map(n => Map(n -> 1))
  val keywordCounts = groupedAnchors.reduce(mergeCountMaps[String])
  val topKeywords = keywordCounts.toList.sortBy{case (keyword, count) => -count}
  println(topKeywords mkString "\n")

}
