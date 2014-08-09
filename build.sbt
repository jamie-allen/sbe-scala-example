import JmhKeys._

name := """sbe-scala-example"""

version := "0.3-SNAPSHOT"

jmhSettings

outputTarget in Jmh := target.value / s"scala-${scalaBinaryVersion.value}"

libraryDependencies ++= Seq(
							"org.openjdk.jmh" % "jmh-core" % "0.3.2",
                            "uk.co.real-logic" % "sbe" % "1.0-RC2"))
