package xs4s

import javax.xml.stream.events.{StartElement, XMLEvent}
import javax.xml.stream.events.{
  Attribute => JavaAttribute,
  Comment => JavaComment,
  Namespace => JavaNamespace,
  ProcessingInstruction => JavaProcessingInstruction
}
import scala.xml._

/**
  * [[ScalaXmlElemBuilder]] rebuilds a [[scala.xml.Elem]] tree from [[javax.xml.stream.events.XMLEvent]] events.
  * The element is fully rebuilt when the builder becomes [[xs4s.ScalaXmlElemBuilder.FinalElemScala]].
  */
sealed trait ScalaXmlElemBuilder {
  def process(xmlEvent: XMLEvent): ScalaXmlElemBuilder
}

object ScalaXmlElemBuilder {

  def initial: ScalaXmlElemBuilder = NoElem$Scala

  private def xmlEventToPartialElement(xmlEvent: XMLEvent): Option[Elem] =
    PartialFunction.condOpt(xmlEvent) {
      case s: StartElement =>
        startElementToPartialElement(s)
    }

  private def startElementToPartialElement(startElement: StartElement): Elem = {
    import collection.JavaConverters._

    // Namespace prefix cannot be empty string, must be null instead
    val nsBindings = startElement.getNamespaces.asScala
      .collect { case n: JavaNamespace => n }
      .foldRight[NamespaceBinding](TopScope) {
        case (a, b) if a.getPrefix.isEmpty =>
          NamespaceBinding(null, a.getNamespaceURI, b)
        case (a, b) => NamespaceBinding(a.getPrefix, a.getNamespaceURI, b)
      }

    // Attribute prefix cannot be empty string
    val attributes = startElement.getAttributes.asScala
      .collect { case a: JavaAttribute => a }
      .foldRight[MetaData](Null)((a, b) =>
        Option(a.getName.getPrefix).filter(_.nonEmpty) match {
          case Some(p) =>
            new PrefixedAttribute(p, a.getName.getLocalPart, a.getValue, b)
          case None =>
            new UnprefixedAttribute(a.getName.getLocalPart, a.getValue, b)
      })

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

  private def xmlEventToNonElement(xmlEvent: XMLEvent): Option[Node] =
    PartialFunction.condOpt(xmlEvent) {
      case ce if ce.isCharacters && ce.asCharacters().isCData =>
        scala.xml.PCData(ce.asCharacters().getData)
      case ce if ce.isCharacters => scala.xml.Text(ce.asCharacters().getData)
      case pis: JavaProcessingInstruction =>
        scala.xml.ProcInstr(pis.getTarget, pis.getData)
      case cm: JavaComment => scala.xml.Comment(cm.getText)
    }

  trait EventToBuilder {
    def convert(xmlEvent: XMLEvent): Option[ScalaXmlElemBuilder]
  }

  final case class NonElemScala(mostRecent: XMLEvent, reverseList: XMLEvent*)
      extends ScalaXmlElemBuilder {

    def process(xmlEvent: XMLEvent): BuildingElemScala =
      produceBuildingElement(xmlEvent).getOrElse(
        NonElemScala(xmlEvent, Seq(mostRecent) ++ reverseList: _*)
          .process(xmlEvent))

    private def produceBuildingElement(
        xmlEvent: XMLEvent): Option[BuildingElemScala] =
      xmlEventToPartialElement(xmlEvent).map(BuildingElemScala(_))

  }

  final case class FinalElemScala(elem: Elem) extends ScalaXmlElemBuilder {
    def process(xMLEvent: XMLEvent): ScalaXmlElemBuilder =
      NoElem$Scala.process(xMLEvent)
  }

  import javax.xml.stream.events.{
    Attribute => JavaAttribute,
    Comment => JavaComment,
    Namespace => JavaNamespace,
    ProcessingInstruction => JavaProcessingInstruction
  }

  import scala.xml._

  final case class BuildingElemScala(element: Elem, ancestors: Elem*)
      extends ScalaXmlElemBuilder {

    def process(xMLEvent: XMLEvent): ScalaXmlElemBuilder =
      includeChildren
        .convert(xMLEvent)
        .orElse(buildChildElement.convert(xMLEvent))
        .orElse(finaliseElement.convert(xMLEvent))
        .getOrElse(sys.error("Invalid state"))

    private def includeChildren: EventToBuilder =
      event =>
        xmlEventToNonElement(event).map { newChildNode =>
          val newElement = element.copy(child = element.child :+ newChildNode)
          BuildingElemScala(newElement, ancestors: _*)
      }

    private def buildChildElement: EventToBuilder = event => {
      xmlEventToPartialElement(event).map { newChildElement =>
        BuildingElemScala(newChildElement, Seq(element) ++ ancestors: _*)
      }
    }

    private def finaliseElement: EventToBuilder =
      event =>
        PartialFunction.condOpt(event) {
          case e if e.isEndElement && ancestors.nonEmpty =>
            val Seq(first, rest @ _*) = ancestors
            val newElement            = first.copy(child = first.child :+ element)
            BuildingElemScala(newElement, rest: _*)
          case e if e.isEndElement =>
            FinalElemScala(elem = element)
      }

  }

  object Scanner extends Scanner[XMLEvent, ScalaXmlElemBuilder, Elem] {
    def initial: ScalaXmlElemBuilder = NoElem$Scala

    def scan(xmlElementBuilder: ScalaXmlElemBuilder,
             xMLEvent: XMLEvent): ScalaXmlElemBuilder =
      xmlElementBuilder.process(xMLEvent)

    def collect(xmlElementBuilder: ScalaXmlElemBuilder): Option[Elem] =
      PartialFunction.condOpt(xmlElementBuilder) {
        case FinalElemScala(e) => e
      }
  }

  case object NoElem$Scala extends ScalaXmlElemBuilder {
    def process(xMLEvent: XMLEvent): ScalaXmlElemBuilder = xMLEvent match {
      case s: StartElement => BuildingElemScala(startElementToPartialElement(s))
      case any             => NonElemScala(any)
    }
  }

}
