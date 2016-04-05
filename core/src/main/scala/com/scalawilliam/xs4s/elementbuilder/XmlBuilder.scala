package com.scalawilliam.xs4s.elementbuilder

import javax.xml.stream.events.{StartElement, XMLEvent}

import scala.xml.Elem

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
sealed trait XmlBuilder {
  type EventToBuilder = PartialFunction[XMLEvent, XmlBuilder]

  def process: EventToBuilder
}

case class NonElement(mostRecent: XMLEvent, reverseList: XMLEvent*) extends XmlBuilder {

  def process: EventToBuilder = produceBuildingElement orElse appendNonElement

  private def produceBuildingElement: PartialFunction[XMLEvent, BuildingElement] =
    xmlEventToPartialElement andThen {
      e => BuildingElement(e)
    }

  private def appendNonElement: PartialFunction[XMLEvent, NonElement] = {
    case e => NonElement(e, Seq(mostRecent) ++ reverseList: _*)
  }

}

case object NoElement extends XmlBuilder {
  val process: EventToBuilder = {
    case s: StartElement => BuildingElement(startElementToPartialElement(s))
    case any => NonElement(any)
  }
}

case class FinalElement(elem: Elem) extends XmlBuilder {
  def process: EventToBuilder = NoElement.process
}


case class BuildingElement(element: Elem, ancestors: Elem*) extends XmlBuilder {

  def process: EventToBuilder =
    includeChildren orElse buildChildElement orElse finaliseElement

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