package com.scalawilliam.xs4s.elementprocessor

import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}

import com.scalawilliam.xs4s.XmlEventIterator
import com.scalawilliam.xs4s.elementbuilder.XmlBuilder
import com.scalawilliam.xs4s.elementbuilder.XmlBuilder.{FinalElement, NoElement}
import com.scalawilliam.xs4s.elementprocessor.XmlStreamElementProcessor.CollectorDefinition

import scala.xml.Elem

object XmlStreamElementProcessor {

  object IteratorCreator {

    implicit class addIteratorCreateorToBasicElementExtractorBuilder[T](beeb: XmlStreamElementProcessor[T]) {
      def processInputStream(inputStream: InputStream)(implicit xMLInputFactory: XMLInputFactory): Iterator[T] = {
        val reader = xMLInputFactory.createXMLEventReader(inputStream)
        import XmlEventIterator._
        reader.scanLeft(beeb.EventProcessor.Scan.initial)(beeb.EventProcessor.Scan.scan).collect(beeb.EventProcessor.Scan.collect)
      }
    }

  }

  /**
    * Collector Definition: if the 'xpath' of the current position is equal to the
    * the List[String] part of this parameter, then we begin capturing
    * its element. Once that element is captured, we call _2(element)
    * and change state to Captured().
    */
  type CollectorDefinition[T] = PartialFunction[List[String], Elem => T]

  def collectElements[T](pf: CollectorDefinition[T]) = {
    XmlStreamElementProcessor(pf)
  }

}

/**
  * See tests for examples.
  *
  * @tparam T Return type of these capture converters
  */
case class XmlStreamElementProcessor[T](pf: CollectorDefinition[T]) {

  def initial: EventProcessor = EventProcessor.initial

  sealed trait EventProcessor {
    def process: PartialFunction[XMLEvent, EventProcessor]
  }

  object EventProcessor {
    ep =>

    object Scan {
      def initial = ep.initial

      def scan(eventProcessor: EventProcessor, xMLEvent: XMLEvent) = eventProcessor.process(xMLEvent)

      def collect: PartialFunction[EventProcessor, T] = {
        case Captured(_, e) => e
      }
    }

    def initial: EventProcessor = ProcessingStack()

    case class Captured(stack: List[String], data: T) extends EventProcessor {
      def process: PartialFunction[XMLEvent, EventProcessor] =
        ProcessingStack(stack.dropRight(1): _*).process
    }

    case class Capturing(stack: List[String], state: XmlBuilder, callback: Elem => T) extends EventProcessor {
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
