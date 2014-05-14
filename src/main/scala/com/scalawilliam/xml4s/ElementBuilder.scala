package com.scalawilliam.xml4s

import javax.xml.stream.events.{XMLEvent, StartElement}

object ElementBuilder {

  import javax.xml.stream.events.{
    Attribute => JavaAttribute,
    Comment => JavaComment,
    ProcessingInstruction => JavaProcessingInstruction,
    Namespace => JavaNamespace
  }

  import scala.Some
  import scala.xml._

  /** First node must be a StartElement **/
  def constructTree(startElement: StartElement, input: Iterator[XMLEvent]): scala.xml.Elem = {
    var currentState: XmlBuilder = BuildingElement(buildElement apply startElement)
    input.map{x =>
      currentState = currentState.process(x)
      currentState
    }.collectFirst{
      case FinalElement(elem) => elem
    } match {
      case Some(elem) => elem
      case other => throw new IllegalStateException(s"Tree could not be constructed, final processor was: $other")
    }
  }

  def step(builder: XmlBuilder, event: XMLEvent): XmlBuilder =
    builder.process(event)


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

  case class FinalElement(elem: Elem) extends XmlBuilder {
    /** Never stops - you must capture this yourself and start with a fresh builder **/
    val process: EventToBuilder = {
      case _ => this
    }
  }

  val buildElement: PartialFunction[StartElement, Elem] = {
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

  val buildElementFromEvent: PartialFunction[XMLEvent, Elem] = {
    val getStart: PartialFunction[XMLEvent, StartElement] = {
      case s: StartElement => s
    }
    getStart andThen buildElement
  }

  val translateNonElement: PartialFunction[XMLEvent, Node] = {
    case ce if ce.isCharacters && ce.asCharacters().isCData => PCData(ce.asCharacters().getData)
    case ce if ce.isCharacters => Text(ce.asCharacters().getData)
    case pis: JavaProcessingInstruction =>
      ProcInstr(pis.getTarget, pis.getData)
    case cm :JavaComment => Comment(cm.getText)
  }

  private case class BuildingElement(element: Elem, ancestors: Elem*) extends XmlBuilder {

    val addChildren = translateNonElement andThen {
      case newChildNode =>
        val newElement = element.copy(child = element.child :+ newChildNode)
        BuildingElement(newElement, ancestors :_*)
    }

    val buildChildElement: EventToBuilder = buildElementFromEvent andThen {
      case newChildElement: Elem =>
        BuildingElement(newChildElement, Seq(element) ++ ancestors: _*)
    }

    val finaliseElement: EventToBuilder = {
      case e if e.isEndElement && ancestors.nonEmpty =>
        val Seq(first, rest @ _*) = ancestors
        val newElement = first.copy(child = first.child :+ element)
        BuildingElement(newElement, rest :_*)
      case e if e.isEndElement =>
        FinalElement(elem = element)
    }

    val process: EventToBuilder =
      addChildren orElse buildChildElement orElse finaliseElement

  }
}
