package com.scalawilliam.xs4s

trait NodeSeqExtensions {

  implicit class NodeSeqExtensions(nodeSeq: scala.xml.NodeSeq) {
    def ===(hasValue: String): Boolean =
      nodeSeq.text == hasValue

    def !==(notValue: String): Boolean =
      nodeSeq.text != notValue
  }

}

object NodeSeqExtensions extends NodeSeqExtensions