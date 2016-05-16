import sbt.Keys._

lazy val cScalaVersion =  "2.11.8"
lazy val cAkkaVersion =  "2.4.4"

organization in Global := "com.github.jurajburian"

description := "Makka modules in Akka. Runtime assembly of several modules running on top of Akka."

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
	<url>https://github.com/JurajBurian/ic</url>
		<licenses>
			<license>
				<name>unlicense</name>
				<url>http://unlicense.org/</url>
				<distribution>repo</distribution>
			</license>
		</licenses>
		<scm>
			<url>https://github.com/jurajburian/ic</url>
			<connection>scm:git:https://github.com/jurajburian/ic</connection>
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

lazy val root = project.in(file(".")).settings (publish := { }, publishLocal:={}).aggregate(makka, makkaAkkaHttp, makkaSbtPlugin)

lazy val makka = project.in(file("makka")).settings(
	name := "makka",
	libraryDependencies ++= Seq(
		"org.scala-lang" % "scala-reflect" % cScalaVersion withSources,
		"com.typesafe.akka" %% "akka-actor" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-cluster-tools" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-slf4j" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-cluster" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-contrib" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-testkit" % cAkkaVersion % "test" withSources
	)
)

lazy val makkaAkkaHttp = project.in(file("makkaAkkaHttp")).settings(
	name := "makka-akka-http",
	libraryDependencies ++= Seq(
		"com.typesafe.akka" %% "akka-http-experimental" % cAkkaVersion withSources,
		"com.typesafe.akka" %% "akka-http-testkit" % cAkkaVersion % "test" withSources
	)
).dependsOn(makka)


lazy val makkaSbtPlugin = project.in(file("makkaSbtPlugin")).settings(
	name := "sbt-makka",
	scalaVersion := "2.10.6",
	sbtPlugin := true,
	scalacOptions := Seq("-deprecation", "-encoding", "utf8")
)