# Akkamo configuration

The *Akkamo* platform itself and all bundled modules are configured using the
[Typesafe Config](https://github.com/typesafehub/config) library. The *Akkamo* platform and all
provided bundled modules uses the `akkamo` global namespace for its configuration. Below is the list
of all configuration options, available for *Akkamo* platform itself. For configuration details
about individual bundled modules, see the specific page in [Core modules](modules/index.md) section.

## Akkamo configuration options

- `akkamo.ctx.strict` *(optional, default value: `false`)* - As the *Akkamo Context* is immutable
  and each its modification (e.g. registering a service) yields new instance, the developer must
  be aware of this behaviour and return the very last instance of the context at the end of
  `Runnable#run` and `Initializable#initialize` methods. Since version `1.1.0`, *Akkamo* tracks all
  changes of *Akkamo Context* and warns the developer if not the last context is returned
  (see [Issue #15](https://github.com/akkamo/akkamo/issues/15)). By enabling this option, exception
  is thrown instead of log warning.
- `akkamo.verbose` *(optional, default value: `true`)* - Enables more verbose logging.