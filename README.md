XML Streaming for Scala (xs4s) [![Maven Central](https://img.shields.io/maven-central/v/com.scalawilliam/xs4s_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.scalawilliam/xs4s_2.11) [![Build Status](https://travis-ci.org/ScalaWilliam/xs4s.svg?branch=master)](https://travis-ci.org/ScalaWilliam/xs4s) [![Join the chat at https://gitter.im/ScalaWilliam/xs4s](https://badges.gitter.im/ScalaWilliam/xs4s.svg)](https://gitter.im/ScalaWilliam/xs4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
====

Ths library makes it easy for you to consume multi-gigabyte XML files in a streaming fashion - ie read and process a 4GB Wikipedia XML file,
straight from the web, without running out of memory.

It consumes events from the standard XML API (https://github.com/FasterXML/woodstox),
gradually forms a partial tree, and based on a user-supplied function ("query"), it will 
materialise that partial tree into a full tree, which will return to the user.

Using the library
======

Add the following to your build.sbt:

```sbt
scalaVersion := "2.12.3"

libraryDependencies += "com.scalawilliam" %% "xs4s" % "0.4"

// optionally, if you want to use a snapshot build.
// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
```

Examples
======

* ComputeBritainsRegionalMinimumParkingCosts
* FindMostPopularWikipediaKeywords
* Question12OfXTSpeedoXmarkTests

Running the examples
======


```bash
$ git clone https://github.com/ScalaWilliam/xs4s.git
$ sbt examples/downloadCarparks "examples/runMain xs4s.examples.ComputeBritainsRegionalMinimumParkingCosts" 
$ sbt examples/downloadXmark "examples/runMain xs4s.examples.Question12OfXTSpeedoXmarkTests"
$ sbt "examples/runMain xs4s.examples.FindMostPopularWikipediaKeywords" 
```

Publishing
======
``` bash
$ cat <<EOF > ~/.sbt/1.0/sonatype.sbt
credentials +=
  Credentials("Sonatype Nexus Repository Manager",
              "oss.sonatype.org",
              "USERNAME",
              "PASSWORD")
EOF

$ sbt +core/publishSigned
```

Then in https://oss.sonatype.org/ log in, go to 'Staging Repositories', sort by date descending, select the latest package, click 'Close' and then 'Release'.

ScalaWilliam <https://www.scalawilliam.com/>
