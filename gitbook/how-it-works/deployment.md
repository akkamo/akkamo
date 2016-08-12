# Akkamo Deployment

Akkamo flexible deployment option is one of Akkamo core features. Any module can be deployed from
simplest single node model up-to multiple modules on large Akka cluster. All deployment features
known from Akka applies here and additionally dependency management is introduced by Akkamo. There
can be multiple sbt build scripts building your application from single or multiple modules.
 

*An application can be built from one module, one module with dependent modules or multiple modules
in one Akka app.*


Each module may have its own Akka system. The system can be configured as known from Akka. In larger
installation the Akka system can be assigned to a specific cluster node using Akka roles.
 

Akkamo modules enables 100% code reuse regardless the deployment model. Even more multiple modules
can share code using dependent modules and make you application much more simpler. Multiple business
modules can also share connection to external resources like database. 
 

And a result for the deployment is always one Akka app that you Akka mates will know already. 


## One root Akkamo module on one Akka node

This deployment of one module to one server is typically used in development. Developer works on a
single module and deploys on local machine for testing. The developer can use `build.sbt` script for
deployment modules required for the development. `application.conf` file is part of the project on
classpath. The module can have dependencies to other modules described in build script. 
   
{%plantuml%}
node AkkaNode  {
  agent AkkamoModule
}
{%endplantuml%}

Or Akkamo module together with dependent modules on one Akka node
 
{%plantuml%}
node AkkaNode {
  agent RootModule
  agent DependentModule1
  agent DependentModule2
  RootModule -- DependentModule1
  RootModule -- DependentModule2
}
{%endplantuml%}
 
  
## More root Akkamo modules on one server

The deployment of more modules to one server shows value of Akkamo. By creating a build script with
multiple modules, the script can create one Akka jar application that can run. A deployer is
flexible in number of modules to be deployed on Akka node. 

Additionally multiple nodes can share resources like connection to database.

{%plantuml%}
artifact AkkaCluster {
node AkkaNodeA {
  agent RootModuleA1
  agent DependentModuleA1
  agent DependentModuleA2
  RootModuleA1 -- DependentModuleA1
  RootModuleA1 -- DependentModuleA2
}

node AkkaNodeB {
  agent RootModuleB1
  agent DependentModuleB1
  agent DependentModuleB2
  RootModuleB1 -- DependentModuleB1
  RootModuleB1 -- DependentModuleB2
}
}

{%endplantuml%}

## More root Akkamo modules on Akka cluster

The deployment of multiple modules into Akka cluster is what a final deployment model of Akkamo
modules should be. Multiple modules share connections to external services like database, Kafka etc.
Every module may have its own Akka system and can be configured in cluster to get sufficient
resources to work properly. Every module can be also assigned to a specific group of nodes in
cluster as Akka allows nodes to have roles. And nodes in roles can run named Akka system, i.e.
Akkamo modules.
 
{%plantuml%}
artifact AkkaCluster {
node AkkaNodeA {
  agent RootNodeA1
  agent RootNodeA2
  agent DependentNodeA1
  agent DependentNodeA2
  RootNodeA1 -- DependentNodeA1
  RootNodeA1 -- DependentNodeA2
  RootNodeA2 -- DependentNodeA1
  RootNodeA2 -- DependentNodeA2
}

node AkkaNodeB {
  agent RootNodeB1
  agent RootNodeB2
  agent DependentNodeB1
  agent DependentNodeB2
  RootNodeB1 -- DependentNodeB1
  RootNodeB1 -- DependentNodeB2
  RootNodeB2 -- DependentNodeB1
  RootNodeB2 -- DependentNodeB2
}
}
{%endplantuml%}