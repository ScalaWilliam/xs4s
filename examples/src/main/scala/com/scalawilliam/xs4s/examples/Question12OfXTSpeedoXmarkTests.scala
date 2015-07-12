package com.scalawilliam.xs4s.examples

import javax.xml.stream.XMLInputFactory
import java.io.{InputStream, File, FileInputStream}
import com.scalawilliam.xs4s.{XmlEventIterator, XmlStreamElementProcessor}

object Question12OfXTSpeedoXmarkTests extends App {

  /** Doing the same thing as the following XSLT:
    * https://github.com/Saxonica/XT-Speedo/blob/master/data/xmark-tests/q12.xsl
    */
  val xmlInputFactory = XMLInputFactory.newInstance()
  // xmark1 XML file - 100MB or so - get it from XT Speedo chaps
   def fileAsInputStream = new FileInputStream(new File("downloads/xmark4.xml"))
   println(testInput(fileAsInputStream))

   def testInput(inputStream: InputStream): scala.xml.Elem = {

     val xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream)

     case class InitialOpen(value: Double)
     case class Person(name: String, income: Double)

     val splitter = XmlStreamElementProcessor(
       {
         case List("site", "open_auctions", "open_auction", "initial") =>
           initialElement =>
             Seq(InitialOpen(initialElement.text.toDouble))
       },
       {
         case List("site", "people", "person") =>
           personElement => for {
             name <- personElement \ "name" map (_.text)
             income = (personElement \ "profile" \ "@income").map(_.text.toDouble).headOption.getOrElse(0.0)
           } yield Person(name, income)
       }
     )
     import XmlEventIterator._
     val collectedData = xmlEventReader.scanLeft(splitter.initial)(_.process(_)).collect {
       case splitter.Captured(_, d) => d
     }.toList.flatten
     <out>{
       for {
         Person(name, income) <- collectedData
         noItems = collectedData.count {
           case InitialOpen(value) if income > 5000 * value => true;
           case _ => false
         }
       } yield <items name={name}>{noItems.toString}</items>}</out>
   }

}
