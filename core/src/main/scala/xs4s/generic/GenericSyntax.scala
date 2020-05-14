package xs4s.generic

import fs2.Pipe

import scala.language.higherKinds

trait GenericSyntax {

  implicit class RichScanner[T, S, O](scanner: Scanner[T, S, O]) {
    def fs2Pipe[F[_]]: Pipe[F, T, O] =
      _.scan(scanner.initial)(scanner.scan).map(scanner.collect).unNone

    def iteratorPipe: Iterator[T] => Iterator[O] =
      _.scanLeft(scanner.initial)(scanner.scan).flatMap(scanner.collect)
  }

  implicit class RichIterator[I](iterator: Iterator[I]) {
    def through[S, O](scanner: Scanner[I, S, O]): Iterator[O] =
      scanner.iteratorPipe(iterator)
    def lastOption: Option[I] = iterator.toStream.lastOption
  }

}
