package com.scalawilliam.xs4s.examples

import java.io.FileReader
import javax.xml.stream.XMLInputFactory

import com.scalawilliam.xs4s.Implicits._
import com.scalawilliam.xs4s.XmlElementExtractor

object ComputeBritainsRegionalMinimumParkingCosts extends App {

  val xmlInputfactory = XMLInputFactory.newInstance()

  // http://data.gov.uk/dataset/car-parks
  val splitter = XmlElementExtractor.collectElementsByName("CarPark")

  val regionMinCosts = (1 to 8).flatMap { i =>
    val fileReader = new FileReader(s"downloads/carparks-data/CarParkData_$i.xml")
    val reader = xmlInputfactory.createXMLEventReader(fileReader)
    try {
      (for {
        carPark <- reader.toIterator.scanCollect(splitter.Scan)
        regionName <- carPark \\ "RegionName" map (_.text)
        minCost <- (carPark \\ "MinCostPence") map (_.text.toInt)
        if minCost > 0
      } yield regionName -> minCost).toList
    } finally reader.close()
  }

  val regionMinimumParkingCosts = regionMinCosts.toList
    .groupBy { case (region, cost) => region }
    .mapValues { regionCosts => regionCosts.map { case (region, cost) => cost } }
    .mapValues(costs => costs.sum / costs.size)

  val sortedParkingCosts = regionMinimumParkingCosts.toList.sortBy { case (region, cost) => -cost }

  sortedParkingCosts foreach println

}
