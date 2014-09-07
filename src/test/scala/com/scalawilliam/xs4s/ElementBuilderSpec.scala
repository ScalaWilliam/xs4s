package com.scalawilliam.xs4s

import java.io.{ByteArrayInputStream, InputStream}
import javax.xml.stream.events.{XMLEvent, StartElement}
import javax.xml.stream.{XMLEventReader, XMLInputFactory}

import com.scalawilliam.xs4s.{XmlEventIterator, ElementBuilder}
import org.scalatest.{Inside, Inspectors, Matchers, WordSpec}

import scala.xml._

class ElementBuilderSpec extends WordSpec with Matchers with Inspectors with Inside {

  /**
   * The purpose of ElementBuilder is to
   * turn a (StartElement, ...) into a scala.xml.Elem
   */

  "Element builder spec" must {
    "Pass a sanity test of a complex XML" in {

      val input =
        """<stuff xmlns="urn:level:1" xmlns:n="urn:level:2">
          |<![CDATA[&]]>&amp;
          |<m:more xmlns:m="urn:level:3">
          |One bit <!-- of commentary -->
          |<n:extra n:attribute="good" attr="value&quot;" m:other-chars="&amp;&apos;&gt;&lt;"/>
          |</m:more>
          |</stuff>
        """.stripMargin

      val is = new ByteArrayInputStream(input.getBytes("UTF-8"))
      val inputFactory = XMLInputFactory.newInstance()
      val streamer = inputFactory.createXMLEventReader(is)
      import ElementBuilder.eventReaderExtractors
      val tree = streamer.blockingElement.next()

      inside(tree) {
        case Elem(prefix, label, attributes, scope, child @ _*) =>
          prefix shouldBe null
          label shouldBe "stuff"
          attributes shouldBe Null
          scope shouldBe NamespaceBinding(null, "urn:level:1", NamespaceBinding("n", "urn:level:2", TopScope))
          inside(child.toVector) {
            case Vector(newline: Text, cdata: PCData, ampNl: Text, e: Elem, newline3: Text) =>
              cdata.text shouldBe "&"
              ampNl.text shouldBe "&\n"
              inside(e) {
                case Elem(prefix2, label2, attributes2, scope2, child2 @ _*) =>
                  prefix2 shouldBe "m"
                  label2 shouldBe "more"
                  attributes2 shouldBe Null
                  scope2 shouldBe NamespaceBinding("m", "urn:level:3", TopScope)
                  inside(child2.toVector) {
                    case Vector(nlOneBit: Text, cmt: Comment, newline5: Text, otherE: Elem, newLine6: Text) =>
                      nlOneBit.text shouldBe "\nOne bit "
                      cmt.commentText shouldBe " of commentary "
                      inside(otherE) {
                        case Elem(prefix3, label3, attributes3, scope3, child3 @ _*) =>
                          prefix3 shouldBe "n"
                          label3 shouldBe "extra"
                          inside(attributes3) {
                            case PrefixedAttribute(pre, key, value, next) =>
                              pre shouldBe "n"
                              key shouldBe "attribute"
                              value.text shouldBe "good"
                              inside(next) {
                                case UnprefixedAttribute(key, value, next) =>
                                  key shouldBe "attr"
                                  value.text shouldBe """value""""
                                  inside(next) {
                                    case PrefixedAttribute(pre, key, value, next) =>
                                      pre shouldBe "m"
                                      key shouldBe "other-chars"
                                      value.text shouldBe "&'><"
                                      next shouldBe Null
                                  }
                              }
                          }
                          scope3 shouldBe TopScope
                          child3 shouldBe empty
                      }
                  }
              }
          }
      }

    }

  }

}