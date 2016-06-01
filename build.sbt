import sbt.Keys._

lazy val cScalaVersion =  "2.11.8"
lazy val cAkkaVersion =  "2.4.4"

organization in Global := "eu.akkamo"

description := "Akkamo modules in Akka. Runtime assembly of several modules running on top of Akka."

crossScalaVersions in Global := Seq("2.11.8", cScalaVersion)

scalaVersion in Global := cScalaVersion

publishMavenStyle := true

cancelable in Global := true

fork in (IntegrationTest, run) := true

resolvers ++= Seq(
	Resolver.sonatypeRepo("releases"),
	Resolver.sonatypeRepo("snapshots"))

publishTo := {
	val nexus = "https://oss.sonatype.org/"
	if (isSnapshot.value)
		Some("snapshots" at nexus + "content/repositories/snapshots")
	else
		Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

// enable automatic linking to the external Scaladoc of managed dependencies
autoAPIMappings := true

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := (
	<url>http://www.akkamo.eu</url>
		<licenses>
			<license>
				<name>unlicense</name>
				<url>http://unlicense.org/</url>
				<distribution>repo</distribution>
			</license>
		</licenses>
		<scm>
			<url>https://github.com/akkamo/akkamo</url>
			<connection>scm:git:https://github.com/akkamo/akkamo</connection>
		</scm>
		<developers>
			<developer>
				<id>JurajBurian</id>
				<name>Juraj Burian</name>
				<url>https://github.com/JurajBurian</url>
			</developer>
			<developer>
				<id>xwinus</id>
				<name>Vaclav Svejcar</name>
				<url>https://github.com/xwinus</url>
			</developer>
		</developers>)

scalacOptions in Global := Seq(
	"-encoding", "utf-8",
	"-unchecked",
	"-deprecation",
	"-feature",
	"-Xfatal-warnings",
	"-Xlint",
	"-Yrangepos",
	"-language:postfixOps"
)

version in Global := "1.0.0-SNAPSHOT"

lazy val akkamoRoot = project.in(file(".")).settings (publish := { }, publishLocal:={}).aggregate(akkamo, akkamoAkkaHttp, akkamoSbtPlugin)

lazy val akkamo = project.in(file("akkamo")).settings(
	name := "akkamo",
	libraryDependencies ++= Seq(
		"org.scala-lang" % "scala-reflect" % cScalaVersion withSources,
		"com.typesafe.akka" %% "akka-actor" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-cluster-tools" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-cluster" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-contrib" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-testkit" % cAkkaVersion % "test" withSources
	)
)

lazy val akkamoAkkaHttp = project.in(file("akkamoAkkaHttp")).settings(
	name := "akkamo-akka-http",
	libraryDependencies ++= Seq(
		"com.typesafe.akka" %% "akka-http-experimental" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-http-testkit" % cAkkaVersion % "test" withSources
	)
).dependsOn(akkamo)


lazy val akkamoSbtPlugin = project.in(file("akkamoSbtPlugin")).settings(
	name := "sbt-akkamo",
	scalaVersion := "2.10.6",
	sbtPlugin := true,
	scalacOptions := Seq("-deprecation", "-encoding", "utf8")
)