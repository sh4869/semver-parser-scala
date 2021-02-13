ThisBuild / organization := "com.github.sh4869"
ThisBuild / name := "semver-parser"
ThisBuild / version := "0.0.4"
ThisBuild / scalaVersion := "2.13.4"

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.11", "2.13.4")

ThisBuild / libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.scalatest" %% "scalatest-funsuite" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.0" % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-feature"
)

ThisBuild / licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
ThisBuild / homepage := Some(url("https://github.com/sh4869/semver-parser-scala"))


ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/sh4869/semver-parser-scala"),
    "scm:https://github.com/sh4869/semver-parser-scala.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "sh4869",
    name  = "Nobuhiro Kasai",
    email = "nobuk4869@gmail.com",
    url   = url("https://sh4869.net")
  )
)

publishMavenStyle in ThisBuild := true
publishArtifact in Test := false
pomIncludeRepository in ThisBuild := { _ => false }
publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

