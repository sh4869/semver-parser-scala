package com.github.sh4869.semver_parser

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

class RangeTest extends AnyFunSuite with Matchers {
  import Range._

  def check(range: Range, valid: String, invalid: String) = {
    range.valid(SemVer(valid)) && range.invalid(SemVer(invalid))
  }
  test("specify") {
    val x = SemVer("100.100.100")
    assert(parse("1.0.0") exists (r => r.valid(SemVer("1.0.0")) && r.invalid(x)))
    assert(parse("2.3.4") exists (r => r.valid(SemVer("2.3.4")) && r.invalid(x)))
    assert(parse("123.456.789") exists (r => r.valid(SemVer("123.456.789")) && r.invalid(x)))
    assert(parse("0.1.0") exists (r => r.valid(SemVer("0.1.0")) && r.invalid(x)))
    assert(parse("0.1.1") exists (r => r.valid(SemVer("0.1.1")) && r.invalid(x)))
  }

  test("x range/partital") {
    assert(parse("1.2.x") exists (r => check(r, "1.2.3", "1.3.0")))
    assert(parse("1.x") exists (r => check(r, "1.5.3", "2.0.0")))
    assert(parse("*") exists (r => check(r, "12.0.0", "1.0.0-alph")))
    assert(parse("1") exists (r => check(r, "1.3.0", "2.0.0")))
    assert(parse("1.1") exists (r => check(r, "1.1.3", "1.2.0")))
    assert(parse("") exists (r => check(r, "12.0.0", "2.0.0-beta")))
  }

  test("caret range") {
    assert(parse("^1.2.3") exists (r => check(r, "1.3.0", "2.0.0")))
    assert(parse("^0.2.0") exists (r => check(r, "0.2.3", "0.3.0")))
    assert(parse("^1.2.3-beta.2") exists (r => check(r, "1.2.3-beta.4", "1.2.4-beta.2")))
    assert(parse("^0.0.3-beta") exists (r => check(r, "0.0.3-pr.2", "0.0.4")))
    assert(parse("^1.0.x") exists (r => check(r, "1.2.4", "2.3.0")))
    assert(parse("^1.x") exists (r => check(r, "1.0.0", "2.0.0")))
    assert(parse("^0.0.x") exists (r => check(r, "0.0.1", "0.1.0")))
    assert(parse("^0.0") exists (r => r.valid(SemVer("0.0.1"))))
    assert(parse("^0.x") exists (r => check(r, "0.1.1", "1.0.0")))
    assert(parse("^1.x") exists (r => check(r, "1.1.1", "2.0.0")))
  }

  test("tilde range") {
    assert(parse("~1.2.3") exists (r => check(r, "1.2.4", "1.3.0")))
    assert(parse("~1.2") exists (r => check(r, "1.2.2", "1.3.0")))
    assert(parse("~1") exists (r => check(r, "1.3.0", "2.0.0")))
    assert(parse("~0.2.3") exists (r => check(r, "0.2.4", "0.3.0")))
    assert(parse("~0.3") exists (r => check(r, "0.3.2", "0.4.0")))
    assert(parse("~0") exists (r => check(r, "0.3.0", "1.0.0")))
    assert(parse("~1.2.3-beta.2") exists (r => check(r, "1.2.3-beta.4", "1.2.4-beta.2")))
  }

  test("condition") {
    // >
    assert(parse("> 1.0.0") exists (r => check(r, "1.3.0", "0.5.0")))
    assert(parse("> 0.5.0") exists (r => check(r, "0.8.0", "0.3.0")))
    assert(parse("> 0.4") exists (r => check(r, "0.8.0", "0.4.3")))
    assert(parse("> 1") exists (r => check(r, "2.0.0", "1.5.0")))
    assert(parse("> 1.0.0-rc.1") exists (r => check(r, "1.0.0-rc.2", "0.9.0-rc.0")))
    assert(parse("> x") exists (r => r.invalid(SemVer("1.0.0"))))
    // <
    assert(parse("< 1.0.0") exists (r => check(r, "0.5.0", "1.1.0")))
    assert(parse("< 0.5.0") exists (r => check(r, "0.4.0", "0.5.0")))
    assert(parse("< 0.4") exists (r => check(r, "0.3.9", "0.4.0")))
    assert(parse("< 1") exists (r => check(r, "0.5.0", "1.0.0")))
    assert(parse("> 1.0.0-rc.1") exists (r => check(r, "1.0.0-rc.2", "1.2.0-rc.1")))
    assert(parse("< x") exists (r => r.invalid(SemVer("1.0.0"))))
    // >=
    assert(parse(">= 1.0.0") exists (r => check(r, "1.0.0", "0.5.0")))
    assert(parse(">= 0.3.0") exists (r => check(r, "0.3.0", "0.2.9")))
    assert(parse(">= 1.0") exists (r => check(r, "1.0.2", "0.9.1")))
    assert(parse(">= 1") exists (r => check(r, "1.3.0", "0.0.1")))
    assert(parse(">= 1.0.0-rc.1") exists (r => check(r, "1.0.0-rc.1", "1.2.0-rc.1")))
    assert(parse(">= x") exists (r => r.valid(SemVer("12.0.0"))))
    // <=
    assert(parse("<= 1.0.0") exists (r => check(r, "1.0.0", "1.1.0")))
    assert(parse("<= 0.3.0") exists (r => check(r, "0.3.0", "0.3.1")))
    assert(parse("<= 1.2") exists (r => check(r, "1.2.1", "1.3.5")))
    assert(parse("<= 1") exists (r => check(r, "1.3.0", "2.0.0")))
    assert(parse("<= 1.0.0-rc.1") exists (r => check(r, "1.0.0-rc.1", "0.9.0-rc.1")))
    assert(parse("<= x") exists (r => r.valid(SemVer("12.0.0"))))
    // error
    assert(parse("> ^1.0.0").isEmpty)
    assert(parse("> ~1.0.0").isEmpty)
  }

  test("hyphen") {
    // hyphen error
    assert(parse("1.1.0 - 2.2.0 - 3.3.0").isEmpty)
    assert(parse("1.1.0 - 2.2.0 < 3.3.0").isEmpty)
    assert(parse(("^1.1.0 - 2.2.0")).isEmpty)
    // hypen
    assert(parse("1.2.3 - 2.3.4") exists (r => check(r, "1.3.0", "2.3.5")))
    assert(parse("1.2 - 2.3.4") exists (r => check(r, "1.2.3", "2.3.5")))
    assert(parse("1 - 2") exists (r => check(r, "1.2.3", "3.4.0")))
  }

  test("and range") {
    assert(parse("^1.0.0 < 1.5.0") exists (r => check(r, "1.2.0", "1.6.0")))
    assert(parse("x < 1.5.0") exists (r => check(r, "0.3.0", "1.6.0")))
    assert(parse("<=1.3.6 ~1.3.0") exists (r => check(r, "1.3.5", "1.3.7")))
  }
  test("or range") {
    assert(parse("1.0.0 || ^2.0.0") exists (r => check(r, "1.0.0", "1.2.0")))
    assert(parse("1.0.0||^2.0.0") exists (r => check(r, "1.0.0", "1.2.0")))
    assert(parse("~1.0.0 || ^2.0.0") exists (r => check(r, "1.0.1", "1.2.0")))
    assert(parse("1.0.0 - 3.0.0 || 4.0.0 - 5.6.0") exists (r => check(r, "4.0.0", "3.5.0")))
  }

  test("parse error") {
    assert(parse("aaaa").isEmpty)
    assert(parse("1.1.1.1").isEmpty)
    assert(parse("1.1.1.1").isEmpty)
    assert(parse(">").isEmpty)
  }

}
