XML Streaming for Scala (xs4s) [![Maven Central](https://img.shields.io/maven-central/v/com.scalawilliam/xs4s-core_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.scalawilliam/xs4s-core_2.13) [![Build Status](https://travis-ci.org/ScalaWilliam/xs4s.svg?branch=master)](https://travis-ci.org/ScalaWilliam/xs4s)
====

## Capabilities

xs4s offers the following capabilities:

- Scala-friendly utilities around the `javax.xml.stream.events` API.
- A mapping from the StAX to `scala.xml.Elem` and other Scala XML classes.
- An alternative method of parsing XML to `scala.xml.XML.load()`, so for example this will work:
```scala
assert(xs4s.XML.loadString("<test/>") == <test/>)
```
- An integration with FS2 and ZIO for pure FP streaming.
- Large file streaming, such as multi-gigabyte XML files, for example GZIPped files straight from Wikipedia, without running out of memory.


## How it does it
It uses the standard XML API (https://github.com/FasterXML/woodstox) as a back-end. It gradually forms a partial tree, and based on a user-supplied function ("query"), it will materialise that partial tree into a full tree, which will return to the user.

## Getting started

Add the following to your build.sbt (compatible with Scala 2.12 and 2.13 series):

```sbt
libraryDependencies += "com.scalawilliam" %% "xs4s-core" % "0.8"
libraryDependencies += "com.scalawilliam" %% "xs4s-fs2" % "0.8"
libraryDependencies += "com.scalawilliam" %% "xs4s-zio" % "0.8"
```

## Examples

### FS2 Streaming

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

### ZIO Streaming

Then, you can implement functions such as the following ([BriefZIOExample](example/src/main/scala/xs4s/example/brief/BriefZIOExample.scala) - note the explicit types are for clarity):

```scala
/**
  *
  * @param byteStream Could be, for example, zio.stream.Stream.fromInputStream(inputStream)
  * @return
  */
def extractAnchorTexts[R <: Blocking](byteStream: ZStream[R, IOException, Byte]): ZStream[R, Throwable, String] = {
  /** extract all elements called 'anchor' **/
  val anchorElementExtractor: XmlElementExtractor[Elem] =
    XmlElementExtractor.filterElementsByName("anchor")

  /** Turn into XMLEvent */
  val xmlEventStream: ZStream[R, Throwable, XMLEvent] =
    byteStream.via(byteStreamToXmlEventStream()(_))

  /** Collect all the anchors as [[scala.xml.Elem]] */
  val anchorElements: ZStream[R, Throwable, Elem] =
    xmlEventStream.via(anchorElementExtractor.toZIOPipeThrowError)

  /** And finally extract the text contents for each Elem */
  anchorElements.map(_.text)
}
```

### Plain `Iterator` streaming

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

### Advanced Wikipedia example

This example counts the popularity of Wikipedia anchors from their `abstract` documentation.

Many things all at once:
- Reading a streaming URL
- Passing through GZip decoder
- Then parsing XML
- Then doing map-reduce data from Wikipedia

The main example is in [FindMostPopularWikipediaKeywordsFs2App](example/src/main/scala/xs4s/example/FindMostPopularWikipediaKeywordsFs2App.scala) or [FindMostPopularWikipediaKeywordsZIOApp](example/src/main/scala/xs4s/example/FindMostPopularWikipediaKeywordsZIOApp.scala).
There is also a plain Scala example (using `Iterator`) in [FindMostPopularWikipediaKeywordsPlainScalaApp](example/src/main/scala/xs4s/example/FindMostPopularWikipediaKeywordsPlainScalaApp.scala).

```bash
$ git clone https://github.com/ScalaWilliam/xs4s.git
$ sbt "examples/runMain xs4s.example.FindMostPopularWikipediaKeywordsFs2App" 
$ sbt "examples/runMain xs4s.example.FindMostPopularWikipediaKeywordsZIOApp" 
$ sbt "examples/runMain xs4s.example.FindMostPopularWikipediaKeywordsPlainScalaApp" 
```

This can consume 100MB files or 4GB files without any problems. And it does it fast. It converts XML streams into Scala XML trees on demand, which you can then query from.

## Authors & Contributors
- @ScalaWilliam <https://www.scalawilliam.com/>
- @stettix <http://www.janvsmachine.net/>
- @er1c <https://twitter.com/ericpeters>
