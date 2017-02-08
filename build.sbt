lazy val root = (project in file(".")).
	settings(
		name := "mongoose",
		organization := "com.emc",
		version := "3.2.0",
		description := "Mongoose is a high-load storage performance testing automation tool.",
		// Enables publishing to maven repo
		publishMavenStyle := true,
		// Do not append Scala versions to the generated artifacts
		crossPaths := false,
		// This forbids including Scala related libraries into the dependency
		autoScalaLibrary := false,
		javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8")
	)
