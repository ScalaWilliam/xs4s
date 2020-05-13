package com.scalawilliam.xs4s

import javax.xml.stream.events.{StartElement, XMLEvent}

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
sealed trait XmlElementBuilder {
  def process(xmlEvent: XMLEvent): XmlElementBuilder
}

object XmlElementBuilder {

  trait EventToBuilder {
    def convert(xmlEvent: XMLEvent): Option[XmlElementBuilder]
  }

  def initial: XmlElementBuilder = NoElement

  object Scanner extends Scanner[XMLEvent, XmlElementBuilder, Elem] {
    def initial: XmlElementBuilder = NoElement

    def scan(xmlElementBuilder: XmlElementBuilder, xMLEvent: XMLEvent): XmlElementBuilder =
      xmlElementBuilder.process(xMLEvent)

    def collect(xmlElementBuilder: XmlElementBuilder): Option[Elem] = PartialFunction.condOpt(xmlElementBuilder) {
      case FinalElement(e) => e
    }
  }

  final case class NonElement(mostRecent: XMLEvent, reverseList: XMLEvent*) extends XmlElementBuilder {

    def process(xmlEvent: XMLEvent): BuildingElement =
      produceBuildingElement(xmlEvent).getOrElse(NonElement(xmlEvent, Seq(mostRecent) ++ reverseList: _*).process(xmlEvent))

    private def produceBuildingElement(xmlEvent: XMLEvent): Option[BuildingElement] =
      xmlEventToPartialElement(xmlEvent).map(BuildingElement(_))

  }

  case object NoElement extends XmlElementBuilder {
    def process(xMLEvent: XMLEvent): XmlElementBuilder = xMLEvent match {
      case s: StartElement => BuildingElement(startElementToPartialElement(s))
      case any => NonElement(any)
    }
  }

  final case class FinalElement(elem: Elem) extends XmlElementBuilder {
    def process(xMLEvent: XMLEvent): XmlElementBuilder = NoElement.process(xMLEvent)
  }

  final case class BuildingElement(element: Elem, ancestors: Elem*) extends XmlElementBuilder {

    def process(xMLEvent: XMLEvent): XmlElementBuilder =
      includeChildren.convert(xMLEvent).orElse(buildChildElement.convert(xMLEvent)).orElse(finaliseElement.convert(xMLEvent)).getOrElse(sys.error("Invalid state"))

    private def includeChildren: EventToBuilder = event =>
      xmlEventToNonElement(event).map { newChildNode =>
        val newElement = element.copy(child = element.child :+ newChildNode)
        BuildingElement(newElement, ancestors: _*)
      }

    private def buildChildElement: EventToBuilder = event => {
      xmlEventToPartialElement(event).map { newChildElement =>
        BuildingElement(newChildElement, Seq(element) ++ ancestors: _*)
      }
    }

    private def finaliseElement: EventToBuilder = event => PartialFunction.condOpt(event) {
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

  private def xmlEventToPartialElement(xmlEvent: XMLEvent): Option[Elem] = PartialFunction.condOpt(xmlEvent) {
    case s: StartElement =>
      startElementToPartialElement(s)
  }

  private def xmlEventToNonElement(xmlEvent: XMLEvent): Option[Node] = PartialFunction.condOpt(xmlEvent) {
    case ce if ce.isCharacters && ce.asCharacters().isCData => scala.xml.PCData(ce.asCharacters().getData)
    case ce if ce.isCharacters => scala.xml.Text(ce.asCharacters().getData)
    case pis: JavaProcessingInstruction =>
      scala.xml.ProcInstr(pis.getTarget, pis.getData)
    case cm: JavaComment => scala.xml.Comment(cm.getText)
  }

}
