package com.github.sh4869.semver_parser

import scala.util.Try
import scala.util.parsing.combinator._
import scala.language.implicitConversions
import CommonParser._

/**
  * Semantic Version
  *
  * @param major major number
  * @param minor minor number
  * @param patch patch number
  * @param preRelease preRelease(optional)
  * @param build build(optional)
  */
case class SemVer(
    major: Long,
    minor: Long,
    patch: Long,
    preRelease: Option[String],
    build: Option[String]
) {
  def original =
    s"$major.$minor.$patch${preRelease.map(x => s"-$x").getOrElse("")}${build.map(x => s"+$x").getOrElse("")}"
}

private object SemVerParser extends CommonParser {

  def semver: Parser[SemVer] =
    num_identifier ~ ("." ~> num_identifier) ~ ("." ~> num_identifier) ~ ("-" ~> prerelease_identifier).? ~ ("+" ~> build_identifier).? ^^ {
      case major ~ minor ~ patch ~ prerelease ~ build =>
        SemVer(major, minor, patch, prerelease, build)
    }

  def semver_parse(str: String): Option[SemVer] = parseAll(semver, str) match {
    case Success(result, _) => Some(result)
    case _                  => None
  }
}

object SemVer {
  def parse(str: String): Option[SemVer] = SemVerParser.semver_parse(str)

  def apply(major: Long, minor: Long, patch: Long): SemVer = new SemVer(major, minor, patch, None, None)
  implicit object SemVerOrdering extends Ordering[SemVer] {
    override def compare(x: SemVer, y: SemVer): Int = {
      if (x.major != y.major) x.major.compare(y.major)
      else if (x.minor != y.minor) x.minor.compare(y.minor)
      else if (x.patch != y.patch) x.patch.compare(y.patch)
      else {
        comparePreRelease(x.preRelease, y.preRelease)
      }
    }
  }
  
  implicit def orderingToOrdered(v: SemVer) = Ordered.orderingToOrdered(v)(SemVerOrdering)

  def apply(original: String): SemVer =
    try { parse(original).get }
    catch { case _: Throwable => throw new Error(s"semver parse error: ${original}") }
}
