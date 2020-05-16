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

libraryDependencies += "com.scalawilliam" %% "xs4s-core" % "0.6"
libraryDependencies += "com.scalawilliam" %% "xs4s-fs2" % "0.6"

// optionally, if you want to use a snapshot build.
// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

Then, you can implement functions such as the following ([BriefFS2Example](example/src/main/scala/xs4s/example/brief/BriefFS2Example.scala) - note the explicit types are for clarity):

```scala
/**
  *
  * @param byteStream Could be, for example, fs2.io.readInputStream(inputStream)
  * @param blocker obtained with Blocker[IO]
  */
def extractAnchorTexts(byteStream: Stream[IO, Byte], blocker: Blocker)(
    implicit cs: ContextShift[IO]): Stream[IO, String] = {

  /** extract all elements called 'anchor' **/
  val anchorElementExtractor: XmlElementExtractor[Elem] =
    XmlElementExtractor.filterElementsByName("anchor")

  /** Turn into XMLEvent */
  val xmlEventStream: Stream[IO, XMLEvent] =
    byteStream.through(byteStreamToXmlEventStream(blocker))

  /** Collect all the anchors as [[scala.xml.Elem]] */
  val anchorElements: Stream[IO, Elem] =
    xmlEventStream.through(anchorElementExtractor.toFs2PipeThrowError)

  /** And finally extract the text contents for each Elem */
  anchorElements.map(_.text)
}
```

Alternatively, we have a plain-Scala API, especially where you have legacy Java interaction, or you feel uncomfortable with pure FP for now: [BriefPlainScalaExample](example/src/main/scala/xs4s/example/brief/BriefPlainScalaExample.scala).:

```scala
def extractAnchorTexts(sourceFile: File): Unit = {
  val anchorElementExtractor: XmlElementExtractor[Elem] =
    XmlElementExtractor.filterElementsByName("anchor")
  val xmlEventReader = XMLStream.fromFile(sourceFile)
  try {
    val elements: Iterator[Elem] =
      xmlEventReader.extractWith(anchorElementExtractor)
    val text: Iterator[String] = elements.map(_.text)
    text.foreach(println)
  } finally xmlEventReader.close()
}
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
