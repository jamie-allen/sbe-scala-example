import JmhKeys._

name := """sbe-scala-example"""

version := "0.3-SNAPSHOT"

jmhSettings

outputTarget in Jmh := target.value / s"scala-${scalaBinaryVersion.value}"

libraryDependencies += "uk.co.real-logic" % "sbe" % "1.0-RC2"