# What is Akkamo?

Main goal of the *Akkamo* project is to make modularization of your Scala applications much easier. Its based on runtime assembly of provided modules, built on top of the [Akka](http://akka.io) actor platform.

*Akkamo* platform allows to decompose your monolithic application into the set of modules. *Akkamo* modules can work independently, or can define dependencies on another modules and create logic blocks which works together.

Developing modular applications with *Akkamo* is piece of cake for the developer. The whole platform is designed to be as simple as possible, yet powerful, and uses extensively existing tools well-known for developers, such as [SBT](http://www.scala-sbt.org) for dependency and build management, Java's [Service loader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism for module discovery on classpath during *Akkamo* startup, and the [Lightbend Config](https://github.com/typesafehub/config) library for application-wide configuration. Also, *Akkamo*'s SBT plugin provides hot-reload functionality on code change, so developing the application is even easier and faster.

When your application is ready for production deployment, the SBT plugin [sbt-native-packager](https://github.com/sbt/sbt-native-packager) allows you to build your application as a native package (zip file with startup scripts).
