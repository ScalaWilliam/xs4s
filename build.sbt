name := "xs4s-root"

ThisBuild / scalaVersion := "2.13.6"
ThisBuild / crossScalaVersions := Seq("2.12.12", "2.13.6", "3.0.1")
ThisBuild / organization := "com.scalawilliam"
ThisBuild / version := "0.9.1"
ThisBuild / resolvers += Resolver.JCenterRepository

lazy val root = (project in file("."))
  .aggregate(core)
  .aggregate(example)
  .aggregate(fs2)
  .aggregate(fs2v3)
  .aggregate(zio)
  .settings(publishArtifact := false)

lazy val core = project.settings(
  libraryDependencies ++= Seq(
    "xmlunit"                % "xmlunit"           % "1.6" % "test",
    "com.fasterxml.woodstox" % "woodstox-core"     % "6.5.1",
    "org.scalatest"          %% "scalatest"        % "3.2.9" % "test",
    "org.scala-lang.modules" %% "scala-xml"        % "2.0.1"
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
      "co.fs2" %% "fs2-io" % "2.5.9"
    )
  )

lazy val fs2v3 = project
  .dependsOn(core)
  .settings(
    name := "xs4s-fs2v3",
    libraryDependencies ++= Seq(
      "co.fs2"        %% "fs2-io"    % "3.0.6",
      "org.scalatest" %% "scalatest" % "3.2.9" % "test"
    )
  )

lazy val zio = project
  .dependsOn(core)
  .settings(
    name := "xs4s-zio",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"         % "1.0.9",
      "dev.zio" %% "zio-streams" % "1.0.9"
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
    id = "ScalaWilliam",
    name = "ScalaWilliam",
    email = "hello@scalawilliam.com",
    url = url("https://www.scalawilliam.com/")
  )
)
ThisBuild / description := "XML Streaming library for Scala"
ThisBuild / licenses := List(
  "Apache 2"  -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"),
  "BSD-style" -> new URL("http://www.opensource.org/licenses/bsd-license.php")
)
ThisBuild / homepage := Some(url("https://github.com/ScalaWilliam/xs4s"))
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
