import java.nio.file.Paths
import jawn.support.json4s.Parser
import scala.language.postfixOps

val mongooseConfig = (Parser parseFromFile(Paths.get("config", "defaults.json") toFile)) get

// core dependencies
val commonsCodec = "commons-codec" % "commons-codec" % "1.10"
val commonsCollections4 = "org.apache.commons" % "commons-collections4" % "4.1"
val commonsLang = "commons-lang" % "commons-lang" % "2.6"
val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.4"
val log4jApi = "org.apache.logging.log4j" % "log4j-api" % "2.7"
val log4jCore = "org.apache.logging.log4j" % "log4j-core" % "2.7"
val log4jIostreams = "org.apache.logging.log4j" % "log4j-iostreams" % "2.7"
val metricsCore = "io.dropwizard.metrics" % "metrics-core" % "3.1.2"
val nettyBuffer = "io.netty" % "netty-buffer" % "4.1.6.Final"
val nettyCodecHttp = "io.netty" % "netty-codec-http" % "4.1.6.Final"
val nettyCommon = "io.netty" % "netty-common" % "4.1.6.Final"
val nettyHandler = "io.netty" % "netty-handler" % "4.1.6.Final"
val nettyTransport = "io.netty" % "netty-transport" % "4.1.6.Final"
val nettyTransportNativeEpoll = "io.netty" % "netty-transport-native-epoll" % "4.1.6.Final" classifier "linux-x86_64"

// storage mock only dependencies
val jmdns = "org.jmdns" % "jmdns" % "3.5.1"

// runtime only dependencies
val disruptor = "com.lmax" % "disruptor" % "3.3.4"
val javassist = "org.javassist" % "javassist" % "3.21.0-GA"
val log4jJul = "org.apache.logging.log4j" % "log4j-jul" % "${depVersion.log4j}"

// test only dependencies
val commonsCsv = "org.apache.commons" % "commons-csv" % "1.1" % Test
val commonsIo = "commons-io" % "commons-io" % "2.5" % Test
val commonsMath = "org.apache.commons" % "commons-math3" % "3.6.1" % Test
val jsonSchemaValidator = "com.github.fge" % "json-schema-validator" % "2.2.6" % Test
val junit = "junit" % "junit" % "4.12" % Test
val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test

lazy val commonSettings = Seq(
	organization := "com.emc.mongoose",
	version := ((mongooseConfig \ "version") toString),
	// Enables publishing to maven repo
	publishMavenStyle := true,
	// Do not append Scala versions to the generated artifacts
	crossPaths := false,
	// This forbids including Scala related libraries into the dependency
	autoScalaLibrary := false,
	javacOptions in (Compile, compile) ++= Seq("-source", "1.8", "-target", "1.8"),
	compileOrder := CompileOrder.JavaThenScala
)

lazy val mongoose = (project in file("."))
	.settings(commonSettings)
	.aggregate(
		common,
		loadGenerator,
		loadMonitor,
		model,
		run,
		storageDriverBase,
		storageDriverBuilder,
		storageDriverNetBase,
		storageDriverNetHttpAtmos,
		storageDriverNetHttpBase,
		storageDriverNetHttpS3,
		storageDriverNetHttpSwift,
		storageDriverNioBase,
		storageDriverNioFs,
		storageDriverService,
		storageMock,
		testsPerf,
		testsSystem,
		testsUnit,
		ui
	)

lazy val common = (project in file("common"))
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsCollections4,
			commonsLang
		),
		name := "mongoose-core"
	)

lazy val loadGenerator = (project in Paths.get("load", "generator").toFile())
	.dependsOn(
		common,
		model,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore
		),
		name := "mongoose-load-generator"
	)

lazy val loadMonitor = (project in Paths.get("load", "monitor").toFile())
	.dependsOn(
		common,
		model,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsLang,
			log4jApi,
			log4jCore,
			metricsCore
		),
		name := "mongoose-load-monitor"
	)

lazy val model = (project in file("model"))
	.dependsOn(
		common
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsCollections4,
			commonsCodec,
			metricsCore
		),
		name := "mongoose-model"
	)

lazy val run = (project in file("run"))
	.dependsOn(
		common,
		model,
		loadGenerator,
		loadMonitor,
		storageDriverBuilder,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi
		),
		name := "mongoose-run"
	)

lazy val storageDriverBase = (project in Paths.get("storage", "driver", "base").toFile())
	.dependsOn(
		common,
		model,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore
		),
		name := "mongoose-storage-driver-base"
	)

lazy val storageDriverBuilder = (project in Paths.get("storage", "driver", "builder").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverNetBase,
		storageDriverNetHttpAtmos,
		storageDriverNetHttpBase,
		storageDriverNetHttpS3,
		storageDriverNetHttpSwift,
		storageDriverNioBase,
		storageDriverNioFs,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi
		),
		name := "mongoose-storage-driver-builder"
	)

lazy val storageDriverNetBase = (project in Paths.get("storage", "driver", "net", "base").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsLang,
			log4jApi,
			log4jCore,
			nettyBuffer,
			nettyCommon,
			nettyHandler,
			nettyTransport,
			nettyTransportNativeEpoll
		),
		name := "mongoose-storage-driver-net-base"
	)

lazy val storageDriverNetHttpAtmos = (project in Paths.get("storage", "driver", "net", "http", "atmos").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverNetBase,
		storageDriverNetHttpBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore,
			nettyBuffer,
			nettyCodecHttp,
			nettyCommon,
			nettyHandler,
			nettyTransport
		),
		name := "mongoose-storage-driver-net-http-atmos"
	)

lazy val storageDriverNetHttpBase = (project in Paths.get("storage", "driver", "net", "http", "base").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverNetBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore,
			nettyBuffer,
			nettyCodecHttp,
			nettyCommon,
			nettyHandler,
			nettyTransport
		),
		name := "mongoose-storage-driver-net-http-base"
	)

lazy val storageDriverNetHttpS3 = (project in Paths.get("storage", "driver", "net", "http", "s3").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverNetBase,
		storageDriverNetHttpBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore,
			nettyBuffer,
			nettyCodecHttp,
			nettyCommon,
			nettyHandler,
			nettyTransport
		),
		name := "mongoose-storage-driver-net-http-s3"
	)

lazy val storageDriverNetHttpSwift = (project in Paths.get("storage", "driver", "net", "http", "swift").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverNetBase,
		storageDriverNetHttpBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			jacksonDatabind,
			log4jApi,
			log4jCore,
			nettyBuffer,
			nettyCodecHttp,
			nettyCommon,
			nettyHandler,
			nettyTransport
		),
		name := "mongoose-storage-driver-net-http-swift"
	)

lazy val storageDriverNioBase = (project in Paths.get("storage", "driver", "nio", "base").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore
		),
		name := "mongoose-storage-driver-nio-base"
	)

lazy val storageDriverNioFs = (project in Paths.get("storage", "driver", "nio", "fs").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverNioBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore
		),
		name := "mongoose-storage-driver-nio-fs"
	)

lazy val storageDriverService = (project in Paths.get("storage", "driver", "service").toFile())
	.dependsOn(
		common,
		model,
		storageDriverBase,
		storageDriverBuilder,
		storageDriverNetBase,
		storageDriverNetHttpAtmos,
		storageDriverNetHttpBase,
		storageDriverNetHttpS3,
		storageDriverNetHttpSwift,
		storageDriverNioBase,
		storageDriverNioFs,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			log4jApi,
			log4jCore
		),
		name := "mongoose-storage-driver-service"
	)

lazy val storageMock = (project in Paths.get("storage", "mock").toFile())
	.dependsOn(
		common,
		model,
		storageDriverNetBase,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsCodec,
			commonsCollections4,
			commonsLang,
			jacksonDatabind,
			jmdns,
			log4jApi,
			log4jCore,
			metricsCore,
			nettyBuffer,
			nettyCodecHttp,
			nettyCommon,
			nettyHandler,
			nettyTransport,
			nettyTransportNativeEpoll
		),
		name := "mongoose-storage-mock"
	)

lazy val testsPerf = (project in Paths.get("tests", "perf").toFile())
	.dependsOn(
		common,
		model,
		loadGenerator,
		loadMonitor,
		run,
		storageDriverBase,
		storageDriverBuilder,
		storageDriverNetBase,
		storageDriverNetHttpAtmos,
		storageDriverNetHttpBase,
		storageDriverNetHttpS3,
		storageDriverNetHttpSwift,
		storageDriverNioBase,
		storageDriverNioFs,
		storageDriverService,
		storageMock,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			junit
		),
		name := "mongoose-tests-perf"
	)

lazy val testsSystem = (project in Paths.get("tests", "system").toFile())
	.dependsOn(
		common,
		model,
		loadGenerator,
		loadMonitor,
		run,
		storageDriverBase,
		storageDriverBuilder,
		storageDriverNetBase,
		storageDriverNetHttpAtmos,
		storageDriverNetHttpBase,
		storageDriverNetHttpS3,
		storageDriverNetHttpSwift,
		storageDriverNioBase,
		storageDriverNioFs,
		storageDriverService,
		storageMock,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsCsv,
			commonsIo,
			commonsMath,
			log4jApi,
			log4jCore,
			junit
		),
		name := "mongoose-tests-system"
	)

lazy val testsUnit = (project in Paths.get("tests", "unit").toFile())
	.dependsOn(
		common,
		model,
		loadGenerator,
		loadMonitor,
		run,
		storageDriverBase,
		storageDriverBuilder,
		storageDriverNetBase,
		storageDriverNetHttpAtmos,
		storageDriverNetHttpBase,
		storageDriverNetHttpS3,
		storageDriverNetHttpSwift,
		storageDriverNioBase,
		storageDriverNioFs,
		storageDriverService,
		storageMock,
		ui
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsIo,
			jsonSchemaValidator,
			junit,
			junitInterface
		),
		name := "mongoose-tests-unit"
	)

lazy val ui = (project in file("ui"))
	.dependsOn(
		common,
		model
	)
	.settings(
		commonSettings,
		libraryDependencies ++= Seq(
			commonsLang,
			jacksonDatabind,
			log4jApi,
			log4jCore,
			log4jIostreams
		),
		name := "mongoose-ui"
	)