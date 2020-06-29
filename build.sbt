name := "semver-parser"
version := "0.0.1"
scalaVersion := "2.13.1"
organization := "com.github.sh4869"

crossScalaVersions := Seq("2.11.12", "2.12.11")

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
  "org.scalatest" %% "scalatest-funsuite" % "3.2.0" % "test",
  "org.scalatest" %% "scalatest-mustmatchers" % "3.2.0" % "test"
)

scalacOptions ++= Seq(
  "-deprecation"
)

publishMavenStyle := true
publishArtifact in Test := false
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/sh4869/semver-parser-scala"))

scmInfo := Some(
  ScmInfo(
    url("https://github.com/sh4869/semver-parser-scala"),
    "scm:https://github.com/sh4869/semver-parser-scala.git"
  )
)

developers := List(
  Developer(
    id    = "sh4869",
    name  = "Nobuhiro Kasai",
    email = "nobuk4869@gmail.com",
    url   = url("https://sh4869.net")
  )
)