package com.scalawilliam.xs4s

import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}

import com.scalawilliam.xs4s.XmlElementBuilder.{FinalElement, NoElement}

import scala.xml.Elem

object XmlElementExtractor {

  def collectElements[T](p: List[String] => Boolean): XmlElementExtractor[Elem] = {
    XmlElementExtractor {
      case l if p(l) => identity
    }
  }

}

/**
 * See tests for examples.
 *
 * @tparam T Return type of these capture converters
 */
case class XmlElementExtractor[T](pf: PartialFunction[List[String], Elem => T]) {

  def initial: EventProcessor = EventProcessor.initial

  sealed trait EventProcessor {
    def process: PartialFunction[XMLEvent, EventProcessor]
  }

  object Scan extends Scanner[XMLEvent, EventProcessor, T] {
    def initial: EventProcessor = EventProcessor.initial

    def scan(eventProcessor: EventProcessor, xMLEvent: XMLEvent): EventProcessor = eventProcessor.process(xMLEvent)

    def collect: PartialFunction[EventProcessor, T] = {
      case EventProcessor.Captured(_, e) => e
    }
  }

  object EventProcessor {

    def initial: EventProcessor = ProcessingStack()

    case class Captured(stack: List[String], data: T) extends EventProcessor {
      def process: PartialFunction[XMLEvent, EventProcessor] =
        ProcessingStack(stack.dropRight(1): _*).process
    }

    case class Capturing(stack: List[String], state: XmlElementBuilder, callback: Elem => T) extends EventProcessor {
      def process: PartialFunction[XMLEvent, EventProcessor] = {
        case e => state.process(e) match {
          case FinalElement(elem) =>
            Captured(stack, callback(elem))
          case other =>
            Capturing(stack, other, callback)
        }
      }
    }

    case class ProcessingStack(stack: String*) extends EventProcessor {
      def process: PartialFunction[XMLEvent, EventProcessor] = {
        case startElement: StartElement =>
          val newStack = stack ++ Seq(startElement.getName.getLocalPart)
          pf.lift.apply(newStack.toList).map { f =>
            Capturing(
              stack = newStack.toList,
              state = NoElement.process(startElement),
              callback = f
            )
          }.getOrElse(ProcessingStack(newStack: _*))
        case endElement: EndElement =>
          val newStack = stack.dropRight(1)
          if (newStack.isEmpty) {
            FinishedProcessing
          } else {
            ProcessingStack(newStack: _*)
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

}
