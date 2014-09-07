package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.{XMLEvent, StartElement}

import scala.xml.Elem

/**
 * Build 'Elem' out of 'XMLEvent'. See tests for examples.
 */
object ElementBuilder {

  import javax.xml.stream.events.{
    Attribute => JavaAttribute,
    Comment => JavaComment,
    ProcessingInstruction => JavaProcessingInstruction,
    Namespace => JavaNamespace
  }

  implicit class eventReaderExtractors(eventReader: XMLEventReader) {
    import XmlEventIterator._
    def xmlBuilders = eventReader.toIterator.xmlBuilders
    def blockingFinal = eventReader.toIterator.blockingFinal
    def blockingElement = eventReader.toIterator.blockingElement
  }

  implicit class extractors(input: Iterator[XMLEvent]) {
    def xmlBuilders: Iterator[XmlBuilder] = {
      input.scanLeft(NoElement: XmlBuilder)(_.process(_))
    }
    def blockingFinal: Iterator[FinalElement] = {
      xmlBuilders.collect {
        case f: FinalElement => f
      }
    }
    def blockingElement: Iterator[Elem] = {
      blockingFinal.collect {
        case FinalElement(e) => e
      }
    }
  }

  import scala.xml._

  /**
    * XmlBuilder trait:
    * Takes as input XMLEvent and produces XmlBuilder. There are two states that .process can lead to:
    * BuildingElement(currentElement: Elem, ancestors: Elem*)
    * FinalNode(elem: Elem)
    *
    * By passing in an event to BuildingElement you will get a new BuildingElement back
    * We only expose FinalElement for you though. When you reach a FinalElement,
    * Capture the output - this will be the fully formed tree.
    * Then you can start the process all over again.
    *
    */
  sealed trait XmlBuilder {
    type EventToBuilder = PartialFunction[XMLEvent, XmlBuilder]
    def process: EventToBuilder
  }

  case object NoElement extends XmlBuilder {
    val process: EventToBuilder = {
      case s: StartElement => BuildingElement(startElementToPartialElement.apply(s))
      case any => NonElement(any)
    }
  }

  case class NonElement(mostRecent: XMLEvent, reverseList: XMLEvent*) extends XmlBuilder {

    val process: EventToBuilder = produceBuildingElement orElse appendNonElement

    private lazy val produceBuildingElement: PartialFunction[XMLEvent, BuildingElement] = xmlEventToPartialElement andThen {
      case e => BuildingElement(e)
    }
    private lazy val appendNonElement: PartialFunction[XMLEvent, NonElement] = {
      case e => NonElement(e, Seq(mostRecent) ++ reverseList :_*)
    }
  }

  case class FinalElement(elem: Elem) extends XmlBuilder {
    val process: EventToBuilder = NoElement.process
  }

  case class BuildingElement(element: Elem, ancestors: Elem*) extends XmlBuilder {

    val process: EventToBuilder =
      addChildren orElse buildChildElement orElse finaliseElement

    private lazy val addChildren: EventToBuilder = xmlEventToNonElement andThen {
      case newChildNode =>
        val newElement = element.copy(child = element.child :+ newChildNode)
        BuildingElement(newElement, ancestors :_*)
    }

    private lazy val buildChildElement: EventToBuilder = xmlEventToPartialElement andThen {
      case newChildElement: Elem =>
        BuildingElement(newChildElement, Seq(element) ++ ancestors: _*)
    }

    private lazy val finaliseElement: EventToBuilder = {
      case e if e.isEndElement && ancestors.nonEmpty =>
        val Seq(first, rest @ _*) = ancestors
        val newElement = first.copy(child = first.child :+ element)
        BuildingElement(newElement, rest :_*)
      case e if e.isEndElement =>
        FinalElement(elem = element)
    }
  }

  private val startElementToPartialElement: PartialFunction[StartElement, Elem] = {
    case startElement: StartElement =>
      import collection.JavaConverters._

      // Namespace prefix cannot be empty string, must be null instead
      val nsBindings = startElement.getNamespaces.asScala.collect{case n: JavaNamespace => n}.foldRight[NamespaceBinding](TopScope) {
        case (a, b) if a.getPrefix.isEmpty => NamespaceBinding(null, a.getNamespaceURI, b)
        case (a, b) => NamespaceBinding(a.getPrefix, a.getNamespaceURI, b)
      }

      // Attribute prefix cannot be empty string
      val attributes = startElement.getAttributes.asScala.collect{case a: JavaAttribute => a}.foldRight[MetaData](Null)((a, b) =>
        Option(a.getName.getPrefix).filter(_.nonEmpty) match {
          case Some(p) => new PrefixedAttribute(p, a.getName.getLocalPart, a.getValue, b)
          case None => new UnprefixedAttribute(a.getName.getLocalPart, a.getValue, b)
        }
      )

      // Scala XML requires a null prefix, doesn't like empty string
      val prefix = {
        val javaPrefix = startElement.getName.getPrefix
        if ( javaPrefix.isEmpty ) null else javaPrefix
      }

      Elem(
        prefix = prefix,
        label = startElement.getName.getLocalPart,
        attributes = attributes,
        scope = nsBindings,
        minimizeEmpty = false
      )
  }

  private val xmlEventToPartialElement: PartialFunction[XMLEvent, Elem] = {
    val getStart: PartialFunction[XMLEvent, StartElement] = {
      case s: StartElement => s
    }
    getStart andThen startElementToPartialElement
  }

  private val xmlEventToNonElement: PartialFunction[XMLEvent, Node] = {
    case ce if ce.isCharacters && ce.asCharacters().isCData => PCData(ce.asCharacters().getData)
    case ce if ce.isCharacters => Text(ce.asCharacters().getData)
    case pis: JavaProcessingInstruction =>
      ProcInstr(pis.getTarget, pis.getData)
    case cm :JavaComment => Comment(cm.getText)
  }

}
