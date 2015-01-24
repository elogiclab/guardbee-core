import play.Project._

organization := "com.elogiclab.guardbee"

name := """guardbee-core"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.2"

crossScalaVersions := Seq("2.10.2")

libraryDependencies ++= Seq(
)

playScalaSettings

EclipseKeys.withBundledScalaContainers := true



