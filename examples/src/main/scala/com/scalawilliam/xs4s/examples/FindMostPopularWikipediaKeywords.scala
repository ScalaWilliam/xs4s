package com.scalawilliam.xs4s.examples

import java.net.URL

import com.scalawilliam.xs4s.elementprocessor.XmlStreamElementProcessor

object FindMostPopularWikipediaKeywords extends App {

  // Wikipedia abstracts - 4GB
  val url = new URL("https://dumps.wikimedia.org/enwiki/20140903/enwiki-20140903-abstract.xml")

  // builder that extracts all the anchors
  val anchorSplitter = XmlStreamElementProcessor.collectElements(_.last == "anchor")

  val anchors = {
    import XmlStreamElementProcessor.IteratorCreator._
    val anchorsStream = anchorSplitter.processInputStream(url.openStream())
    // add 'full' as an argument to go through the whole stream
    if (args contains "full") {
      anchorsStream
    } else {
      anchorsStream take 500
    }
  }

  val groupedAnchors = anchors.map(_.text).filter(_.nonEmpty).map(n => Map(n -> 1))

  // only count those anchors that are non-unique
  val keywordCounts = groupedAnchors.reduce(mergeCountMaps[String]).filter(_._2 > 1)

  // and sort them for us
  val topKeywords = keywordCounts.toList.sortBy { case (keyword, count) => -count }

  topKeywords foreach println

  // A simple map-reduce strategy
  def mergeCountMaps[T](a: Map[T, Int], b: Map[T, Int]): Map[T, Int] = {
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
