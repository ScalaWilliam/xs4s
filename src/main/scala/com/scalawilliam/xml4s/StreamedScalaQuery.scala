package com.scalawilliam.xml4s

import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}
import javax.xml.stream.XMLEventReader
import scala.xml.Elem
import com.scalawilliam.xml4s.TreeExtractor.Capture

case class TreeExtractor[T](captures: List[Capture[T]]) {
  implicit class iter(reader: XMLEventReader) extends Iterator[XMLEvent] {
    def hasNext: Boolean = reader.hasNext
    def next(): XMLEvent = reader.nextEvent()
  }
  def apply(reader: XMLEventReader): Iterator[T] = {
    apply(reader:Iterator[XMLEvent])
  }
  def apply(events: Iterator[XMLEvent]): Iterator[T] = {
    var currentState: EventProcessor = AtStack(events, stack = Seq.empty)
    events.map{
      event =>
        currentState = currentState.process(event)
        currentState
    }.flatMap {
      case Captured(_, _, results) => results
      case other => Seq.empty
    }
  }
  private trait EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor]
  }
  private case class AtStack(events: Iterator[XMLEvent], stack: Seq[String]) extends EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor] = {
      case startElement: StartElement =>
        val newStack = stack ++ Seq(startElement.getName.getLocalPart)
        captures.find(_.path == newStack) match {
          case Some(callback) =>
            StartCapturing(events, stack = newStack, capture = callback).process(startElement)
          case None =>
            AtStack(events, stack = newStack)
        }
      case endElement: EndElement =>
        AtStack(events, stack = stack.dropRight(1))
      case other =>
        this
    }
  }
  private case class StartCapturing(events: Iterator[XMLEvent], stack: Seq[String], capture: Capture[T]) extends EventProcessor {
    val process: PartialFunction[XMLEvent, EventProcessor] = {
      case startElement: StartElement =>
        val results = capture.map.apply(ElementBuilder.constructTree(startElement, events))
        Captured(events, stack, results)
    }
  }
  private case class Captured(reader: Iterator[XMLEvent], stack: Seq[String], results: Seq[T]) extends EventProcessor {
    val process = AtStack(reader, stack = stack.dropRight(1)).process
  }
}
object TreeExtractor {
  case class Capture[+T](path: Seq[String], map: ElementToSeq[T])
  type ElementToSeq[+T] = Elem => Seq[T]
  case class CaptureBuilder(items: Seq[String]) {
    def apply[T](elementBuilder: ElementToSeq[T]): Capture[T] = Capture(items, elementBuilder)
  }
  object CaptureBuilder {
    implicit class startAtString(a: String) {
      def \(b: String) = CaptureBuilder(Seq(a, b))
    }
    implicit class startAtCaptureBuilder(a: CaptureBuilder) {
      def \(b: String) = CaptureBuilder(a.items :+ b)
    }
  }
}

