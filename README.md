XML Streaming for Scala (xs4s) [![Maven Central](https://img.shields.io/maven-central/v/com.scalawilliam/xs4s-core_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/com.scalawilliam/xs4s-core_2.13)
====

# Capabilities

- Scala-friendly utilities around the `javax.xml.stream.events` API.
- A mapping from the to `scala.xml.Elem` and related XML classes.
- An alternative method of parsing XML to `scala.xml.XML.load()`, for example
 ```scala
assert(xs4s.XML.loadString("<test/>") == <test/>)
```
- An integration with FS2 and ZIO for pure-FP streaming.
- Large file streaming, such as multi-gigabyte XML files, for example GZIPped files straight from Wikipedia, without running out of memory.

# Documentation

Find the full documentation at --> **https://www.scalawilliam.com/xml-streaming-for-scala/**

## Authors & Contributors
- @ScalaWilliam <https://www.scalawilliam.com/>
- @stettix <http://www.janvsmachine.net/>
- @er1c <https://twitter.com/ericpeters>
- @LolHens <https://lolhens.de/>
