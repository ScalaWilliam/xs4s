package com.scalawilliam.examples

import scala.xml.Node

/**
 * Add .sum and .product functionality to Scala XML nodes.
 */
object NumericNodesExample extends App {

  import NumericUtility._

  implicit val nodeDoubleIsomorphism = new Isomorphism[Node, Double] {
    assert(to(from(2.5)) == 2.5)
    def to(n: Node): Double = n.text.toDouble
    def from(d: Double): Node = <double>{d}</double>
  }

  implicit val doubleNumericNode = new NumericIsomorphism[Node, Double]

  val nums = <nums>{List(0.5, 5, -1.2).map(n => <num>{n}</num>)}</nums>

  println((nums \ "num").sum.text)

  println((nums \ "num").product.text)

}
object NumericUtility {

  trait Isomorphism[T, V] {
    def to(from: T): V
    def from(to: V): T
  }

  class NumericIsomorphism[V, T](implicit iso: Isomorphism[V, T], num: Numeric[T]) extends Numeric[V] {
    private implicit val from = iso.from _
    private implicit val to = iso.to _
    def plus(x: V, y: V) = num.plus(x, y)
    def toDouble(x: V) = num.toDouble(x)
    def toFloat(x: V) = num.toFloat(x)
    def toInt(x: V) = num.toInt(x)
    def negate(x: V) = num.negate(x)
    def fromInt(x: Int) = num.fromInt(x)
    def toLong(x: V) = num.toLong(x)
    def times(x: V, y: V) = num.times(x, y)
    def minus(x: V, y: V) = num.minus(x, y)
    def compare(x: V, y: V) = num.compare(x, y)
  }
}