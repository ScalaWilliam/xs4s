xs4s
====

XML Streaming for Scala

This library shows how to use Scala to process XML streams.

Scala's scalability makes it easy to do XML stream processing with StAX.

Using the library
======

Add the following to your build.sbt:

```sbt
scalaVersion := "2.11.7"

libraryDependencies += "com.scalawilliam" %% "xs4s" % "0.1-SNAPSHOT"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

Examples
======
```
examples/src/main/scala/com/scalawilliam/xs4s/examples/ComputeBritainsRegionalMinimumParkingCosts.scala
examples/src/main/scala/com/scalawilliam/xs4s/examples/FindMostPopularWikipediaKeywords.scala
examples/src/main/scala/com/scalawilliam/xs4s/examples/Question12OfXTSpeedoXmarkTests.scala
```

Running the examples
======


```bash
$ git clone https://github.com/ScalaWilliam/xs4s.git

$ sbt examples/run
```

The code is lightweight, with ElementBuilder being the heaviest code as it converts
StAX events into Scala XML classes.

This can consume 100MB files or 4GB files without any problems. And it does it fast. It converts XML streams into Scala XML trees on demand, which you can then query from.


ScalaWilliam <https://www.scalawilliam.com/>
