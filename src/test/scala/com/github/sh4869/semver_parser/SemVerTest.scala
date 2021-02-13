package com.github.sh4869.semver_parser
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
class SemVerTest extends AnyFunSuite with Matchers {
  import SemVer._

  test("versions") {
    // Suucess
    assert(parse("1.0.0") == Some(SemVer(1, 0, 0, None, None)))
    assert(parse("0.1.0") == Some(SemVer(0, 1, 0, None, None)))
    assert(parse("0.0.1") == Some(SemVer(0, 0, 1, None, None)))
    assert(parse("100000.100000.100000") == Some(SemVer(100000, 100000, 100000, None, None)))
    assert(parse("123.456.789") == Some(SemVer(123, 456, 789, None, None)))
    assert(parse("1.1.0-rc.20190924164006453") == Some(SemVer(1, 1, 0, Some("rc.20190924164006453"), None)))
    assert(parse("1.201508240703.1") == Some(SemVer(1, 201508240703L, 1, None, None)))
    // Failed
    assert(parse("x.0.0") == None)
    assert(parse("1") == None)
    assert(parse("1.0.x") == None)
    assert(parse("01.0.0") == None)
    assert(parse("1.0.0.0") == None)
  }

  test("with pre-rerelase") {
    // Success
    assert(parse("1.0.0-alpha") == Some(SemVer(1, 0, 0, Some("alpha"), None)))
    assert(parse("0.1.0-alpha.1") == Some(SemVer(0, 1, 0, Some("alpha.1"), None)))
    assert(parse("0.0.1-alpha-x") == Some(SemVer(0, 0, 1, Some("alpha-x"), None)))
    assert(parse("1.0.0-120") == Some(SemVer(1, 0, 0, Some("120"), None)))
    assert(parse("1.0.0-012a") == Some(SemVer(1, 0, 0, Some("012a"), None)))
    // Failed
    assert(parse("1.0.0-") == None) // Empty
    assert(parse("1.0.0-120x..a12") == None) // Empty
    assert(parse("1.0.0-012") == Some(SemVer(1, 0, 0, Some("012"), None))) // 0 start number
    assert(parse("1.0.0-0120x.012") == Some(SemVer(1, 0, 0, Some("0120x.012"), None))) // 0 start number
    assert(parse("1.0.0-_x_") == None) // invalit character
  }

  test("with build") {
    // Success
    assert(parse("1.0.0+build") == Some(SemVer(1, 0, 0, None, Some("build"))))
    assert(parse("1.0.0+120") == Some(SemVer(1, 0, 0, None, Some("120"))))
    assert(parse("1.0.0+012") == Some(SemVer(1, 0, 0, None, Some("012"))))
    assert(parse("1.0.0+build.test-") == Some(SemVer(1, 0, 0, None, Some("build.test-"))))
    assert(parse("1.0.0+build.xxx.000.aaa") == Some(SemVer(1, 0, 0, None, Some("build.xxx.000.aaa"))))
    // Failed
    assert(parse("1.0.0+").isEmpty) // Empty
    assert(parse("1.0.0+_").isEmpty) // invalit character
  }

  test("with build and prerelease") {
    assert(parse("1.0.0-alpha-build+build") == Some(SemVer(1, 0, 0, Some("alpha-build"), Some("build"))))
    assert(parse("1.0.0-alpha.beta+aaaaa") == Some(SemVer(1, 0, 0, Some("alpha.beta"), Some("aaaaa"))))
  }

  test("other") {
    assert(parse("NG").isEmpty)
    assert(parse("aaaaa~~~~").isEmpty)
    assert(parse("1.0.0 - alpha").isEmpty)
  }

  test("semver compareble") {
    assert(SemVer("1.1.0") > SemVer("0.1.0"))
    assert(SemVer("0.2.0") > SemVer("0.1.0"))
    assert(SemVer("0.1.1") > SemVer("0.1.0"))
    assert(SemVer("1.1.0") > SemVer("1.0.0-rc.1"))
    assert(SemVer("1.1.0-rc.1") > SemVer("1.0.0-beta.11"))
    assert(SemVer("1.1.0-beta.11") > SemVer("1.0.0-beta.2"))
    assert(SemVer("1.1.0-beta.2") > SemVer("1.0.0-beta.1"))
    assert(SemVer("1.1.0-beta.1") > SemVer("1.0.0-beta"))
    assert(SemVer("1.1.0-beta.1") > SemVer("1.0.0-beta"))
    assert(SemVer("1.0.0-beta") > SemVer("1.0.0-alpha.beta"))
    assert(SemVer("1.0.0-alpha.beta") > SemVer("1.0.0-alpha.1"))
    assert(SemVer("1.0.0-alpha.1") > SemVer("1.0.0-alpha"))
    assert(SemVer("1.0.0-alpha") > SemVer("0.9.0"))
  }

}
