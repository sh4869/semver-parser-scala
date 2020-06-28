package net.sh4869.semver_parser

import scala.util.parsing.combinator._

import CommonParser._
trait Range {
  def valid(version: SemVer): Boolean
}

// https://github.com/npm/node-semver#range-grammar
private object RangeParser extends CommonParser {
  import Range._

  def xr: Parser[Xr] = """\*|x|X""".r ^^ (_ => AnyR) | num_identifier ^^ (x => NumR(x))
  def range: Parser[VersionRange] =
    xr ~ ("." ~> xr).? ~ ("." ~> xr).? ^^ {
      case major ~ minor ~ patch => VersionRange(Some(major), minor, patch, None)
    } | xr ~ ("." ~> xr) ~ ("." ~> xr) ~ ("-" ~> prerelease_identifier).? ~ ("+" ~> build_identifier).? ^^ {
      case major ~ minor ~ patch ~ preRelease ~ _ => VersionRange(Some(major), Some(minor), Some(patch), preRelease)
    }

  def conditon: Parser[Condition] =
    ">" ^^ (_ => >) | ">=" ^^ (_ => >=) | "<" ^^ (_ => <) | "<=" ^^ (_ => <=) | "=" ^^ (_ => \=)

  def conditonRange: Parser[ConditionExpressionRange] = conditon ~ whiteSpace.? ~ range ^^ {
    case con ~ _ ~ range => ConditionExpressionRange(range, con)
  }

  def hatRange: Parser[HatRange] = "^" ~> whiteSpace.? ~> range ^^ { range => HatRange(range) }

  def caretRange: Parser[TildeRange] = "~" ~> whiteSpace.? ~> range ^^ { range => TildeRange(range) }

  def emptyRange: Parser[Range] = """^$""".r ^^ { _ => SimpleRange(VersionRange(None, None, None, None)) }

  def baseRange: Parser[Range] = conditonRange | hatRange | caretRange | range ^^ { x => SimpleRange(x) } | emptyRange

  def andRange: Parser[Range] = (whiteSpace.? ~> baseRange) ~ rep(whiteSpace ~> baseRange) ^^ {
    case x ~ xs => AndRange(x +: xs)
  }

  def orRange: Parser[Range] = andRange ~ rep("||" ~> andRange) ^^ {
    case x ~ xs => OrRange(x +: xs)
  }

  def range_parse(str: String): Option[Range] = parseAll(orRange, str) match {
    case Success(r, _) => Some(r)
    case Error(msg, next) => println(msg); None
    case Failure(msg, next) => println(msg); None
    case _             => None
  }
}

object Range {

  sealed trait Xr
  object AnyR extends Xr
  case class NumR(src: Long) extends Xr

  case class VersionRange(major: Option[Xr], minor: Option[Xr], patch: Option[Xr], preRelease: Option[String])

  private def basic_valid(range: VersionRange, version: SemVer): Boolean = {
    lazy val nonExtra = version.build.isEmpty && version.preRelease.isEmpty
    (range.major, range.minor, range.patch, range.preRelease) match {
      // ex: "x","X","*"
      case (None, _, _, _) | (Some(AnyR), _, _, _) => nonExtra
      // ex: "1.x", "2"
      case (Some(NumR(major)), Some(AnyR), _, _) => nonExtra && version.major == major
      case (Some(NumR(major)), None, _, _)       => nonExtra && version.major == major
      // ex: "1.1.x", "1.1"
      case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), _) =>
        major == version.major && minor == version.minor && nonExtra
      case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
        major == version.major && minor == version.minor && nonExtra
      // ex: "1.1.1"
      case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
        major == version.major && minor == version.minor && patch == version.patch && nonExtra
      case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(pre)) =>
        major == version.major && minor == version.minor && patch == version.patch && version.preRelease.exists(x =>
          x == pre
        ) && version.build.isEmpty
    }
  }

  case class SimpleRange(range: VersionRange) extends Range {
    def valid(version: SemVer) = basic_valid(range, version)
  }

  case class HatRange(range: VersionRange) extends Range {
    def valid(version: SemVer) = {
      lazy val nonExtra = version.build.isEmpty && version.preRelease.isEmpty
      (range.major, range.minor, range.patch, range.preRelease) match {
        // ex: "^1.1.0", "^1.1.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
          version.major == major && version.minor >= minor && version.patch >= patch && nonExtra
        // ex: "^1.0.0-rc.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(_)) =>
          version.major == major && version.minor >= minor && version.patch >= patch &&
            (version.preRelease.isEmpty || comparePreRelease(version.preRelease, range.preRelease) >= 0)
        // ex: "^1.0.x", "^1.3.x"
        case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), None) =>
          version.major == major && version.minor >= minor && nonExtra
        case _ => basic_valid(range, version)
      }
    }
  }

  case class TildeRange(range: VersionRange) extends Range {
    def valid(version: SemVer) = {
      lazy val nonExtra = version.build.isEmpty && version.preRelease.isEmpty
      (range.major, range.minor, range.patch, range.preRelease) match {
        // ex: "~1.1.0", "~1.1.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
          version.major == major && version.minor == minor && version.patch >= patch && nonExtra
        // ex: "~1.0.0-rc.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(_)) =>
          version.major == major && version.minor == minor && version.patch >= patch &&
            (version.preRelease.isEmpty || comparePreRelease(version.preRelease, range.preRelease) >= 0)
        case _ => basic_valid(range, version)
      }
    }
  }

  private[semver_parser] sealed trait Condition
  private[semver_parser] object > extends Condition
  private[semver_parser] object < extends Condition
  private[semver_parser] object >= extends Condition
  private[semver_parser] object <= extends Condition
  private[semver_parser] object \= extends Condition

  case class ConditionExpressionRange(range: VersionRange, con: Condition) extends Range {
    def valid(version: SemVer): Boolean = {
      lazy val nonExtra = version.build.isEmpty && version.preRelease.isEmpty
      con match {
        case \= => basic_valid(range, version)
        case > => {
          (range.major, range.minor, range.patch, range.preRelease) match {
            // ex: "> *" (this returns always false)
            case (Some(AnyR), _, _, _) | (None, _, _, _) => false
            // ex: "> 1.x", "> 1"
            case (Some(NumR(major)), None, _, _)       => version.major > major && nonExtra
            case (Some(NumR(major)), Some(AnyR), _, _) => version.major > major && nonExtra
            // ex: "> 1.0.x", "> 1.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), _) =>
              (version.major > major || (version.major == major && version.minor > minor)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
              (version.major > major || (version.major == major && version.minor > minor)) && nonExtra
            // ex: "> 1.0.0", "> 1.0.0-rc.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
              (version.major > major || (version.major == major && version.minor > minor) || (version.major == major && version.minor == minor && version.patch > patch)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
              (version.major > major || (version.major == major && version.minor > minor) || (version.major == major && version.minor == minor && version.patch > patch)) &&
                comparePreRelease(version.preRelease, range.preRelease) > 0
          }
        }
        case >= => {
          (range.major, range.minor, range.patch, range.preRelease) match {
            // ex: ">= *" (this returns always false)
            case (Some(AnyR), _, _, _) | (None, _, _, _) => nonExtra
            // ex: ">= 1.x", ">= 1"
            case (Some(NumR(major)), None, _, _)       => version.major >= major && nonExtra
            case (Some(NumR(major)), Some(AnyR), _, _) => version.major >= major && nonExtra
            // ex: ">= 1.0.x", ">= 1.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), _) =>
              (version.major >= major || (version.major == major && version.minor >= minor)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
              (version.major >= major || (version.major == major && version.minor >= minor)) && nonExtra
            // ex: ">= 1.0.0", ">= 1.0.0-rc.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
              (version.major >= major || (version.major == major && version.minor >= minor) || (version.major == major && version.minor == minor && version.patch >= patch)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
              (version.major >= major || (version.major == major && version.minor >= minor) || (version.major == major && version.minor == minor && version.patch >= patch)) &&
                comparePreRelease(version.preRelease, range.preRelease) >= 0
          }
        }
        case < => {
          (range.major, range.minor, range.patch, range.preRelease) match {
            // ex: "< *" (this returns always false)
            case (Some(AnyR), _, _, _) | (None, _, _, _) => false
            // ex: "< 1.x", "< 1"
            case (Some(NumR(major)), None, _, _)       => version.major < major && nonExtra
            case (Some(NumR(major)), Some(AnyR), _, _) => version.major < major && nonExtra
            // ex: "< 1.0.x", "< 1.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), _) =>
              (version.major < major || (version.major == major && version.minor < minor)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
              (version.major < major || (version.major == major && version.minor < minor)) && nonExtra
            // ex: "< 1.0.0", "< 1.0.0-rc.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
              (version.major < major || (version.major == major && version.minor < minor) || (version.major == major && version.minor == minor && version.patch < patch)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
              (version.major < major || (version.major == major && version.minor < minor) || (version.major == major && version.minor == minor && version.patch < patch)) &&
                comparePreRelease(version.preRelease, range.preRelease) < 0
          }
        }
        case <= => {
          (range.major, range.minor, range.patch, range.preRelease) match {
            // ex: "<= *" (this returns always false)
            case (Some(AnyR), _, _, _) | (None, _, _, _) => nonExtra
            // ex: "<= 1.x", "<= 1"
            case (Some(NumR(major)), None, _, _)       => version.major <= major && nonExtra
            case (Some(NumR(major)), Some(AnyR), _, _) => version.major <= major && nonExtra
            // ex: "<= 1.0.x", "<= 1.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), _) =>
              (version.major <= major || (version.major == major && version.minor <= minor)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
              (version.major <= major || (version.major == major && version.minor <= minor)) && nonExtra
            // ex: "> 1.0.0", "> 1.0.0-rc.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
              (version.major <= major || (version.major == major && version.minor <= minor) || (version.major == major && version.minor == minor && version.patch <= patch)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
              (version.major <= major || (version.major == major && version.minor <= minor) || (version.major == major && version.minor == minor && version.patch <= patch)) &&
                comparePreRelease(version.preRelease, range.preRelease) <= 0
          }
        }
      }
    }
  }

  case class AndRange(ranges: List[Range]) extends Range {
    def valid(version: SemVer): Boolean = ranges.forall(_.valid(version))
  }

  case class OrRange(ranges: List[Range]) extends Range {
    def valid(version: SemVer): Boolean = ranges.exists(_.valid(version))
  }

  def parse(str: String) = RangeParser.range_parse(str)
}