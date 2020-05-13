package com.scalawilliam.xs4s

import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}

import com.scalawilliam.xs4s.XmlElementBuilder.{FinalElement, NoElement}

import scala.xml.Elem

object XmlElementExtractor {

  def collectElementsByName(name: String): XmlElementExtractor[Elem] = {
    collectElementsPredicate(l => l.lastOption.contains(name))
  }

  def collectElementsPredicate[T](p: List[String] => Boolean): XmlElementExtractor[Elem] = {
    fromPartialFunction {
      case l if p(l) => identity
    }
  }

  def fromPartialFunction[T](pf: PartialFunction[List[String], Elem => T]): XmlElementExtractor[T] = {
    XmlElementExtractor[T](l => pf.lift(l))
  }

}

/**
 * See tests for examples.
 *
 * @tparam T Return type of these capture converters
 */
final case class XmlElementExtractor[T](extractionFunction: List[String] => Option[Elem => T]) {

  def initial: EventProcessor = EventProcessor.initial

  sealed trait EventProcessor {
    def process(xmlElemv: XMLEvent): Option[EventProcessor]
  }

  object Scan extends Scanner[XMLEvent, EventProcessor, T] {
    def initial: EventProcessor = EventProcessor.initial

    def scan(eventProcessor: EventProcessor, xMLEvent: XMLEvent): EventProcessor = eventProcessor.process(xMLEvent).getOrElse(eventProcessor)

    def collect(eventProcessor: EventProcessor): Option[T] = PartialFunction.condOpt(eventProcessor) {
      case EventProcessor.Captured(_, e) => e
    }
  }

  object EventProcessor {

    def initial: EventProcessor = ProcessingStack()

    final case class Captured(stack: List[String], data: T) extends EventProcessor {
      def process(xmlEvent: XMLEvent): Option[EventProcessor] =
        ProcessingStack(stack.dropRight(1): _*).process(xmlEvent)
    }

    final case class Capturing(stack: List[String], state: XmlElementBuilder, callback: Elem => T) extends EventProcessor {
      def process(xmlEvent: XMLEvent): Option[EventProcessor] = Option {
        state.process(xmlEvent) match {
          case FinalElement(elem) =>
            Captured(stack, callback(elem))
          case other =>
            Capturing(stack, other, callback)
        }
      }
    }

    final case class ProcessingStack(stack: String*) extends EventProcessor {
      def process(xmlEvent: XMLEvent): Option[EventProcessor] = PartialFunction.condOpt(xmlEvent) {
        case startElement: StartElement =>
          val newStack = stack ++ Seq(startElement.getName.getLocalPart)
          extractionFunction.apply(newStack.toList).map { f =>
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
      override def process(xmlElemv: XMLEvent): Option[EventProcessor] = Some(FinishedProcessing)
    }

  }

}
