package com.scalawilliam.examples.xml4s

import java.io.{ByteArrayInputStream, FileInputStream, File}
import javax.xml.stream.XMLInputFactory

import com.scalawilliam.xs4s.{XmlEventIterator, BasicElementExtractorBuilder}

import scala.xml.Elem

object CarParksMapReduce extends App {

  // http://data.gov.uk/dataset/car-parks
  implicit class NodeSeqExtensions(nodeSeq: scala.xml.NodeSeq) {
    def ===(hasValue: String): Boolean =
      nodeSeq.text == hasValue
    def !==(notValue: String): Boolean =
      nodeSeq.text != notValue
  }
  val splitter = BasicElementExtractorBuilder { case list if list.last == "CarPark"  => (e: Elem) => e }
  val inputFactory = XMLInputFactory.newInstance()
  import XmlEventIterator._
  val regionMinCosts = for {
    i <- (1 to 8).par
    file = new File(s"carparks/CarParkData_$i.xml")
    streamer = inputFactory.createXMLEventReader(new FileInputStream(file))
    splitter.Captured(_, carPark) <- streamer.scanLeft(splitter.initial)(_.process(_))
    regionName <- carPark \\ "RegionName" map (_.text)
    minCost <- (carPark \\ "MinCostPence") map (_.text.toInt)
    if minCost > 0
  } yield regionName -> minCost

  val regionMinimumParkingCosts = regionMinCosts.toList
    .groupBy{case (region, cost) => region}
    .mapValues{regionCosts => regionCosts.map{ case (region, cost) => cost }}
    .mapValues(costs => costs.sum / costs.size)

  println(regionMinimumParkingCosts.toList.sortBy{case (region, cost) => -cost} mkString "\n")
}
