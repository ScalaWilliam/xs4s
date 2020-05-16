package xs4s

import javax.xml.stream.events.{EndElement, StartElement, XMLEvent}
import xs4s.generic.Scanner

import scala.xml.Elem

object XmlElementExtractor {

  /** Materialises the element and all its contents as soon as the specified name is reached */
  def filterElementsByName(name: String): XmlElementExtractor[Elem] =
    filterElementsByPredicate(elementTree =>
      elementTree.lastOption.map(_.getName.getLocalPart).contains(name))

  /** Looks at the ancenstry of the element. Newest element is last. */
  def filterElementsByPredicate[T](
      p: Vector[StartElement] => Boolean): XmlElementExtractor[Elem] =
    new XmlElementExtractor(lse => if (p(lse)) Some(identity) else None)

  def captureWithPartialFunctionOfElementNames[T](
      pf: PartialFunction[Vector[String], Elem => T]): XmlElementExtractor[T] =
    new XmlElementExtractor[T](l => pf.lift(l.map(_.getName.getLocalPart)))

  def collectWithPartialFunction[T](
      pf: PartialFunction[Vector[StartElement], Elem => T])
    : XmlElementExtractor[T] =
    new XmlElementExtractor[T](l => pf.lift(l))

  def captureRoot: XmlElementExtractor[Elem] =
    new XmlElementExtractor(_ => Some(identity))

}

/**
  * The [[XmlElementExtractor]] continuously builds a tree of XML elements, and once a
  * desired match is found (as specified by extractionFunction), it will materialise the rest of the element,
  * and then call a function to transform it.
  *
  * The reason to return a [[T]] rather than a plain {{{Option[Elem]}}} is because
  * the function Elem => T may actually depend on the StartElement, for example:
  * if we have 2 types of StartElements we want to capture, we would like to return a different
  * function to process them, and then for example, return an {{{Either[X, Y]}}}.
  * Basically we want immediate processing here.
  */
final class XmlElementExtractor[T](
    extractionFunction: Vector[StartElement] => Option[Elem => T]) {

  def scannerThrowingOnError: Scanner[XMLEvent, EventProcessor, T] =
    Scanner.of(EventProcessor.initial)((processor, event: XMLEvent) =>
      processor.process(event).getOrElse(processor))(
      _.fold(whenCaptured = output => Some(output),
             whenErrored = error => throw error,
             otherwise = None))

  def scannerEitherOnError
    : Scanner[XMLEvent, EventProcessor, Either[XmlStreamError, T]] =
    Scanner.of(EventProcessor.initial)((processor, event: XMLEvent) =>
      processor.process(event).getOrElse(processor))(
      _.fold(whenCaptured = output => Some(Right(output)),
             whenErrored = error => Some(Left(error)),
             otherwise = None))

  sealed trait EventProcessor {
    def process(xmlEvent: XMLEvent): Option[EventProcessor]

    def fold[V](whenCaptured: T => V,
                whenErrored: XmlStreamError => V,
                otherwise: => V): V = this match {
      case EventProcessor.Errored(_, exception) =>
        whenErrored(exception)
      case EventProcessor.Captured(_, data) => whenCaptured(data)
      case _                                => otherwise
    }
  }

  private object EventProcessor {

    def initial: EventProcessor = ProcessingStack()

    final case class Errored(capturing: Capturing, error: XmlStreamError)
        extends EventProcessor {
      override def process(xmlEvent: XMLEvent): Option[EventProcessor] =
        Some(this)
    }

    final case class Captured(stack: Vector[StartElement], data: T)
        extends EventProcessor {

      /**
        * The next XMLEvent that happens forces us to drop the captured stack element
        * and ascent to its parent, hence the .dropRight(1)
        */
      def process(xmlEvent: XMLEvent): Option[EventProcessor] =
        ProcessingStack(stack.dropRight(1): _*).process(xmlEvent)
    }

    final case class Capturing(stack: Vector[StartElement],
                               state: ScalaXmlElemBuilder,
                               callback: Elem => T)
        extends EventProcessor {
      def process(xmlEvent: XMLEvent): Option[EventProcessor] = Option {
        state
          .process(xmlEvent)
          .fold(
            whenFinal = elem => Captured(stack, callback(elem)),
            whenError = error => Errored(this, error),
            otherwise = other => Capturing(stack, other, callback)
          )
      }
    }

    final case class ProcessingStack(stack: StartElement*)
        extends EventProcessor {
      def process(xmlEvent: XMLEvent): Option[EventProcessor] =
        PartialFunction.condOpt(xmlEvent) {
          case startElement: StartElement =>
            val newStack = stack ++ Seq(startElement)
            extractionFunction(newStack.toVector)
              .map { f =>
                Capturing(
                  stack = newStack.toVector,
                  state = ScalaXmlElemBuilder.initial.process(startElement),
                  callback = f
                )
              }
              .getOrElse(ProcessingStack(newStack: _*))
          case _: EndElement =>
            val newStack = stack.dropRight(1)
            if (newStack.isEmpty) {
              FinishedProcessing
            } else {
              ProcessingStack(newStack: _*)
            }
          case _ => this
        }
    }

    case object FinishedProcessing extends EventProcessor {
      override def process(xmlEvent: XMLEvent): Option[EventProcessor] =
        Some(FinishedProcessing)
    }

  }

}
