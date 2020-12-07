name := "xs4s-root"

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / crossScalaVersions := Seq("2.12.12", "2.13.4")
ThisBuild / organization := "com.scalawilliam"
ThisBuild / version := "0.8.5"

lazy val root = (project in file("."))
  .aggregate(core)
  .aggregate(example)
  .aggregate(fs2)
  .aggregate(zio)
  .settings(publishArtifact := false)

lazy val core = project.settings(
  libraryDependencies ++= Seq(
    "xmlunit"                % "xmlunit"           % "1.6" % "test",
    "org.codehaus.woodstox"  % "woodstox-core-asl" % "4.4.1",
    "org.scalatest"          %% "scalatest"        % "3.2.3" % "test",
    "org.scala-lang.modules" %% "scala-xml"        % "1.3.0"
  ),
  name := "xs4s-core"
)

ThisBuild / pomIncludeRepository := { _ =>
  false
}

ThisBuild / publishMavenStyle := true

ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

def download(source: sbt.URL, target: sbt.File): Int = {
  import scala.sys.process._

  (source #> target).!
}

lazy val fs2 = project
  .dependsOn(core)
  .settings(
    name := "xs4s-fs2",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "2.4.6",
      "co.fs2" %% "fs2-io"   % "2.4.6"
    )
  )

lazy val zio = project
  .dependsOn(core)
  .settings(
    name := "xs4s-zio",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "1.0.3",
      "dev.zio" %% "zio-streams" % "1.0.3"
    )
  )

lazy val example = project
  .dependsOn(fs2)
  .dependsOn(zio)
  .dependsOn(core)
  .settings(
    publishArtifact := false
  )

Global / onChangedBuildSource := ReloadOnSourceChanges
ThisBuild / organizationHomepage := Some(url("https://www.scalawilliam.com"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/ScalaWilliam/xs4s"),
    "scm:git:git@github.com:ScalaWilliam/xs4s.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "ScalaWilliam",
    name  = "ScalaWilliam",
    email = "hello@scalawilliam.com",
    url   = url("https://www.scalawilliam.com/")
  )
)
ThisBuild / description := "XML Streaming library for Scala"
ThisBuild / licenses := List(
  "Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"),
  "BSD-style" -> new URL("http://www.opensource.org/licenses/bsd-license.php")
)
ThisBuild / homepage := Some(url("https://github.com/ScalaWilliam/xs4s"))
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
