package xs4s.generic

/**
  * A Scanner of functions that allow us to .scan/.scanLeft and then extract a
  * final result when the state emits it. This is useful for finite-state machines
  * who produce some sort of optional output result after every iteration.
  */
trait Scanner[In, State, Out] {
  def initial: State

  def scan(state: State, element: In): State

  def collect(state: State): Option[Out]
}

object Scanner {
  def of[In, State, Out](initialState: State)(process: (State, In) => State)(
      collectFunction: State => Option[Out]): Scanner[In, State, Out] =
    new Scanner[In, State, Out] {
      override def initial: State = initialState

      override def scan(state: State, element: In): State =
        process(state, element)

      override def collect(state: State): Option[Out] = collectFunction(state)
    }

  implicit class RichScanner[I, S, O](scanner: Scanner[I, S, O]) {
    def map[V](f: O => V): Scanner[I, S, V] = new Scanner[I, S, V] {
      override def initial: S = scanner.initial

      override def scan(state: S, element: I): S = scanner.scan(state, element)

      override def collect(state: S): Option[V] = scanner.collect(state).map(f)
    }
  }

  trait ScannerSyntaxes {

    implicit class RichScanner[T, S, O](scanner: Scanner[T, S, O]) {
      def iteratorPipe: Iterator[T] => Iterator[O] =
        _.scanLeft(scanner.initial)(scanner.scan).flatMap(scanner.collect)
    }

    implicit class RichIteratorScanner[I](iterator: Iterator[I]) {
      def through[S, O](scanner: Scanner[I, S, O]): Iterator[O] =
        scanner.iteratorPipe(iterator)

      def lastOption: Option[I] = iterator.toStream.lastOption
    }

  }

}
