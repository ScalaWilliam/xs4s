name := "xs4s"
scalaVersion := "2.11.5"
version := "0.1-SNAPSHOT"
organization := "com.scalawilliam"
libraryDependencies ++= Seq(
  "xmlunit" % "xmlunit" % "1.5",
  "org.codehaus.woodstox" % "woodstox-core-asl" % "4.3.0",
  "org.compass-project" % "compass" % "2.2.0",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3-1",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test",
  "org.scala-lang.modules" %% "scala-xml" % "1.0.3"
)
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

publishMavenStyle := true

pomIncludeRepository := { _ => false }

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/ScalaWilliam/xs4s</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
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
  </developers>)