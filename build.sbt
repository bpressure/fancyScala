
// ###############
// ### Version ###
// ###############

name := "fancyScala"

version := "4.2.2"

organization := "fancyScala"

scalaVersion := "2.11.8"

// ####################
// ### Dependencies ###
// ####################

libraryDependencies ++= Dependencies.dependencies

// https://github.com/sbt/sbt/issues/3618
// sbt cannot handle ${packaging.type} variable in pom files.
val workaround: Unit = {
	sys.props += "packaging.type" -> "jar"
	()
}

// #######################
// ### Compile Options ###
// #######################

scalacOptions := Seq("-unchecked", "-deprecation", "-Ywarn-unused-import", "-language:postfixOps")
