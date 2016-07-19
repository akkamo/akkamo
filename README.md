# _Akkamo_ - modules in Akka.
Runtime assembly of several modules running on top of Akka.

## Documentation
Actual documentation is available on [http://_Akkamo_.eu/](http://_Akkamo_.eu/).
## API documentation
_ScalaDoc_ documentation for each version is available online for, see:

* [version 1.0.x](http://akkamo.github.io/api/1.0/)

## Install
_Akkamo_ is available for Scala 2.11
To get started with SBT, add dependency to your build.sbt file:
```Scala
libraryDependencies += "eu._Akkamo_" %% "_Akkamo_" % "1.0.0" withSources
```
_Akkamo_ consist from multiple modules, please consult documentation 
if you want use other modules.

## Execution
There is several ways how to execute _Akkamo_ based application. As developer
one can use `sbt-akkamo` plugin, with commands `runAkkamo` (`run-akkamo`) or 
`stopAkkamo` (`stop-akkamo`).
The second way is to use `run` (main class is: `eu.akkamo.Main`) command in `sbt`. 

We have not direct support for application packaging, one can use `sbt-native-packager` sbt plugin.
  
## Examples
One example is project can be found on [github.com/akkamo/akkamo-demo](https://github.com/akkamo/akkamo-demo).
Project demonstrate instantiation of two Actor systems with several `Actors`, and usage of 
`akkamo-akka-http` module.
 
  
### Open problems

* We do not solve problem with the main class registration to sbt from akkamo sbt plugin,
 so one must have defined one in sbt build definition, here is an example:
 ```Scala
 lazy val akkamoDemo = project.in(file(".")).dependsOn(pingAkkamo, pongAkkamo, httpAkkamo).settings(
 	name := "akkamo-demo",
 	mainClass in Compile := Some("eu.akkamo.Main")
 ).enablePlugins(JavaAppPackaging, AkkamoSbtPlugin)
 ```
 
 



