package net.sh4869.semver_parser

import scala.util.Try
import scala.util.parsing.combinator._

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
      def parsePR(opt: Option[String]) =
        opt.map[Seq[_]](x =>
          if (x.contains("."))
            x.split("\\.").toIndexedSeq.map(x => Try { x.toLong }.getOrElse(x))
          else Seq(x)
        )
      (parsePR(preRelease), parsePR(that.preRelease)) match {
        case (None, None)    => 0
        case (Some(_), None) => -1
        case (None, Some(_)) => 1
        case (Some(thi), Some(tha)) => {
          var result = 0
          var i = 0
          while (i < Seq(thi.length, tha.length).min && result == 0) {
            (thi(i), tha(i)) match {
              case (li: Long, la: Long) => result = li.compare(la)
              case (si, sa)             => result = si.toString().compare(sa.toString())
            }
            i += 1
          }
          if (result != 0) result
          else thi.length.compare(tha.length)
        }
      }
    }
  }
}

private object SemVerParser extends JavaTokenParsers {
  def num_identifier: Parser[Int] = """(0|[1-9]\d*)""".r ^^ { _.toInt }

  def build_identifier: Parser[String] = build_identifier_content ~ rep("." ~> build_identifier_content) ^^ {
    case a ~ as => s"$a${if (as.isEmpty) "" else "." + as.mkString(".")}"
  }
  def build_identifier_content: Parser[String] = """([a-zA-Z0-9\-]+)""".r ^^ { _.toString() }

  def prerelease_identifier: Parser[String] =
    prerelease_identifier_content ~ rep("." ~> prerelease_identifier_content) ^^ {
      case a ~ as => s"$a${if (as.isEmpty) "" else "." + as.mkString(".")}"
    }

  def prerelease_identifier_content: Parser[String] =
    alphanumeric_identifier | num_identifier ^^ { _.toString }

  def alphanumeric_identifier: Parser[String] =
    """([a-zA-Z\-][0-9a-zA-Z\-]*)""".r ^^ { _.toString() } |
      """([0-9a-zA-Z\-]*[a-zA-Z\-][0-9a-zA-Z\-]*)""".r ^^ { _.toString() }

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
