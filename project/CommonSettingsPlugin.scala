import sbt._
import Keys._

object CommonSettingsPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def projectSettings = Seq(
    scalaVersion := "2.11.7",
    organization := "com.scalawilliam",
    version := "0.2-SNAPSHOT"
  )
}
