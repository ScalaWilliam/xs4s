package com.scalawilliam.xs4s.examples

import java.io.{File, FileInputStream}
import javax.xml.stream.XMLInputFactory

import com.scalawilliam.xs4s.XmlElementExtractor

object ComputeBritainsRegionalMinimumParkingCosts extends App {

  implicit val xmlInputfactory = XMLInputFactory.newInstance()

  // http://data.gov.uk/dataset/car-parks
  val splitter = XmlElementExtractor.collectElements { case l if l.last == "CarPark" => identity }

  val regionMinCosts = for {
    i <- (1 to 8).par
    file = new File(s"downloads/carparks-data/CarParkData_$i.xml")
    carPark <- {
      import XmlElementExtractor.IteratorCreator._
      splitter.processInputStream(new FileInputStream(file))
    }
    regionName <- carPark \\ "RegionName" map (_.text)
    minCost <- (carPark \\ "MinCostPence") map (_.text.toInt)
    if minCost > 0
  } yield regionName -> minCost

  val regionMinimumParkingCosts = regionMinCosts.toList
    .groupBy { case (region, cost) => region }
    .mapValues { regionCosts => regionCosts.map { case (region, cost) => cost } }
    .mapValues(costs => costs.sum / costs.size)

  val sortedParkingCosts = regionMinimumParkingCosts.toList.sortBy { case (region, cost) => -cost }

  sortedParkingCosts foreach println

}
