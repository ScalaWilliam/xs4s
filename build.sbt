name := "xs4s-root"

scalaVersion in ThisBuild := "2.12.11"
crossScalaVersions in ThisBuild := Seq("2.12.11", "2.13.2")
organization in ThisBuild := "com.scalawilliam"
version in ThisBuild := "0.7"

lazy val root = (project in file("."))
  .aggregate(core)
  .aggregate(example)
  .aggregate(fs2)
  .settings(publishArtifact := false)

lazy val core = project.settings(
  libraryDependencies ++= Seq(
    "xmlunit"                % "xmlunit"           % "1.6" % "test",
    "org.codehaus.woodstox"  % "woodstox-core-asl" % "4.4.1",
    "org.scalatest"          %% "scalatest"        % "3.1.2" % "test",
    "org.scala-lang.modules" %% "scala-xml"        % "1.3.0"
  ),
  name := "xs4s-core"
)

pomIncludeRepository in ThisBuild := { _ =>
  false
}

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild := {
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
      "co.fs2" %% "fs2-core" % "2.3.0",
      "co.fs2" %% "fs2-io"   % "2.3.0"
    )
  )

lazy val example = project
  .dependsOn(fs2)
  .dependsOn(core)
  .settings(
    publishArtifact := false
  )

Global / onChangedBuildSource := ReloadOnSourceChanges

pomExtra in ThisBuild := <url>https://github.com/ScalaWilliam/xs4s</url>
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

enablePlugins(MicrositesPlugin)

micrositeName := "XML Streaming for Scala (xs4s)"
micrositeDescription := "Consume large documents without running out of RAM"
micrositeUrl := "https://scalawilliam.github.io"
micrositeBaseUrl := "/xs4s"
//micrositeDocumentationUrl := "/yoursite/docs"
micrositeDocumentationLabelDescription := "Documentation"
micrositeAuthor := "ScalaWilliam"
micrositeHomepage := "https://47deg.github.io/sbt-microsites/"
micrositeOrganizationHomepage := "https://www.scalawilliam.com"
micrositeTwitter := "@scalawilliam"
micrositeTwitterCreator := "@scalawilliam"
micrositeGithubOwner := "ScalaWilliam"
micrositeGithubRepo := "xs4s"
// micrositeAnalyticsToken := 'UA-XXXXX-Y'
micrositeGitterChannel := false
