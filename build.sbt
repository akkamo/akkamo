import sbt.Keys._
import UnidocKeys._
import com.typesafe.sbt.pgp.PgpKeys._

lazy val cScalaVersion = "2.11.8"
lazy val cAkkaVersion = "2.4.9"
lazy val cReactiveMongoVersion = "0.11.14"


organization in Global := "eu.akkamo"

description := "Akkamo modules in Akka. Runtime assembly of several modules running on top of Akka."

crossScalaVersions in Global := Seq("2.11.8", cScalaVersion)

scalaVersion in Global := cScalaVersion

publishMavenStyle in Global := true

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"))

publishTo in Global := {
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

pomExtra in Global :=
  <url>http://www.akkamo.eu</url>
    <licenses>
      <license>
        <name>unlicense</name>
        <url>http://unlicense.org/</url>
        <distribution>repo</distribution>
      </license>
    </licenses>
    <scm>
      <url>https://github.com/akkamo/akkamo.git</url>
    </scm>
    <developers>
      <developer>
        <id>JurajBurian</id>
        <name>Juraj Burian</name>
        <url>https://github.com/JurajBurian</url>
      </developer>
      <developer>
        <id>VaclavSvejcar</id>
        <name>Vaclav Svejcar</name>
        <url>https://github.com/vaclavsvejcar</url>
      </developer>
    </developers>

scalacOptions in Global := Seq(
  "-encoding", "utf-8",
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:postfixOps",
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-Xfatal-warnings",
  "-Xlint",
  "-Xfuture",
  "-Yrangepos",
  "-Yrangepos",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Ywarn-unused-import"
)

version in Global := "1.1.0-SNAPSHOT"

lazy val akkamoRoot = project.in(file("."))
  .settings(publish := {}, publishLocal := {}, publishSigned := {}, publishLocalSigned := {})
  .settings(unidocSettings: _*)
  .settings(unidocProjectFilter in(ScalaUnidoc, unidoc) := inAnyProject -- inProjects(akkamoSbtPlugin))
  .aggregate(
    akkamo, akkamoAkkaHttp, akkamoReactivemongo, akkamoMongo, akkamoKafka,
    akkamoPersistentConfig, akkamoMongoPersistentConfig, akkamoWebContent, akkamoSbtPlugin
  )

lazy val akkamo = project.in(file("akkamo")).settings(
  name := "akkamo",
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-reflect" % cScalaVersion withSources ,
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "com.typesafe.akka" %% "akka-testkit" % cAkkaVersion % "test" withSources,
    "org.scalatest" %% "scalatest" % "3.0.0-RC2" % "test" withSources
  )
)

lazy val akkamoAkkaHttp = project.in(file("akkamoAkkaHttp")).settings(
  name := "akkamo-akka-http",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % cAkkaVersion % "provided" withSources,
    "com.typesafe.akka" %% "akka-http-testkit" % cAkkaVersion % "test" withSources,
    "org.scalatest" %% "scalatest" % "3.0.0-RC2" % "test" withSources
  )
).dependsOn(akkamo)


lazy val akkamoReactivemongo = project.in(file("akkamoReactivemongo")).settings(
  name := "akkamo-reactivemongo",
  libraryDependencies ++= Seq(
    "org.reactivemongo" %% "reactivemongo" % cReactiveMongoVersion % "provided" withSources
  )
).dependsOn(akkamo)

lazy val akkamoMongo = project.in(file("akkamoMongo")).settings(
  name := "akkamo-mongo",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "org.mongodb.scala" %% "mongo-scala-driver" % "1.1.1" % "provided"
  )
).dependsOn(akkamo)

lazy val akkamoKafka = project.in(file("akkamoKafka")).settings(
  name := "akkamo-kafka",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "org.apache.kafka" % "kafka-clients" % "0.9.0.1" % "provided" withSources
  )
).dependsOn(akkamo)

lazy val akkamoWebContent = project.in(file("akkamoWebContent")).settings(
  name := "akkamo-web-content",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http-experimental" % cAkkaVersion % "provided" withSources
  )
).dependsOn(akkamoAkkaHttp)


lazy val akkamoPersistentConfig = project.in(file("akkamoPersistentConfig/api")).settings(
  name := "akkamo-persistent-config",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources
  )
).dependsOn(akkamo)

lazy val akkamoMongoPersistentConfig = project.in(file("akkamoPersistentConfig/mongo")).settings(
  name := "akkamo-mongo-persistent-config",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "org.reactivemongo" %% "reactivemongo" % cReactiveMongoVersion % "provided" withSources
  )
).dependsOn(akkamoPersistentConfig, akkamoReactivemongo)

lazy val akkamoSbtPlugin = project.in(file("akkamoSbtPlugin")).settings(
  name := "sbt-akkamo",
  scalaVersion := "2.10.6",
  sbtPlugin := true,
  scalacOptions := Seq("-deprecation", "-encoding", "utf8")
)