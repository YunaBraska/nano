[Home](../../README.md)
| [Context](../context/README.md)
| [Events](../events/README.md)
| [**> Logger <**](README.md)
| [Schedulers](../schedulers/README.md)
| [Services](../services/README.md)

# Logger

The [Logger](../logger/README.md) is a simple wrapper around the build in java logger which comes with predefined log formats `console`
and `json`.

## Placeholder

The logger supports placeholders in the message string. The placeholders are replaced by the arguments passed to the
logger.

* `{}` and `%s` is replaced by the argument at the same index
* `{0}` is replaced by the argument at the specified index

## Log Formatter

The [Logger](../logger/README.md) supports two log formatters at default:

* `console` - The console formatter logs the message to the console.
    * Example: `context.logger(() -> "Hello {}", "World")` will
      log: `[2024-11-11 11:11:11.111] [DEBUG] [Nano] - Hello World`
* `json` - The json formatter logs the message as json to the console.
    * Example: `context.logger(() -> "Hello {}", "World")` will
      log: `{"Hello":"World", "level":"DEBUG","logger":"Nano","message":"Hello World","timestamp":"2024-11-11 11:11:11.111"}`

## Custom Log Formatter

Custom log formatters can be registered by using `LogFormatRegister.registerLogFormatter(Name, Formatter)` - (
java.util.logging.Formatter)

## Log Queue

The [Logger](../logger/README.md) supports a `LogQueue` which can be used to not block the main thread when logging.
Nano comes with a default `LogQueue` Service which can be added as any other services like: `new Nano(new LogQueue())`

```mermaid
flowchart TD
    logger(((Logger))) --> javaLogger[JavaLogger]
    logger --> logQueue[LogQueue]
    logQueue --> javaLogger
    
    click context "docs/context/README.md" "Context"
    click logger "docs/logger/README.md" "Logger"
    click services "docs/services/README.md" "Services"
    click events "docs/events/README.md" "Events"
    click schedulers "docs/schedulers/README.md" "Schedulers"
    
    style logger fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style javaLogger fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
    style logQueue fill:#90CAF9,stroke:#1565C0,stroke-width:1px,color:#1A237E,rx:2%,ry:2%
```