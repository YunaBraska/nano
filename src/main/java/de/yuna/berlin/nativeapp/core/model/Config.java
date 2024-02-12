package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.core.NanoServices;
import de.yuna.berlin.nativeapp.core.NanoThreads;
import de.yuna.berlin.nativeapp.helper.logger.LogFormatRegister;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

/**
 * Configuration keys enumeration to control various aspects of the NanoThreads framework.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum Config {

    APP_HELP("help", "Lists available config keys (see " + Config.class.getSimpleName() + ")"),
    APP_PARAMS("app_params_print", "Pints all config values"),
    CONFIG_APP_NAME("app_name", "Changes the name in the log output"), // TODO: implement this
    CONFIG_LOG_LEVEL("app_log_level", "Log level for the application (see " + LogLevel.class.getSimpleName() + ")"),
    CONFIG_LOG_FORMATTER("app_log_formatter", "Log formatter (see " + LogFormatRegister.class.getSimpleName() + ")"),
    CONFIG_LOG_QUEUE_SIZE("app_log_queue_size", "Log queue size. A full queue means that log messages will start to wait to be executed (see " + LogQueue.class.getSimpleName() + ")"),
    CONFIG_THREAD_POOL_TIMEOUT_MS("app_thread_pool_shutdown_timeout_ms", "Timeout for thread pool shutdown in milliseconds (see " + NanoThreads.class.getSimpleName() + ")"),
    CONFIG_PARALLEL_SHUTDOWN("app_service_shutdown_parallel", "Enable or disable parallel service shutdown (see " + NanoServices.class.getSimpleName() + "). Enabled = Can increase the shutdown performance on`true`"),
    CONFIG_CONTEXT_CONCURRENT("app_context_concurrent", "Enable or disable concurrent `" + Context.class.getSimpleName() + "`Enabled = multiple `" + Service.class.getSimpleName() + "s` & Listeners write on the same to the same `" + Context.class.getSimpleName() + "`. Disabled = increases performance."),
    ;

    private final String id;
    private final String description;

    Config(final String id, final String description) {
        this.id = id;
        this.description = description;
    }

    public String id() {
        return id;
    }

    public String description() {
        return description;
    }
}
