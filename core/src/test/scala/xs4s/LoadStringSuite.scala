package xs4s

import org.scalatest.funsuite.AnyFunSuite

final class LoadStringSuite extends AnyFunSuite {
  test("It can read XML very simply") {
    assert(xs4s.XML.loadString("<test/>") == <test/>)
  }
}
