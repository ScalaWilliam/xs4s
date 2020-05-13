XML Streaming for Scala (xs4s) [![Maven Central](https://img.shields.io/maven-central/v/com.scalawilliam/xs4s_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.scalawilliam/xs4s_2.11) [![Build Status](https://travis-ci.org/ScalaWilliam/xs4s.svg?branch=master)](https://travis-ci.org/ScalaWilliam/xs4s) [![Join the chat at https://gitter.im/ScalaWilliam/xs4s](https://badges.gitter.im/ScalaWilliam/xs4s.svg)](https://gitter.im/ScalaWilliam/xs4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
====

Ths library makes it easy for you to consume multi-gigabyte XML files in a streaming fashion - ie read and process a 4GB Wikipedia XML file,
straight from the web, without running out of memory.

It consumes events from the standard XML API (https://github.com/FasterXML/woodstox),
gradually forms a partial tree, and based on a user-supplied function ("query"), it will 
materialise that partial tree into a full tree, which will return to the user.

FS2 compatibility is included (Functional Streams for Scala).

Using the library
======

Add the following to your build.sbt:

```sbt
scalaVersion := "2.12.3"

libraryDependencies += "com.scalawilliam" %% "xs4s" % "0.4"

// optionally, if you want to use a snapshot build.
// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

Then, 

```scala
import xs4s._
import xs4s.syntax._
// extract all elements called 'anchor'
val anchorElementExtractor: XmlElementExtractor[scala.xml.Elem] = XmlElementExtractor.filterElementsByName("anchor")
val byteStream: fs2.Stream[cats.effect.IO, Byte] = ??? // could be for example, fs2.io.readInputStream(inputStream)
val blocker: cats.effect.Blocker = ???
val xmlEventStream: fs2.Stream[cats.effect.IO, javax.xml.stream.events.XMLEvent] = byteStream.through(byteStreamToXmlEventStream(blocker))
// collect your anchor Elements and do what you need
val anchorElements: fs2.Stream[cats.effect.IO, scala.xml.Elem] = xmlEventStream.through(anchorElementExtractor.fs2Pipe)
val anchorTexts: fs2.Stream[cats.effect.IO, String] = anchorElements.map(_.text)
```

Alternatively, for plain Scala, especially where you have legacy Java interaction:

```scala
import xs4s._
import xs4s.syntax._
// extract all elements called 'anchor'
val anchorElementExtractor: XmlElementExtractor[scala.xml.Elem] = XmlElementExtractor.filterElementsByName("anchor")
val xmlEventReader: javax.xml.stream.XMLEventReader = ???
val elements: Iterator[scala.xml.Elem] = xmlEventReader.extractXml(anchorElementExtractor) 
val text: Iterator[String] = elements.map(_.text) 
``` 

Example
======

The main example is in [FindMostPopularWikipediaKeywordsFs2App](example/src/main/scala/xs4s/example/FindMostPopularWikipediaKeywordsFs2App.scala).
There is also a plain Scala example (using `Iterator`) in [FindMostPopularWikipediaKeywordsPlainScalaApp](example/src/main/scala/xs4s/example/FindMostPopularWikipediaKeywordsPlainScalaApp.scala).

```bash
$ git clone https://github.com/ScalaWilliam/xs4s.git
$ sbt "examples/runMain xs4s.example.FindMostPopularWikipediaKeywordsFs2App" 
$ sbt "examples/runMain xs4s.example.FindMostPopularWikipediaKeywordsPlainScalaApp" 
```

ScalaWilliam <https://www.scalawilliam.com/>
