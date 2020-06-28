package net.sh4869.semver_parser

import scala.util.Try
import scala.util.parsing.combinator._

import CommonParser._

case class SemVer(
    major: Long,
    minor: Long,
    patch: Long,
    preRelease: Option[String],
    build: Option[String]
) extends Ordered[SemVer] {

  def original =
    s"$major.$minor.$patch${preRelease.map(x => s"-$x").getOrElse("")}${build.map(x => s"+$x").getOrElse("")}"

  /**
    * see https://semver.org/#spec-item-11
    *
    * @param that
    * @return
    */
  def compare(that: SemVer): Int = {
    if (that.major != major) major.compare(that.major)
    else if (that.minor != minor) minor.compare(that.minor)
    else if (that.patch != patch) patch.compare(that.patch)
    else {
      comparePreRelease(preRelease, that.preRelease)
    }
  }
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

  def apply(original: String): SemVer =
    try { parse(original).get }
    catch { case _: Throwable => throw new Error(s"semver parse error: ${original}") }
}
