package com.github.sh4869.semver_parser

import scala.util.Try
import scala.util.parsing.combinator._

class CommonParser extends JavaTokenParsers {

  // dont skip whitespace
  override def handleWhiteSpace(source: CharSequence, offset: Int): Int = {
    offset
  }
  def num_identifier: Parser[Long] = """(0|[1-9]\d*)""".r ^^ { _.toLong }

  def prerelease_identifier: Parser[String] =
    prerelease_identifier_content ~ rep("." ~> prerelease_identifier_content) ^^ {
      case a ~ as => s"$a${if (as.isEmpty) "" else "." + as.mkString(".")}"
    }

  def prerelease_identifier_content: Parser[String] =
    alphanumeric_identifier

  def alphanumeric_identifier: Parser[String] =
    """([a-zA-Z\-][0-9a-zA-Z\-]*)""".r ^^ { _.toString() } |
      """([0-9a-zA-Z\-]*[a-zA-Z\-][0-9a-zA-Z\-]*)""".r ^^ { _.toString() } |
      """([0-9]\d*)""".r ^^ { _.toString() }

  def build_identifier: Parser[String] = build_identifier_content ~ rep("." ~> build_identifier_content) ^^ {
    case a ~ as => s"$a${if (as.isEmpty) "" else "." + as.mkString(".")}"
  }

  def build_identifier_content: Parser[String] = """([a-zA-Z0-9\-]+)""".r ^^ { _.toString() }
}

object CommonParser {

  /**
    * compare preRelease for semver
    * see also https://semver.org/#spec-item-11 .4
    *
    * @param preReleaseA
    * @param preReleaseB
    * @return
    */
  private[semver_parser] def comparePreRelease(preReleaseA: Option[String], preReleaseB: Option[String]): Int = {
    (parsePR(preReleaseA), parsePR(preReleaseB)) match {
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

  private def parsePR(opt: Option[String]) =
    opt.map[Seq[_]](x =>
      if (x.contains("."))
        x.split("\\.").toIndexedSeq.map(x => Try { x.toLong }.getOrElse(x))
      else Seq(x)
    )

}
