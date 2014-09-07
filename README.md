xs4s
====

XML Streaming for Scala

This library shows how to use Scala to process XML streams.

Scala's scalability makes it easy to do XML stream processing with StAX.


Get started:
```
$ sbt "runMain com.scalawilliam.xs4s.examples.Question12OfXTSpeedoXmarkTests"

$ sbt "runMain com.scalawilliam.xs4s.examples.ComputeBritainsRegionalMinimumParkingCosts"

$ sbt "runMain com.scalawilliam.xs4s.examples.FindMostPopularWikipediaKeywords"

# Fetches 4GB of Wikipedia data over the wire as a stream
$ sbt "runMain com.scalawilliam.xs4s.examples.FindMostPopularWikipediaKeywords full"
```

The code is lightweight, with ElementBuilder being the heaviest code as it converts
StAX events into Scala XML classes.

Example XML processing:

```scala
package com.scalawilliam.xs4s.examples

import java.io.{File, FileInputStream}
import com.scalawilliam.xs4s.XmlStreamElementCollector
import scala.xml.Elem

object `Compute Britains regional minimum parking costs` extends App {

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

```

This can consume 100MB files or 4GB files without any problems. And it does it fast. It converts XML streams into Scala XML trees on demand, which you can then query from.

This project has the following source files:

```
src/main/scala/com/scalawilliam/xs4s/examples/FindMostPopularWikipediaKeywords.scala
src/main/scala/com/scalawilliam/xs4s/examples/ComputeBritainsRegionalMinimumParkingCosts.scala
src/main/scala/com/scalawilliam/xs4s/examples/Question12OfXTSpeedoXmarkTests.scala
src/main/scala/com/scalawilliam/xs4s/XmlStreamElementCollector.scala
src/main/scala/com/scalawilliam/xs4s/Util.scala
src/main/scala/com/scalawilliam/xs4s/ElementBuilder.scala
src/main/scala/com/scalawilliam/xs4s/XmlEventIterator.scala
src/test/scala/com/scalawilliam/xs4s/BasicElementExtractorBuilderSpec.scala
src/test/scala/com/scalawilliam/xs4s/ElementBuilderSpec.scala
```


ScalaWilliam <https://scalawilliam.com/>