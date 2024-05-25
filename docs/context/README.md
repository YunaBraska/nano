[Home](../../README.md) 
| [**> Context <**](README.md)
| [Events](../events/README.md)
| [Logger](../logger/README.md)
| [Schedulers](../schedulers/README.md)
| [Services](../services/README.md) 

# Context

The context object is the main object that is passed around in Nano.
It is used to interact with the [Logger](../logger/README.md), [Events](../events/README.md) , [Services](../services/README.md) and [Schedulers](../schedulers/README.md), traces and
configuration.
There is **no need to create any custom config** class as the context contains all needed information including type conversion.

### Examples

#### Configuration

* `context.get(Integer.class, "app_config_key")` - Get a configuration value as an Integer
* `context.getList(Integer.class, "app_config_key")` - Get a configuration value as a List of Integers
* `context.getMap(String.class, Integer.class, "app_config_key")` - Get a configuration value as a Map of Strings to
  Integers
* `context.traceId()` - Get the trace id of the current context
* `context.logLevel()` - Get the log level of the current context
* `context.logger.info(() -> "Hello {}", "World")` - Log a message with the [Logger](../logger/README.md) at the info level
* `context.newContext(MyClass.class)` - Create a new context with a [Logger](../logger/README.md) for the specific class

#### Events

* `context.registerChannelId("MyEventName")` - Register a new [Event](../events/README.md)  type and get the event id
* `context.sendEvent(channelId, MyPayloadObject)` - Send an [Event](../events/README.md)  with a payload
* `context.subscribeEvent(channelId, event -> System.out.println(event))` - Subscribe to an [Event](../events/README.md)  and execute the lambda
  when the event is triggered
* `context.broadcastEvent(channelId, MyPayloadObject)` - Broadcast an [Event](../events/README.md) with a payload
* `context.eventNameOf(channelId)` - Get the name of an [Event](../events/README.md) from the event id
* `context.channelIdOf(eventName)` - Get the id of an [Event](../events/README.md) from the event name

#### Executors

* `context.run(() -> System.out.println("Scheduled"), 128, 256, MILLISECONDS)` - Run a lambda on a [Schedulers](../schedulers/README.md) with a 128ms delay and 256ms period
* `context.run(() -> System.out.println("Async Task"))` - Run a lambda asynchronously
* `context.runAwait(() -> System.out.println("Task 1"), ) -> System.out.println("Task 2"))` - Run a lambda
  asynchronously and wait for them to finish

## Configuration

Available properties can be found by starting the application with the `--help` flag.

| Order | Type                   | Description & examples                                                                                                    |
|-------|------------------------|---------------------------------------------------------------------------------------------------------------------------|
| 0     | Property Files         | `application.properties` config files, which gets automatically loaded from `config`, `resources`, and `resources/config` |
| 1     | Environment Variables  | `export app_profiles=production` variables that are set in the environment                                                |
| 2     | Command Line Arguments | `-Dproperty=value` start parameters                                                                                       |
| 3     | Args                   | `app_profiles=production` arguments that are passed to the `main` method of the application                               |
| 4     | Defaults               | If no other configuration value is provided                                                                               |

### Configuration Profiles

Profiles are used to define different configurations for different environments.
They can be set by using the `app_profiles` property. But compatible with properties
like `spring.profiles.active`, `quarkus.profile`, `micronaut.profiles`, etc.
When using profiles, the corresponding `application-{profile}.properties` file will be loaded if present.

### Configuration format

You can use any configuration format which your system allows, Nano will convert every configuration key into the common
and widely accepted format which is lowercased with underscore separation. For example `app.profiles` will be converted
to `app_profiles`.

### Configuration naming patterns:

* `app_<key>` is reserved for Nano internal configurations
* `app_service_<servicename>_<key>` can be used for [Services](../services/README.md)
* `app_config_` is the prefix for custom configurations

### Configuration Variables

It's possible to use variables in the configuration files
Variables are defined in the format `${variableName}` or also `${variableName:fallback}`.

Example: `test.placeholder.value=${placeholder_value:fallback}`

### Default Configurations

| Config Name                         | Type    | Description                                                                                                                           |
|-------------------------------------|---------|---------------------------------------------------------------------------------------------------------------------------------------|
| help                                | Boolean | Lists available config keys without starting the application                                                                          |
| app_params_print                    | Boolean | Prints all configured values                                                                                                          |
| app_profiles                        | String  | ist of active config profiles for the application                                                                                     |
| app_log_level                       | String  | Log level for the application                                                                                                         |
| app_log_formatter                   | String  | Log formatter `console` or `json`                                                                                                     |
| app_log_queue_size                  | String  | Log queue size. A full queue means that log messages will start to wait to be executed - only available when using `LogQueue` Service |
| app_thread_pool_shutdown_timeout_ms | String  | Timeout for thread pool shutdown in milliseconds                                                                                      |
| app_service_shutdown_parallel       | Boolean | Enable or disable parallel service shutdown. Enabled = Can increase the shutdown performance                                          |


```mermaid
flowchart TD
    context(((Context))) --> nano[Nano]
    context --> logger[Logger]
    context --> events[Events]
    context --> services[Services]
    context --> schedulers[Services]
    
    click context "docs/context/README.md" "Context"
    click logger "docs/logger/README.md" "Logger"
    click services "docs/services/README.md" "Services"
    click events "docs/events/README.md" "Events"
    click schedulers "docs/schedulers/README.md" "Schedulers"
    
    style nano fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style context fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style logger fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style events fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style services fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style schedulers fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
```