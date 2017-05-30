import com.typesafe.sbt.pgp.PgpKeys._
import sbt.Keys._
import sbtunidoc.Plugin.UnidocKeys._

val cScalaVersion = "2.12.2"
val cAkkaVersion = "2.4.17"
val cAkkaHttpVersion = "10.0.5"
val cMongoVersion = "2.0.0"
val cScalaTestVersion = "3.0.1"


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
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
  "-language:higherKinds",             // Allow higher-kinded types
  "-language:implicitConversions",     // Allow definition of implicit functions called views
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
  "-Xfuture",                          // Turn on future language features.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match",              // Pattern match may not be typesafe.
  "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification",             // Enable partial unification in type constructor inference
  "-Ywarn-dead-code",                  // Warn when dead code is identified.
  "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
  "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen",              // Warn when numerics are widened.
  "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
  "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
  "-Ywarn-unused:locals",              // Warn if a local definition is unused.
  "-Ywarn-unused:params",              // Warn if a value parameter is unused.
  "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
  "-Ywarn-unused:privates",            // Warn if a private member is unused.
  "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
)

version in Global := "1.1.0"

lazy val akkamoRoot = project.in(file("."))
  .settings(publish := {}, publishLocal := {}, publishSigned := {}, publishLocalSigned := {})
  .settings(unidocSettings: _*)
  .settings(unidocProjectFilter in(ScalaUnidoc, unidoc) := inAnyProject -- inProjects(akkamoSbtPlugin))
  .aggregate(
    akkamoAkkaDependencies,
    akkamoAkkaHttpDependencies,
    akkamoConfigMacro, akkamo, akkamoAkka, akkamoAkkaHttp, akkamoLog, akkamoAkkaLog, akkamoMongo, akkamoKafka, //akkamoReactivemongo
    akkamoPersistentConfig, akkamoMongoPersistentConfig, akkamoWebContent, akkamoSbtPlugin
  )


lazy val akkamoConfigMacro = project.in(file("akkamoConfigMacro")).settings(
  name := "akkamo-config-macro",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value  withSources,
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided"  withSources
  )
)


lazy val akkamo = project.in(file("akkamo")).settings(
  name := "akkamo",
  libraryDependencies ++= Seq(
    "com.typesafe" % "config" % "1.3.1",
     "org.scalatest" %% "scalatest" % cScalaTestVersion % "test" withSources
  )
).dependsOn(akkamoConfigMacro)

lazy val akkamoAkka = project.in(file("akkamoAkka")).settings(
  name := "akkamo-akka",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "org.scalatest" %% "scalatest" % cScalaTestVersion % "test" withSources
  )
).dependsOn(akkamo, akkamoLog)

lazy val akkamoLog = project.in(file("akkamoLog/api"))
  .settings(name := "akkamo-log").dependsOn(akkamo)

lazy val akkamoAkkaLog = project.in(file("akkamoLog/akka")).settings(
  name := "akkamo-akka-log",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "org.scalatest" %% "scalatest" % cScalaTestVersion % "test" withSources
  )

).dependsOn(akkamoAkka, akkamoLog)

lazy val akkamoAkkaHttp = project.in(file("akkamoAkkaHttp")).settings(
  name := "akkamo-akka-http",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % cAkkaHttpVersion % "provided" withSources,
    "com.typesafe.akka" %% "akka-http-testkit" % cAkkaHttpVersion % "test" withSources,
    "org.scalatest" %% "scalatest" % cScalaTestVersion % "test" withSources
  )
).dependsOn(akkamoAkka)

/*
lazy val akkamoReactivemongo = project.in(file("akkamoReactivemongo")).settings(
  name := "akkamo-reactivemongo",
  libraryDependencies ++= Seq(
    "org.reactivemongo" %% "reactivemongo" % cReactiveMongoVersion % "provided" withSources
  )
).dependsOn(akkamo, akkamoLog)
*/

lazy val akkamoMongo = project.in(file("akkamoMongo")).settings(
  name := "akkamo-mongo",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion % "provided" withSources,
    "org.mongodb.scala" %% "mongo-scala-driver" % cMongoVersion % "provided" withSources
  )
).dependsOn(akkamo, akkamoLog)

lazy val akkamoKafka = project.in(file("akkamoKafka")).settings(
  name := "akkamo-kafka",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion withSources,
    "org.apache.kafka" % "kafka-clients" % "0.9.0.1" % "provided" withSources
  )
).dependsOn(akkamo, akkamoLog)

lazy val akkamoWebContent = project.in(file("akkamoWebContent")).settings(
  name := "akkamo-web-content",
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http" % cAkkaHttpVersion % "provided" withSources
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
    "org.mongodb.scala" %% "mongo-scala-driver" % cMongoVersion % "provided" withSources,
    "org.scalatest" %% "scalatest" % cScalaTestVersion % "test" withSources
  )
).dependsOn(akkamoPersistentConfig, akkamoMongo)
  .dependsOn(akkamoAkkaLog % "test->compile")
  .dependsOn(akkamoAkkaDependencies % "test->compile")


// all akka dependencies
// may be published independently, version number follows Akka version
lazy val akkamoAkkaDependencies = project.in(file("akkamoAkkaDependencies")).settings(
  name := s"akkamo-akka-dependencies",
  version := cAkkaVersion,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-agent" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-camel" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-cluster" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-cluster-metrics" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-cluster-sharding" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-cluster-tools" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-contrib" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-multi-node-testkit" % cAkkaVersion % "test" withSources,
    "com.typesafe.akka" %% "akka-osgi" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-persistence" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-persistence-tck" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-remote" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-slf4j" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-stream" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-stream-testkit" % cAkkaVersion % "test" withSources,
    "com.typesafe.akka" %% "akka-testkit" % cAkkaVersion % "test" withSources,
    "com.typesafe.akka" %% "akka-distributed-data-experimental" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-typed-experimental" % cAkkaVersion withSources,
    "com.typesafe.akka" %% "akka-persistence-query-experimental" % cAkkaVersion withSources
  )
)

// all akka dependencies
// may be published independently, version number follows Akka version
lazy val akkamoAkkaHttpDependencies = project.in(file("akkamoAkkaHttpDependencies")).settings(
  name := s"akkamo-akka-http-dependencies",
  version := cAkkaHttpVersion,
  libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-http-core" % cAkkaHttpVersion withSources,
    "com.typesafe.akka" %% "akka-http" % cAkkaHttpVersion withSources,
    "com.typesafe.akka" %% "akka-http-testkit" % cAkkaHttpVersion % "test" withSources,
    "com.typesafe.akka" %% "akka-http-spray-json" % cAkkaHttpVersion withSources,
    "com.typesafe.akka" %% "akka-http-jackson" % cAkkaHttpVersion withSources,
    "com.typesafe.akka" %% "akka-http-xml" % cAkkaHttpVersion withSources
  )
)

lazy val akkamoSbtPlugin = project.in(file("akkamoSbtPlugin")).settings(
  name := "sbt-akkamo",
  scalaVersion := "2.10.6",
  sbtPlugin := true,
  scalacOptions := Seq("-deprecation", "-encoding", "utf8")
)