import sbt._

object Version {
	// dependencies for testing
	val scalaTest = "3.1.+"

	// dependencies for main feature
	val prometheus = "0.8.1"
}

object Library {
	// dependencies for testing
	val scalaTest = "org.scalatest" %% "scalatest" % Version.scalaTest

	// dependencies for main feature
	val prometheus = "io.prometheus" % "simpleclient_pushgateway" % Version.prometheus
}

object Dependencies {

	import Library._

	val dependencies: Seq[ModuleID] = Seq(
		scalaTest % Test,

		prometheus,
	)
}