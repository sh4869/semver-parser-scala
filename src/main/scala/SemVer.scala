

case class SemVer(major: Long, minor: Long, patch: Long, preRelease: Option[String], build: Option[String], original: String) extends Ordered[SemVer] {

  override def compare(that: SemVer): Int = {
    if (that.major != major) major.compare(that.major)
    else if (that.minor != minor) minor.compare(that.minor)
    else if (that.patch != patch) patch.compare(that.patch)
    else {

    }
    0
  }
}
