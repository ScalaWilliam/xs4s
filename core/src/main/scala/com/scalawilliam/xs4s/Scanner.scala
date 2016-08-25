package com.scalawilliam.xs4s

/**
  * Created by me on 25/08/2016.
  */
trait Scanner[In, State, Out] {
  def initial: State

  def scan(state: State, element: In): State

  def collect: PartialFunction[State, Out]
}

object Scanner {

  trait Implicits {

    implicit class RichIterator[T](iterator: Iterator[T]) {
      def scan[S, O](scanner: Scanner[T, S, O]): Iterator[S] = {
        iterator.scanLeft(scanner.initial)(scanner.scan)
      }

      def scanCollect[S, O](scanner: Scanner[T, S, O]): Iterator[O] = {
        iterator.scanLeft(scanner.initial)(scanner.scan).collect(scanner.collect)
      }
    }

  }

  object Implicits extends Implicits {

  }

}
