lazy val cScalaVersion =  "2.11.8"
lazy val cAkkaVersion =  "2.4.4"

organization := "com.github.jurajburian"

name := "makka"

version := "1.0.0"

description := "Injection & configuration wraper on top of Typesafe config"

crossScalaVersions := Seq("2.11.8", cScalaVersion)

scalaVersion in Global := cScalaVersion

publishMavenStyle := true

cancelable in Global := true

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

scalacOptions := Seq(
	"-encoding", "UTF-8",
	"-unchecked",
	"-deprecation",
	"-feature",
	"-Xfatal-warnings",
	"-Xlint",
	"-Yrangepos",
	"-language:postfixOps"
)

libraryDependencies ++= Seq(
	"org.scala-lang" % "scala-reflect" % cScalaVersion withSources,
	"com.typesafe.akka" %% "akka-actor" % cAkkaVersion withSources,
	"com.typesafe.akka" %% "akka-stream" % cAkkaVersion withSources,
	"com.typesafe.akka" %% "akka-http-experimental" % cAkkaVersion withSources,
	"com.typesafe.akka" %% "akka-http-testkit" % cAkkaVersion % "test" withSources
)
