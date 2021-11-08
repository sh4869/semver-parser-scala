ThisBuild / organization := "com.github.sh4869"
ThisBuild / name := "semver-parser"
ThisBuild / version := "0.0.4"
ThisBuild / scalaVersion := "3.0.2"

ThisBuild / crossScalaVersions := Seq("2.11.12", "2.12.15", "2.13.7")

ThisBuild / libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0",
  "org.scalatest" %% "scalatest-funsuite" % "3.2.10" % "test",
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.10" % "test"
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

ThisBuild / publishMavenStyle := true
Test / publishArtifact := false
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

