package com.scalawilliam.xs4s

import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}
import com.scalawilliam.xs4s.ElementBuilder.{NoElement, FinalElement, XmlBuilder}
import scala.xml.Elem

object BasicElementExtractorBuilder {
}
/**
 * See tests for examples.
 * @param first Specifier: if the 'xpath' of the current position is equal to the
 *              the List[String] part of this parameter, then we begin capturing
 *              its element. Once that element is captured, we call _2(element)
 *              and change state to Captured().
 * @param rest  We can have any combination of these Specifiers
 *              However they cannot intersect.
 * @tparam T    Return type of these capture converters
 */
case class BasicElementExtractorBuilder[T](first: PartialFunction[List[String], Elem => T], rest: PartialFunction[List[String], Elem => T]*) {
  val captures = List(first) ++ rest

  trait EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor]
  }

  def apply(): EventProcessor = ProcessingStack()
  def initial: EventProcessor = ProcessingStack()

  case class Captured(stack: List[String], data: T) extends EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor] =
      ProcessingStack(stack.dropRight(1) :_*).process
  }

  case class Capturing(stack: List[String], state: XmlBuilder, callback: Elem => T) extends EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor] = 
      state.process andThen {
        case FinalElement(elem) => 
          Captured(stack, callback(elem))
        case other =>
          Capturing(stack, other, callback)
      }
  }
  
  case class ProcessingStack(stack: String*) extends EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor] = {
      case startElement: StartElement =>
        val newStack = stack ++ Seq(startElement.getName.getLocalPart)
        captures.toIterator.map(_.lift(newStack.toList)).collectFirst {
          case Some(callback) =>
            Capturing(
              stack = newStack.toList,
              state = NoElement.process(startElement),
              callback = callback
            )
        }.getOrElse{
          ProcessingStack(newStack :_*)
        }
      case endElement: EndElement =>
        val newStack = stack.dropRight(1)
        if ( newStack.isEmpty ) {
          FinishedProcessing
        } else {
          ProcessingStack(newStack :_*)
        }
      case other => this
    } 
  }

  case object FinishedProcessing extends EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor] = {
      case any => FinishedProcessing
    }
  }

}
//object TreeExtractor {
//  case class Capture[+T](path: Seq[String], map: ElementToSeq[T])
//  type ElementToSeq[+T] = Elem => Seq[T]
//  case class CaptureBuilder(items: Seq[String]) {
//    def apply[T](elementBuilder: ElementToSeq[T]): Capture[T] = Capture(items, elementBuilder)
//  }
//  object CaptureBuilder {
//    implicit class startAtString(a: String) {
//      def \(b: String) = CaptureBuilder(Seq(a, b))
//    }
//    implicit class startAtCaptureBuilder(a: CaptureBuilder) {
//      def \(b: String) = CaptureBuilder(a.items :+ b)
//    }
//  }
//}
//
