# _Akkamo_ - modules in Akka.
Runtime assembly of several modules running on top of Akka.

## Documentation
Full  documentation is available on [http://akkamo.eu](http://akkamo.eu).

## API documentation
_ScalaDoc_ documentation for each version is available online for, see:

* [version 1.0.x](http://akkamo.github.io/api/1.0/)

## Installation
_Akkamo_ is available for Scala 2.11
To get started with SBT, add dependency to your `build.sbt` file:
```Scala
libraryDependencies += "eu.akkamo" %% "akkamo" % "1.0.0" withSources
```
_Akkamo_ consist from multiple modules, please consult documentation 
if you want use other modules.

## Execution
There is several ways how to execute _Akkamo_ based application. As developer
one can use `sbt-akkamo` plugin, with commands `runAkkamo` (`run-akkamo`) or 
`stopAkkamo` (`stop-akkamo`).
The second way is to use `run` (main class is: `eu.akkamo.Main`) command in `sbt`. 

We have not direct support for application packaging, one can use `sbt-native-packager` sbt plugin.
  
## Project examples
Example project [akkamp-demo](https://github.com/akkamo/akkamo-demo) demonstrates creation of two
*Akka* actor systems and shows usage of the *Akka HTTP module*.
  
## Known issues
- Each *Akkamo* application must specify the class `eu.akkamo.Main` as its main class, unfortunately
  at this moment is has to be done manually in each project, as the *Akkamo* sbt module is unable
  to do so. To specify the main class manually, set the following key in your project configuration:
  
  ```scala
  mainClass in Compile := Some("eu.akkamo.Main")
  ```
 



