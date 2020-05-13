package xs4s.examples

import xs4s._
import xs4s.syntax._
import java.io.{File, FileInputStream, InputStream}

import javax.xml.stream.XMLInputFactory

object Question12OfXTSpeedoXmarkTests extends App {

  /** Doing the same thing as the following XSLT:
    * https://github.com/Saxonica/XT-Speedo/blob/master/data/xmark-tests/q12.xsl
    */
  val xmlInputFactory = XMLInputFactory.newInstance()
  val splitter: XmlElementExtractor[List[Either[InitialOpen, Person]]] =
    XmlElementExtractor.collectWithPartialFunctionOfElementNames {
      case List("site", "open_auctions", "open_auction", "initial") =>
        initialElement =>
          List(Left(InitialOpen(initialElement.text.toDouble)))
      case List("site", "people", "person") =>
        personElement =>
          for {
            name <- (personElement \ "name").map(_.text).toList
            income = (personElement \ "profile" \ "@income")
              .map(_.text.toDouble)
              .headOption
              .getOrElse(0.0)
          } yield Right(Person(name, income))
    }

  // xmark1 XML file - 100MB or so - get it from XT Speedo chaps
  def fileAsInputStream = new FileInputStream(new File("downloads/xmark4.xml"))

  def testInput(inputStream: InputStream): scala.xml.Elem = {

    val collectedData = {
      val xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream)

      try xmlEventReader.toIterator
        .scanCollect(splitter.Scan)
        .toList
        .flatten
      finally xmlEventReader.close()
    }

    <out>
      {for {
      Right(Person(name, income)) <- collectedData
      noItems = collectedData.count {
        case Left(InitialOpen(value)) if income > 5000 * value => true;
        case _ => false
      }
    } yield <items name={name}>
      {noItems.toString}
    </items>}
    </out>
  }

  final case class InitialOpen(value: Double)

  println(testInput(fileAsInputStream))

  final case class Person(name: String, income: Double)

}
