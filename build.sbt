name := "xs4s"

scalaVersion := "2.11.2"

version := "0.1-SNAPSHOT"

organization := "com.scalawilliam"

libraryDependencies += "xmlunit" % "xmlunit" % "1.5"

libraryDependencies += "org.codehaus.woodstox" % "woodstox-core-asl" % "4.3.0"

libraryDependencies += "org.compass-project" % "compass" % "2.2.0"

libraryDependencies += "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.2"

lazy val downloadCarparks = taskKey[Unit]("Download carparks task")

downloadCarparks := {
  import sbt._
  import IO._
  if ( !file("downloads/carparks-data").exists() ) {
    download(url("http://81.17.70.199/carparks/data.zip"), file("downloads/carparks-data.zip"))
    unzip(file("downloads/carparks-data.zip"), file("downloads/carparks-data"))
  }
}

lazy val downloadXmark = taskKey[Unit]("Download speedo")

downloadXmark := {
  import sbt.IO._
  if ( !file("downloads/xmark4.xml").exists() ) {
    download(url("https://github.com/Saxonica/XT-Speedo/blob/master/data/xmark-tests/xmark4.xml?raw=true"), file("downloads/xmark4.xml"))
  }
}

run <<= (run in Runtime) dependsOn (downloadCarparks, downloadXmark)

runMain <<= (runMain in Runtime) dependsOn (downloadCarparks, downloadXmark)

