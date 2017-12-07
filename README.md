xs4s [![Maven Central](https://img.shields.io/maven-central/v/com.scalawilliam/xs4s_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/com.scalawilliam/xs4s_2.11) [![Build Status](https://travis-ci.org/ScalaWilliam/xs4s.svg?branch=master)](https://travis-ci.org/ScalaWilliam/xs4s) [![Join the chat at https://gitter.im/ScalaWilliam/xs4s](https://badges.gitter.im/ScalaWilliam/xs4s.svg)](https://gitter.im/ScalaWilliam/xs4s?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
====



XML Streaming for Scala

This library shows how to use Scala to process XML streams.

Scala's scalability makes it easy to do XML stream processing with StAX.

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

$ sbt examples/run
```

The code is lightweight, with ElementBuilder being the heaviest code as it converts
StAX events into Scala XML classes.

This can consume 100MB files or 4GB files without any problems. And it does it fast. It converts XML streams into Scala XML trees on demand, which you can then query from.

Publishing
======
``` bash
$ cat <<EOF > ~/.sbt/0.13/sonatype.sbt
credentials +=
  Credentials("Sonatype Nexus Repository Manager",
              "oss.sonatype.org",
              "USERNAME",
              "PASSWORD")
EOF

$ sbt +core/publishSigned
```

Then in https://oss.sonatype.org/ log in, go to 'Staging Repositories', sort by date descending, select the latest package, click 'Close'.

ScalaWilliam <https://www.scalawilliam.com/>
