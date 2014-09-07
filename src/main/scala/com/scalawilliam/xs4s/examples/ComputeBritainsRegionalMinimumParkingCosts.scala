package com.scalawilliam.xs4s.examples

import java.io.{File, FileInputStream}
import com.scalawilliam.xs4s.XmlStreamElementCollector
import scala.xml.Elem

object ComputeBritainsRegionalMinimumParkingCosts extends App {

  // http://data.gov.uk/dataset/car-parks
  val splitter = XmlStreamElementCollector {
    case list if list.last == "CarPark"  => (e: Elem) => e
  }

  val regionMinCosts = for {
    i <- (1 to 8).par
    file = new File(s"downloads/carparks-data/CarParkData_$i.xml")
    carPark <- {
      import com.scalawilliam.xs4s.XmlStreamElementCollector.IteratorCreator._
      splitter.processInputStream(new FileInputStream(file))
    }
    regionName <- carPark \\ "RegionName" map (_.text)
    minCost <- (carPark \\ "MinCostPence") map (_.text.toInt)
    if minCost > 0
  } yield regionName -> minCost

  val regionMinimumParkingCosts = regionMinCosts.toList
    .groupBy{case (region, cost) => region}
    .mapValues{regionCosts => regionCosts.map{ case (region, cost) => cost }}
    .mapValues(costs => costs.sum / costs.size)

  val sortedParkingCosts = regionMinimumParkingCosts.toList.sortBy{case (region, cost) => -cost}

  sortedParkingCosts foreach println

}
