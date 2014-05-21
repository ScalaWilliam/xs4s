package com.scalawilliam.examples.xml4s

import java.io.{FileInputStream, File}
import com.scalawilliam.xml4s.ScalaXmlStreamSplitter

object CarParksMapReduce extends App {

  // http://data.gov.uk/dataset/car-parks
  implicit class NodeSeqExtensions(nodeSeq: scala.xml.NodeSeq) {
    def ===(hasValue: String): Boolean =
      nodeSeq.text == hasValue
    def !==(notValue: String): Boolean =
      nodeSeq.text != notValue
  }

  // Goal: find the % of car parks per region that have CCTV
  val splitter = ScalaXmlStreamSplitter(matchAtTag = _.getName.getLocalPart == "CarPark")

  val regionMinCosts = for {
    i <- (1 to 8).par
    file = new File(s"carparks/CarParkData_$i.xml")
    carPark <- splitter(new FileInputStream(file))
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
