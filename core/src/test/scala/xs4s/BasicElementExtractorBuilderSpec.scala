package xs4s

import xs4s.syntax.core._
import org.scalatest.{Inside, Matchers, WordSpec}
import scala.xml.Elem

/**
  * Purpose of element extractor is to
  * pick apart specific elements from an XML stream as soon as they are matched
  *
  * This version concerns a simple List[ElementName] which does not bother with prefixes and the like
  */
final class BasicElementExtractorBuilderSpec
    extends WordSpec
    with Matchers
    with Inside {
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

    implicit class builderProcess[T](i: XmlElementExtractor[T]) {
      def materialize: Vector[T] =
        XMLStream.fromString(input).extractWith(i).toVector
    }

    "Not match any elements" in {
      assert {
        XmlElementExtractor
          .captureWithPartialFunctionOfElementNames {
            case Vector("fail") =>
              (e: Elem) =>
                e
          }
          .materialize
          .isEmpty
      }
    }
    "Match /items/item" in {
      val extractor =
        XmlElementExtractor.captureWithPartialFunctionOfElementNames {
          case Vector("items", "item") =>
            (e: Elem) =>
              e
        }
      extractor.materialize.map(_.toString) should contain only (
        "<item>General</item>",
        "<item><item>Nested</item></item>"
      )
    }
    "Match /items/item and /items/embedded/item" in {
      XmlElementExtractor
        .captureWithPartialFunctionOfElementNames {
          case Vector("items", "item") =>
            (e: Elem) =>
              e
          case Vector("items", "embedded", "item") =>
            (e: Elem) =>
              e
        }
        .materialize
        .map(_.toString) should contain only (
        "<item>Embedded</item>",
        "<item>General</item>",
        "<item><item>Nested</item></item>"
      )
    }
  }

}
