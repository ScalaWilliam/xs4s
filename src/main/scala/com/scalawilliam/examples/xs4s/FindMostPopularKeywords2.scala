package com.scalawilliam.examples.xs4s

import java.io._
import javax.xml.stream.XMLInputFactory

import com.scalawilliam.xs4s.{XmlEventIterator, BasicElementExtractorBuilder}

import scala.xml.Elem

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

  val xmlInputFactory = XMLInputFactory.newInstance()
  // Wikipedia abstracts - 4GB
  val anchor = new File("enwiki-20140402-abstract.xml")
  val xmlEventReader = xmlInputFactory.createXMLEventReader(new FileInputStream(anchor))
  val anchorGetter = BasicElementExtractorBuilder { case l if l.contains("anchor") => (e: Elem) => e }

  import XmlEventIterator._
  val datums = xmlEventReader.scanLeft(anchorGetter.initial)(_.process(_)).collect {
    case anchorGetter.Captured(_, d) => d
  }
  val keywordCounts = datums.map(_.text).filter(_.nonEmpty).map(n => Map(n -> 1)).reduce(mergeCountMaps[String])
  val topKeywords = keywordCounts.toList.sortBy{case (keyword, count) => -count}
  println(topKeywords mkString "\n")

}

