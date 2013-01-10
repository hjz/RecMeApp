import sbt._
import Keys._
import PlayProject._

import org.ensime.sbt.Plugin.Settings.ensimeConfig
import org.ensime.sbt.util.SExp._

object ApplicationBuild extends Build {

  val appName         = "RecMeApp"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.codahale" %% "jerkson" % "0.5.0",
    "com.factual" % "factual-java-driver" % "1.6.0",
    "fi.foyt" % "foursquare-api" % "1.0.2"
  )

  // Only compile the bootstrap bootstrap.less file and any other *.less file in the stylesheets directory
  def customLessEntryPoints(base: File): PathFinder = (
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "bootstrap.less") +++
    (base / "app" / "assets" / "stylesheets" / "bootstrap" * "responsive.less") +++
    (base / "app" / "assets" / "stylesheets" * "*.less")
  )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      lessEntryPoints <<= baseDirectory(customLessEntryPoints),
      resolvers += "t2v.jp repo" at "http://www.t2v.jp/maven-repo/",
      resolvers += "jbcrypt repo" at "http://mvnrepository.com/",
      resolvers += "4sq repo" at "http://foursquare-api-java.googlecode.com/svn/repository"
    )
}
