package xs4s

import javax.xml.stream.events.{StartElement, XMLEvent}
import javax.xml.stream.events.{
  Attribute => JavaAttribute,
  Comment => JavaComment,
  Namespace => JavaNamespace,
  ProcessingInstruction => JavaProcessingInstruction
}
import xs4s.ScalaXmlElemBuilder.{CannotProcessEvent, FinalElemScala}
import xs4s.generic.Scanner

import scala.xml._

/**
  * This is a finite-state-machine (FSM) with a Scanner that
  * constructs scala.xml.* from javax.xml.stream.events.XMLEvent
  */
private[xs4s] object ScalaXmlElemBuilder {

  def initial: ScalaXmlElemBuilder = noElem

  def scannerThrowingOnError: Scanner[XMLEvent, ScalaXmlElemBuilder, Elem] =
    Scanner.of(noElem)((state, event: XMLEvent) => state.process(event))(
      _.fold(elem => Some(elem), whenError = err => throw err, _ => None))

  def scannerEitherOnError
    : Scanner[XMLEvent, ScalaXmlElemBuilder, Either[XmlStreamError, Elem]] =
    Scanner.of(noElem)((state, event: XMLEvent) => state.process(event))(
      _.fold(elem => Some(Right(elem)),
             whenError = error => Some(Left(error)),
             _ => None))

  private def noElem: ScalaXmlElemBuilder = NoElem

  private def xmlEventToPartialElement(xmlEvent: XMLEvent): Option[Elem] =
    PartialFunction.condOpt(xmlEvent) {
      case startElement: StartElement =>
        startElementToPartialElement(startElement)
    }

  private def startElementToPartialElement(startElement: StartElement): Elem = {
    import collection.JavaConverters._

    // Namespace prefix cannot be empty string, must be null instead
    val nsBindings = startElement.getNamespaces.asScala
      .collect { case namespace: JavaNamespace => namespace }
      .foldRight[NamespaceBinding](TopScope) {
        case (namespace, namespaceBinding) if namespace.getPrefix.isEmpty =>
          NamespaceBinding(null, namespace.getNamespaceURI, namespaceBinding)
        case (namespace, namespaceBinding) =>
          NamespaceBinding(namespace.getPrefix,
                           namespace.getNamespaceURI,
                           namespaceBinding)
      }

    // Attribute prefix cannot be empty string
    val attributes = startElement.getAttributes.asScala
      .collect { case attribute: JavaAttribute => attribute }
      .foldRight[MetaData](Null)((attribute, metaData) =>
        Option(attribute.getName.getPrefix).filter(_.nonEmpty) match {
          case Some(prefix) =>
            new PrefixedAttribute(prefix,
                                  attribute.getName.getLocalPart,
                                  attribute.getValue,
                                  metaData)
          case None =>
            new UnprefixedAttribute(attribute.getName.getLocalPart,
                                    attribute.getValue,
                                    metaData)
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
      case pcData if pcData.isCharacters && pcData.asCharacters().isCData =>
        scala.xml.PCData(pcData.asCharacters().getData)
      case textData if textData.isCharacters =>
        scala.xml.Text(textData.asCharacters().getData)
      case processingInstruction: JavaProcessingInstruction =>
        scala.xml.ProcInstr(processingInstruction.getTarget,
                            processingInstruction.getData)
      case comment: JavaComment => scala.xml.Comment(comment.getText)
    }

  private trait EventToBuilder {
    def convert(xmlEvent: XMLEvent): Option[ScalaXmlElemBuilder]
  }

  private final case class NonElemScala(mostRecent: XMLEvent,
                                        reverseList: XMLEvent*)
      extends ScalaXmlElemBuilder {

    def process(xmlEvent: XMLEvent): BuildingElemScala =
      produceBuildingElement(xmlEvent).getOrElse(
        NonElemScala(xmlEvent, Seq(mostRecent) ++ reverseList: _*)
          .process(xmlEvent))

    private def produceBuildingElement(
        xmlEvent: XMLEvent): Option[BuildingElemScala] =
      xmlEventToPartialElement(xmlEvent).map(BuildingElemScala(_))

  }

  private final case class CannotProcessEvent(currentState: BuildingElemScala,
                                              xmlEvent: XMLEvent)
      extends Exception
      with ScalaXmlElemBuilder {
    override def getMessage: String =
      s"Cannot process event ${xmlEvent} due to invalid sequence. Tree was: ${currentState.elementsTopDown
        .map((elem: Elem) => elem.label)
        .mkString("> ")}"
    override def process(xmlEvent: XMLEvent): ScalaXmlElemBuilder = this
  }

  private final case class FinalElemScala(elem: Elem)
      extends ScalaXmlElemBuilder {
    def process(xMLEvent: XMLEvent): ScalaXmlElemBuilder =
      NoElem.process(xMLEvent)
  }

  import scala.xml._

  private final case class BuildingElemScala(element: Elem, ancestors: Elem*)
      extends ScalaXmlElemBuilder {
    def elementsTopDown: List[Elem] = (element :: ancestors.toList).reverse

    def process(xmlEvent: XMLEvent): ScalaXmlElemBuilder = {
      def includeChildren: Option[BuildingElemScala] =
        xmlEventToNonElement(xmlEvent).map { newChildNode =>
          val newElement = element.copy(child = element.child :+ newChildNode)
          BuildingElemScala(newElement, ancestors: _*)
        }

      def buildChildElement: Option[BuildingElemScala] =
        xmlEventToPartialElement(xmlEvent).map { newChildElement =>
          BuildingElemScala(newChildElement, Seq(element) ++ ancestors: _*)
        }

      def finaliseElement: Option[ScalaXmlElemBuilder] =
        PartialFunction.condOpt(xmlEvent) {
          case e if e.isEndElement && ancestors.nonEmpty =>
            val Seq(first, rest @ _*) = ancestors
            val newElement            = first.copy(child = first.child :+ element)
            BuildingElemScala(newElement, rest: _*)
          case e if e.isEndElement =>
            FinalElemScala(elem = element)
        }

      includeChildren
        .orElse(buildChildElement)
        .orElse(finaliseElement)
        .getOrElse(CannotProcessEvent(this, xmlEvent))

    }

  }

  private case object NoElem extends ScalaXmlElemBuilder {
    def process(xMLEvent: XMLEvent): ScalaXmlElemBuilder = xMLEvent match {
      case startElement: StartElement =>
        BuildingElemScala(startElementToPartialElement(startElement))
      case anyOther => NonElemScala(anyOther)
    }
  }

}

/**
  * [[ScalaXmlElemBuilder]] rebuilds a [[scala.xml.Elem]] tree from [[javax.xml.stream.events.XMLEvent]] events.
  * The element is fully rebuilt when the builder becomes [[xs4s.ScalaXmlElemBuilder.FinalElemScala]].
  */
sealed trait ScalaXmlElemBuilder {
  def process(xmlEvent: XMLEvent): ScalaXmlElemBuilder

  def fold[T](whenFinal: Elem => T,
              whenError: XmlStreamError => T,
              otherwise: ScalaXmlElemBuilder => T): T = this match {
    case FinalElemScala(elem) => whenFinal(elem)
    case CannotProcessEvent(_, _) =>
      whenError(XmlStreamError.InvalidSequenceOfParserEvents)
    case _ => otherwise(this)
  }
}
