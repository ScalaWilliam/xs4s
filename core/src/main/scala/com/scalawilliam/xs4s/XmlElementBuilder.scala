package com.scalawilliam.xs4s

import javax.xml.stream.events.{StartElement, XMLEvent}

import scala.xml._

/**
  * Created on 12/07/2015.
  */

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
sealed trait XmlElementBuilder {
  protected type EventToBuilder = PartialFunction[XMLEvent, XmlElementBuilder]

  def process(xmlEvent: XMLEvent): XmlElementBuilder
}

object XmlElementBuilder {

  def initial: XmlElementBuilder = NoElement

  object Scanner extends Scanner[XMLEvent, XmlElementBuilder, Elem] {
    def initial: XmlElementBuilder = NoElement

    def scan(xmlElementBuilder: XmlElementBuilder, xMLEvent: XMLEvent): XmlElementBuilder =
      xmlElementBuilder.process(xMLEvent)

    def collect: PartialFunction[XmlElementBuilder, Elem] = {
      case FinalElement(e) => e
    }
  }

  case class NonElement(mostRecent: XMLEvent, reverseList: XMLEvent*) extends XmlElementBuilder {

    def process(xMLEvent: XMLEvent) = (produceBuildingElement orElse appendNonElement).apply(xMLEvent)

    private def produceBuildingElement: PartialFunction[XMLEvent, BuildingElement] =
      xmlEventToPartialElement andThen {
        e => BuildingElement(e)
      }

    private def appendNonElement: PartialFunction[XMLEvent, NonElement] = {
      case e => NonElement(e, Seq(mostRecent) ++ reverseList: _*)
    }

  }

  case object NoElement extends XmlElementBuilder {
    def process(xMLEvent: XMLEvent) = xMLEvent match {
      case s: StartElement => BuildingElement(startElementToPartialElement(s))
      case any => NonElement(any)
    }
  }

  case class FinalElement(elem: Elem) extends XmlElementBuilder {
    def process(xMLEvent: XMLEvent) = NoElement.process(xMLEvent)
  }

  case class BuildingElement(element: Elem, ancestors: Elem*) extends XmlElementBuilder {

    def process(xMLEvent: XMLEvent) =
      (includeChildren orElse buildChildElement orElse finaliseElement).apply(xMLEvent)

    private def includeChildren: EventToBuilder = xmlEventToNonElement andThen {
      newChildNode =>
        val newElement = element.copy(child = element.child :+ newChildNode)
        BuildingElement(newElement, ancestors: _*)
    }

    private def buildChildElement: EventToBuilder = xmlEventToPartialElement andThen {
      newChildElement =>
        BuildingElement(newChildElement, Seq(element) ++ ancestors: _*)
    }

    private def finaliseElement: EventToBuilder = {
      case e if e.isEndElement && ancestors.nonEmpty =>
        val Seq(first, rest@_*) = ancestors
        val newElement = first.copy(child = first.child :+ element)
        BuildingElement(newElement, rest: _*)
      case e if e.isEndElement =>
        FinalElement(elem = element)
    }

  }

  import javax.xml.stream.events.{Attribute => JavaAttribute, Comment => JavaComment, Namespace => JavaNamespace, ProcessingInstruction => JavaProcessingInstruction}

  import scala.xml._

  private def startElementToPartialElement(startElement: StartElement): Elem = {
    import collection.JavaConverters._

    // Namespace prefix cannot be empty string, must be null instead
    val nsBindings = startElement.getNamespaces.asScala.collect { case n: JavaNamespace => n }.foldRight[NamespaceBinding](TopScope) {
      case (a, b) if a.getPrefix.isEmpty => NamespaceBinding(null, a.getNamespaceURI, b)
      case (a, b) => NamespaceBinding(a.getPrefix, a.getNamespaceURI, b)
    }

    // Attribute prefix cannot be empty string
    val attributes = startElement.getAttributes.asScala.collect { case a: JavaAttribute => a }.foldRight[MetaData](Null)((a, b) =>
      Option(a.getName.getPrefix).filter(_.nonEmpty) match {
        case Some(p) => new PrefixedAttribute(p, a.getName.getLocalPart, a.getValue, b)
        case None => new UnprefixedAttribute(a.getName.getLocalPart, a.getValue, b)
      }
    )

    // Scala XML requires a null prefix, doesn't like empty string
    val prefix = {
      val javaPrefix = startElement.getName.getPrefix
      if (javaPrefix.isEmpty) null else javaPrefix
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
    case ce if ce.isCharacters && ce.asCharacters().isCData => scala.xml.PCData(ce.asCharacters().getData)
    case ce if ce.isCharacters => scala.xml.Text(ce.asCharacters().getData)
    case pis: JavaProcessingInstruction =>
      scala.xml.ProcInstr(pis.getTarget, pis.getData)
    case cm: JavaComment => scala.xml.Comment(cm.getText)
  }

}
