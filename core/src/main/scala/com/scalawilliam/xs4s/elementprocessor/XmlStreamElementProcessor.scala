package com.scalawilliam.xs4s.elementprocessor

import java.io.InputStream
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}

import com.scalawilliam.xs4s.XmlEventIterator
import com.scalawilliam.xs4s.elementbuilder.{FinalElement, NoElement, XmlBuilder}
import com.scalawilliam.xs4s.elementprocessor.XmlStreamElementProcessor.CollectorDefinition

import scala.xml.Elem

object XmlStreamElementProcessor {

  lazy val xmlInputfactory = XMLInputFactory.newInstance()

  object IteratorCreator {
    implicit class addIteratorCreateorToBasicElementExtractorBuilder[T](beeb: XmlStreamElementProcessor[T]) {
      def processInputStream(inputStream: InputStream): Iterator[T] = {
        val reader = XmlStreamElementProcessor.xmlInputfactory.createXMLEventReader(inputStream)
        import XmlEventIterator._
        reader.scanLeft(beeb.initial)(_.process(_)).collect{case beeb.Captured(_, anchorElement) => anchorElement}
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

  def collectElements(first: List[String] => Boolean, rest: (List[String] => Boolean)*) = {

    XmlStreamElementProcessor(
      { case list if first(list) => (e: Elem) => e },
      rest.map(f =>
        { case list if f(list) => (e: Elem) => e }: CollectorDefinition[Elem]
      ) :_*
    )
  }

}
/**
 * See tests for examples.
 * @tparam T    Return type of these capture converters
 */
case class XmlStreamElementProcessor[T](first: CollectorDefinition[T],
                                        rest: CollectorDefinition[T]*) {
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