# semver-parser-scala

[Semantic Versioning](http://semver.org/) and Range Library for Scala.

```scala
import com.github.sh4869.semver_parser.{SemVer, Range}
val ver = SemVer("1.0.0")
val ver2 = SemVer("1.2.0")
val ver3 = SemVer("2.0.0")
assert(ver2 > ver)
val range = Range("^1.0.0")
assert(range.valid(ver))
assert(range.valid(ver2))
assert(range.invalid(ver3))
```

## Semantic Versioning

Support for [semantic versioning 2.0.0](https://semver.org/spec/v2.0.0.html).

```scala
import com.github.sh4869.semver_parser.SemVer

SemVer("1.0.0")
SemVer("1.0.1")
SemVer("1.0.0-alpha")
SemVer("1.0.0-alpha.1")
SemVer("1.0.0+build")
SemVer("1.0.0+build.1.2")
SemVer("1.0.0-alpha+build")
SemVer("1.0.0-alpha.1.2+build.3")

// Compare Verison
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
```

## Range

Support range syntax of **npm**. see also [node-semver library description](https://github.com/npm/node-semver#advanced-range-syntax).

```scala
import com.github.sh4869.semver_parser.Range

Range("1.0.0").valid(SemVer("1.0.0")) // Simple Range
Range("^1.0.0").valid(SemVer("1.2.0")) // Hat Range
Range("~1.0.0").valid(SemVer("1.0.1")) // Tilde Range
Range("1.0.0 - 2.0.0").valid(SemVer("1.8.0")) // Hyphen Range
Range("<= 1.0.0").valid(SemVer("0.9.0")) // Conditon Range
Range("> 1.0.0 < 2.0.0").valid(SemVer("1.5.0")) // And Range
Range("1.0.0 || 2.0.0").valid(SemVer("2.0.0")) // Or Range
```

# Install

TODO

# TODO

- copy test from [node-semver](https://github.com/npm/node-semver)