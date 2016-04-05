package com.scalawilliam.xs4s

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.{StartElement, XMLEvent}

import scala.xml.Elem

/**
 * Build 'Elem' out of 'XMLEvent'. See tests for examples.
 */
package object elementbuilder {

  import javax.xml.stream.events.{Attribute => JavaAttribute, Comment => JavaComment, Namespace => JavaNamespace, ProcessingInstruction => JavaProcessingInstruction}

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

  private[elementbuilder] def startElementToPartialElement(startElement: StartElement): Elem = {
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

  private[elementbuilder] val xmlEventToPartialElement: PartialFunction[XMLEvent, Elem] = {
    val getStart: PartialFunction[XMLEvent, StartElement] = {
      case s: StartElement => s
    }
    getStart andThen startElementToPartialElement
  }

  private[elementbuilder] val xmlEventToNonElement: PartialFunction[XMLEvent, Node] = {
    case ce if ce.isCharacters && ce.asCharacters().isCData => PCData(ce.asCharacters().getData)
    case ce if ce.isCharacters => Text(ce.asCharacters().getData)
    case pis: JavaProcessingInstruction =>
      ProcInstr(pis.getTarget, pis.getData)
    case cm :JavaComment => Comment(cm.getText)
  }

}
