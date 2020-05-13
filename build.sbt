name := "xs4s-root"

scalaVersion in ThisBuild := "2.12.11"
crossScalaVersions in ThisBuild := Seq("2.12.11", "2.13.2")
organization in ThisBuild := "com.scalawilliam"
version in ThisBuild := "0.5"

lazy val root = (project in file("."))
  .aggregate(core, examples)
  .dependsOn(core, examples)

lazy val core = project.settings(
  libraryDependencies ++= Seq(
    "xmlunit" % "xmlunit" % "1.6" % "test",
    "org.codehaus.woodstox" % "woodstox-core-asl" % "4.4.1",
    "org.scalatest" %% "scalatest" % "3.1.2" % "test",
    "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
    "co.fs2" %% "fs2-core" % "2.2.1" % "provided",
    "co.fs2" %% "fs2-io" % "2.2.1" % "provided",
  ),
  name := "xs4s",
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := <url>https://github.com/ScalaWilliam/xs4s</url>
    <licenses>
      <license>
        <name>BSD-style</name>
        <url>http://www.opensource.org/licenses/bsd-license.php</url>
        <distribution>repo</distribution>
      </license>
      <license>
        <name>The Apache Software License, Version 2.0</name>
        <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:ScalaWilliam/xs4s.git</url>
      <connection>scm:git:git@github.com:ScalaWilliam/xs4s.git</connection>
    </scm>
    <developers>
      <developer>
        <id>ScalaWilliam</id>
        <name>William Narmontas</name>
        <url>https://www.scalawilliam.com</url>
      </developer>
    </developers>
)


def download(source: sbt.URL, target: sbt.File): Int = {
  import scala.sys.process._

  (source #> target).!
}

lazy val examples = project.dependsOn(core).settings(
  downloadCarparks := {
    import sbt._
    import IO._
    if (!file("downloads/carparks-data").exists()) {
      download(url("http://81.17.70.199/carparks/data.zip"), file("downloads/carparks-data.zip"))
      unzip(file("downloads/carparks-data.zip"), file("downloads/carparks-data"))
    }
  },
  downloadXmark := {
    if (!file("downloads/xmark4.xml").exists()) {
      download(url("https://github.com/Saxonica/XT-Speedo/blob/master/data/xmark-tests/xmark4.xml?raw=true"), file("downloads/xmark4.xml"))
    }
  },
  libraryDependencies += "co.fs2" %% "fs2-core" % "2.2.1",
  libraryDependencies += "co.fs2" %% "fs2-io" % "2.2.1"
)

lazy val downloadCarparks = taskKey[Unit]("Download carparks task")

lazy val downloadXmark = taskKey[Unit]("Download speedo")

Global / onChangedBuildSource := ReloadOnSourceChanges