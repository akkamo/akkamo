# Akkamo Module

*Akkamo* module is the cornerstone of each modular application based on this platform. Module is nothing more than an old-good Scala class, implementing appropriate *traits* and their methods.
Each module is represented by the class implementing at least `eu.akkamo.Module` trait. For module lookup during _Akkamo_ startup, Java's [ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) mechanism is used, therefore for each _Akkamo_ module a new line with _fully qualified domain name_ of module's class must be added to the `eu.akkamo.Module` file located in the `META-INF/services/` directory of module's JAR file.
