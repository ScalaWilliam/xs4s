package com.scalawilliam.xs4s

/**
 * A set of functions that allow a streaming scanLeft,
 * where the collection extracts the final result
 */
trait Scanner[In, State, Out] {
  def initial: State

  def scan(state: State, element: In): State

  def collect(state: State): Option[Out]
}

object Scanner {

  trait Implicits {

    implicit class RichIterator[T](iterator: Iterator[T]) {
      def scan[S, O](scanner: Scanner[T, S, O]): Iterator[S] = {
        iterator.scanLeft(scanner.initial)(scanner.scan)
      }

      def scanCollect[S, O](scanner: Scanner[T, S, O]): Iterator[O] = {
        iterator.scanLeft(scanner.initial)(scanner.scan).collect(Function.unlift(scanner.collect))
      }
    }

  }

  object Implicits extends Implicits

}
