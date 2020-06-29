package net.sh4869.semver_parser

import scala.util.parsing.combinator._

import CommonParser._

/**
  * Version Range
  */
trait Range {

  /**
    * check that version is valid
    *
    * @param version target
    * @return version is valid
    */
  def valid(version: SemVer): Boolean

  /**
    * check that version is invalid
    *
    * @param version target
    * @return version is invalid
    */
  def invalid(version: SemVer): Boolean = !valid(version)
}

// https://github.com/npm/node-semver#range-grammar
private object RangeParser extends CommonParser {
  import Range._

  def xr: Parser[Xr] = """\*|x|X""".r ^^ (_ => AnyR) | num_identifier ^^ (x => NumR(x))

  def range: Parser[VersionRange] =
    xr ~ ("." ~> xr) ~ ("." ~> xr) ~ ("-" ~> prerelease_identifier).? ~ ("+" ~> build_identifier).? ^^ {
      case major ~ minor ~ patch ~ preRelease ~ _ => VersionRange(Some(major), Some(minor), Some(patch), preRelease)
    } | xr ~ ("." ~> xr).? ~ ("." ~> xr).? ^^ {
      case major ~ minor ~ patch => VersionRange(Some(major), minor, patch, None)
    }

  def conditon: Parser[Condition] =
    ">=" ^^ (_ => >=) | ">" ^^ (_ => >) | "<=" ^^ (_ => <=) | "<" ^^ (_ => <) | "=" ^^ (_ => \=)

  def conditonRange: Parser[ConditionExpressionRange] = conditon ~ whiteSpace.? ~ range ^^ {
    case con ~ _ ~ range => ConditionExpressionRange(range, con)
  }

  def hatRange: Parser[HatRange] = "^" ~> whiteSpace.? ~> range ^^ { range => HatRange(range) }

  def tildeRange: Parser[TildeRange] = "~" ~> whiteSpace.? ~> range ^^ { range => TildeRange(range) }

  def emptyRange: Parser[Range] = """^$""".r ^^ { _ => SimpleRange(VersionRange(None, None, None, None)) }

  def hyphenRange: Parser[Range] = range ~ (whiteSpace ~ "-" ~ whiteSpace) ~ range ^^ {
    case l ~ _ ~ r => HyphenRange(l, r)
  }

  def baseRange: Parser[Range] = conditonRange | hatRange | tildeRange | range ^^ { x => SimpleRange(x) } | emptyRange

  def andRange: Parser[Range] = (whiteSpace.? ~> baseRange) ~ rep(whiteSpace ~> baseRange) ^^ {
    case x ~ xs => AndRange(x +: xs)
  }

  def orBaseRange: Parser[Range] = hyphenRange | andRange

  def orRange: Parser[Range] = orBaseRange ~ rep(whiteSpace.? ~> "||" ~> whiteSpace.? ~> orBaseRange) ^^ {
    case x ~ xs => OrRange(x +: xs)
  }

  def range_parse(str: String): Option[Range] = parseAll(orRange, str) match {
    case Success(r, _) => Some(r)
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

  /**
    * Simple Range
    *
    * ex: "1.0.0","2.3.5", "1.0.x", "1.x", "x"
    *
    * @param range
    */
  case class SimpleRange(range: VersionRange) extends Range {
    def valid(version: SemVer) = basic_valid(range, version)
  }

  /**
    * Hat Range
    *
    * ex: "^1.0.0","^2.3.5", "^1.0.x", "^1.x", "^x"
    *
    * @param range
    */
  case class HatRange(range: VersionRange) extends Range {
    def valid(version: SemVer) = {
      lazy val nonExtra = version.build.isEmpty && version.preRelease.isEmpty
      (range.major, range.minor, range.patch, range.preRelease) match {
        // ex: "^0.0.1", "^0.0.3"
        case (Some(NumR(0)), Some(NumR(0)), Some(NumR(patch)), None) =>
          version.major == 0 && version.minor == 0 && version.patch == patch && nonExtra
        // ex: "^0.0.1-rc.1"
        case (Some(NumR(0)), Some(NumR(0)), Some(NumR(patch)), Some(pre)) =>
          version.major == 0 && version.minor == 0 && version.patch == patch &&
            (version.preRelease.isEmpty || comparePreRelease(version.preRelease, range.preRelease) >= 0)
        // ex: "^0.1.0", "^0.3.0"
        // "^0.1.0" == "~0.1.0"
        case (Some(NumR(0)), Some(NumR(minor)), Some(NumR(patch)), None) =>
          version.major == 0 && version.minor == minor && version.patch >= patch && nonExtra
        // ex: "^0.1.0-rc.1"
        case (Some(NumR(0)), Some(NumR(minor)), Some(NumR(patch)), Some(pre)) =>
          version.major == 0 && version.minor == minor && version.patch >= patch &&
            (version.preRelease.isEmpty || comparePreRelease(version.preRelease, range.preRelease) >= 0)
        // ex: "^1.1.0", "^1.1.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
          version.major == major && (version.minor > minor || version.minor >= minor && version.patch >= patch) && nonExtra
        // ex: "^1.0.0-rc.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
          (version.major == major && (version.minor > minor || version.minor >= minor && version.patch >= patch) && nonExtra) ||
            // only when [major minor patch] is same, compare preRelease
            (version.major == major && version.minor == minor && version.patch == patch && version.preRelease.exists(
              x => comparePreRelease(Some(x), Some(preRelease)) >= 0
            ))
        // ex: "^0.0.x", "^0.1.x"
        case (Some(NumR(0)), Some(NumR(minor)), Some(AnyR), None) =>
          version.major == 0 && version.minor == minor && nonExtra
        // ex: "^1.0.x", "^1.3.x"
        case (Some(NumR(major)), Some(NumR(minor)), Some(AnyR), None) =>
          version.major == major && version.minor >= minor && nonExtra
        case _ => basic_valid(range, version)
      }
    }
  }

  /**
    * Simple Range
    *
    * ex: "~1.0.0","~2.3.5", "~1.0.x", "~1.x", "~x"
    *
    * @param range
    */
  case class TildeRange(range: VersionRange) extends Range {
    def valid(version: SemVer) = {
      lazy val nonExtra = version.build.isEmpty && version.preRelease.isEmpty
      (range.major, range.minor, range.patch, range.preRelease) match {
        // ex: "~1.1.0", "~1.1.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
          version.major == major && version.minor == minor && version.patch >= patch && nonExtra
        // ex: "~1.0.0-rc.1"
        case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(pre)) =>
          (version.major == major && version.minor == minor && version.patch >= patch && nonExtra) ||
            (version.major == major && version.minor == minor && version.patch == patch && version.preRelease.exists(
              x => comparePreRelease(Some(x), Some(pre)) >= 0
            ))
        case _ => basic_valid(range, version)
      }
    }
  }

  /**
    * Hyphen Range
    *
    * ex: "1.0.0 - 2.0.0", "3.0.0 - 4.3.5", "1.x - 2.3.0"
    *
    * @param left
    * @param right
    */
  case class HyphenRange(left: VersionRange, right: VersionRange) extends Range {
    // see https://github.com/npm/node-semver#hyphen-ranges-xyz---abc
    lazy val alter = AndRange(List(ConditionExpressionRange(left, >=), ConditionExpressionRange(right, <=)))
    def valid(version: SemVer) = alter.valid(version)
  }

  private[semver_parser] sealed trait Condition
  private[semver_parser] object > extends Condition
  private[semver_parser] object < extends Condition
  private[semver_parser] object >= extends Condition
  private[semver_parser] object <= extends Condition
  private[semver_parser] object \= extends Condition

  /**
    * Condition Range
    *
    * ex: "> 1.0.0", ">= 2.3.0", "< 1.x", "> 2.x"
    *
    * @param range
    * @param con
    */
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
              (version.major == major && version.minor == minor && version.patch == patch) &&
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
              (version.major > major || (version.major == major && version.minor >= minor)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
              (version.major > major || (version.major == major && version.minor >= minor)) && nonExtra
            // ex: ">= 1.0.0", ">= 1.0.0-rc.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
              (version.major > major || (version.major == major && version.minor > minor) || (version.major == major && version.minor == minor && version.patch >= patch)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
              (version.major == major && version.minor == minor && version.patch == patch) &&
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
              (version.major == major && version.minor == minor && version.patch == patch) &&
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
              (version.major < major || (version.major == major && version.minor <= minor)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), None, _) =>
              (version.major < major || (version.major == major && version.minor <= minor)) && nonExtra
            // ex: "> 1.0.0", "> 1.0.0-rc.0"
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), None) =>
              (version.major < major || (version.major == major && version.minor < minor) || (version.major == major && version.minor == minor && version.patch <= patch)) && nonExtra
            case (Some(NumR(major)), Some(NumR(minor)), Some(NumR(patch)), Some(preRelease)) =>
              (version.major == major && version.minor == minor && version.patch == patch) &&
                comparePreRelease(version.preRelease, range.preRelease) <= 0
          }
        }
      }
    }
  }

  /**
    * And Range
    *
    * ex: "> 1.0.0 < 2.0.0", "^2.0.0 > 3.0.0"
    *
    * @param ranges
    */
  case class AndRange(ranges: List[Range]) extends Range {
    def valid(version: SemVer): Boolean = ranges.forall(_.valid(version))
  }

  /**
    * Or Range
    *
    * ex: "1.0.0 || 2.0.0", "2.3.0 - 4.0.0 || 2.0.0"
    *
    * @param ranges
    */
  case class OrRange(ranges: List[Range]) extends Range {
    def valid(version: SemVer): Boolean = ranges.exists(_.valid(version))
  }

  def parse(str: String) = RangeParser.range_parse(str)

  def apply(str: String): Range =
    try { parse(str).get }
    catch { case _: Throwable => throw new Error(s"range parse error: ${str}") }
}
