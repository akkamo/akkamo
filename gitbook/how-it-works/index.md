# How it works?

Each *Akkamo* application is in fact a buch of *Akkamo* modules, glued together by the *Akkamo* platform itself. The platform itself is the heart of the application, providing and ensuring fundamental functionality for the following areas:

* ensures proper modules discovery and initialization during application startup
* solves dependencies of the individual modules and satisfies them
* executes corresponding stages of modules' lifecycle

In the following chapters individual important topics are described more thoroughly:

* Chapter [Akkamo Lifecycle](lifecycle.md) describes the entire *Akkamo* application life, from *Akkamo*'s bootstrap and module discovery, to application shutdown.

* Chapter [Akkamo Module](module.md) describes what is *Akkamo* module, how to create and configure new module, how to use other module's functionality or provide own functionality for other modules.

* Chapter [Akkamo Context](context.md) describes *Akkamo* context, the centerpiece, where modules can register own functionality for other modules or access functionality provided by other modules.
