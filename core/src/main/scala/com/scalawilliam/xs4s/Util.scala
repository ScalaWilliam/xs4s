package com.scalawilliam.xs4s

object Util {
  object NodeSeqExtensions {
    implicit class NodeSeqExtensions(nodeSeq: scala.xml.NodeSeq) {
      def ===(hasValue: String): Boolean =
        nodeSeq.text == hasValue
      def !==(notValue: String): Boolean =
        nodeSeq.text != notValue
    }
  }
}
