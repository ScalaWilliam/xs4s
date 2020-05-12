package com.scalawilliam.xs4s

import java.io.ByteArrayInputStream
import javax.xml.stream.XMLInputFactory

import org.scalatest.{Inside, Matchers, WordSpec}
import Implicits._
import scala.xml.Elem

/**
 * Purpose of element extractor is to
 * pick apart specific elements from an XML stream as soon as they are matched
 *
 * This version concerns a simple List[ElementName] which does not bother with prefixes and the like
 */
final class BasicElementExtractorBuilderSpec extends WordSpec with Matchers with Inside {
  "Basic element extractor" must {

    val input =
      s"""
         |<items>
         |<embedded><item>Embedded</item></embedded>
         |<item>General</item>
         |<embedded-twice><embedded-once><item>Doubly embedded</item></embedded-once></embedded-twice>
         |<item><item>Nested</item></item>
         |</items>
         |
       """.stripMargin

    val inputFactory = XMLInputFactory.newInstance()

    def process[T](instance: XmlElementExtractor[T]): Vector[T] = {
      val is = new ByteArrayInputStream(input.getBytes("UTF-8"))
      try {
        val streamer = inputFactory.createXMLEventReader(is)
        try streamer.toIterator.scanCollect(instance.Scan).toVector
        finally streamer.close()
      } finally is.close()
    }

    implicit class builderProcess[T](i: XmlElementExtractor[T]) {
      def materialize = process[T](i)
    }

    "Not match any elements" in {
      val extractor = XmlElementExtractor({ case List("fail") => (e: Elem) => e })
      extractor.materialize shouldBe empty
    }
    "Match /items/item" in {
      val extractor = XmlElementExtractor({
        case List("items", "item") => (e: Elem) => e
      })
      extractor.materialize.map(_.toString) should contain only(
        "<item>General</item>",
        "<item><item>Nested</item></item>"
      )
    }
    "Match /items/item and /items/embedded/item" in {
      XmlElementExtractor {
        case List("items", "item") => (e: Elem) => e
        case List("items", "embedded", "item") => (e: Elem) => e
      }
        .materialize.map(_.toString) should contain only(
        "<item>Embedded</item>",
        "<item>General</item>",
        "<item><item>Nested</item></item>"
      )
    }
  }

}
