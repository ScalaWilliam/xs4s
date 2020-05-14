package xs4s

import xs4s.syntax._
import java.io.ByteArrayInputStream

import javax.xml.stream.XMLInputFactory
import org.scalatest.Inside._
import org.scalatest.OptionValues._
import org.scalatest.wordspec.AnyWordSpec

import scala.xml._

final class ElementBuilderSpec extends AnyWordSpec {

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

      val is           = new ByteArrayInputStream(input.getBytes("UTF-8"))
      val inputFactory = XMLInputFactory.newInstance()
      val streamer     = inputFactory.createXMLEventReader(is)
      val tree         = streamer.buildElement.value

      inside(tree) {
        case Elem(prefix, label, attributes, scope, child @ _*) =>
          assert(prefix == null)
          assert(label == "stuff")
          assert(attributes == Null)
          assert(
            scope == NamespaceBinding(
              null,
              "urn:level:1",
              NamespaceBinding("n", "urn:level:2", TopScope)))
          inside(child.toVector) {
            case Vector(newline: Text,
                        cdata: PCData,
                        ampNl: Text,
                        e: Elem,
                        newline3: Text) =>
              assert(cdata.text == "&")
              assert(ampNl.text == "&\n")
              inside(e) {
                case Elem(prefix2, label2, attributes2, scope2, child2 @ _*) =>
                  assert(prefix2 == "m")
                  assert(label2 == "more")
                  assert(attributes2 == Null)
                  assert(
                    scope2 == NamespaceBinding("m", "urn:level:3", TopScope))
                  inside(child2.toVector) {
                    case Vector(nlOneBit: Text,
                                cmt: Comment,
                                newline5: Text,
                                otherE: Elem,
                                newLine6: Text) =>
                      assert(nlOneBit.text == "\nOne bit ")
                      assert(cmt.commentText == " of commentary ")
                      inside(otherE) {
                        case Elem(prefix3,
                                  label3,
                                  attributes3,
                                  scope3,
                                  child3 @ _*) =>
                          assert(prefix3 == "n")
                          assert(label3 == "extra")
                          inside(attributes3) {
                            case PrefixedAttribute(pre, key, value, next) =>
                              assert(pre == "n")
                              assert(key == "attribute")
                              assert(value.text == "good")
                              inside(next) {
                                case UnprefixedAttribute(key, value, next) =>
                                  assert(key == "attr")
                                  assert(value.text == """value"""")
                                  inside(next) {
                                    case PrefixedAttribute(pre,
                                                           key,
                                                           value,
                                                           next) =>
                                      assert(pre == "m")
                                      assert(key == "other-chars")
                                      assert(value.text == "&'><")
                                      assert(next == Null)
                                  }
                              }
                          }
                          assert(scope3 == TopScope)
                          assert(child3.isEmpty)
                      }
                  }
              }
          }
      }

    }

  }

}
