package berlin.yuna.nano.core.model;

import berlin.yuna.nano.core.NanoServices;
import berlin.yuna.nano.core.NanoThreads;
import berlin.yuna.nano.helper.logger.LogFormatRegister;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.logic.HttpClient;

/**
 * Configuration keys enumeration to control various aspects of the NanoThreads framework.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum Config {

    APP_HELP("help", "Lists available config keys (see " + Config.class.getSimpleName() + ")"),
    APP_PARAMS("app_params_print", "Pints all config values"),
    CONFIG_APP_NAME("app_name", "Changes the name in the log output"), // TODO: implement this
    CONFIG_PROFILES("app_profiles", "Active config profiles for the application"),
    CONFIG_LOG_LEVEL("app_log_level", "Log level for the application (see " + LogLevel.class.getSimpleName() + ")"),
    CONFIG_LOG_FORMATTER("app_log_formatter", "Log formatter (see " + LogFormatRegister.class.getSimpleName() + ")"),
    CONFIG_LOG_QUEUE_SIZE("app_log_queue_size", "Log queue size. A full queue means that log messages will start to wait to be executed (see " + LogQueue.class.getSimpleName() + ")"),
    CONFIG_THREAD_POOL_TIMEOUT_MS("app_thread_pool_shutdown_timeout_ms", "Timeout for thread pool shutdown in milliseconds (see " + NanoThreads.class.getSimpleName() + ")"),
    CONFIG_PARALLEL_SHUTDOWN("app_service_shutdown_parallel", "Enable or disable parallel service shutdown (see " + NanoServices.class.getSimpleName() + "). Enabled = Can increase the shutdown performance on`true`"),

    // ########## DEFAULT SERVICES ##########
    CONFIG_SERVICE_HTTP_PORT("app_service_http_port", "Default port for the HTTP service (see " + HttpService.class.getSimpleName() + ")"),
    CONFIG_HTTP_CLIENT_VERSION("app_service_http_version", "HTTP client version 1 or 2 (see " + HttpClient.class.getSimpleName() + ")"),
    CONFIG_HTTP_CLIENT_MAX_RETRIES("app_service_http_max_retries", "Maximum number of retries for the HTTP client (see " + HttpClient.class.getSimpleName() + ")"),
    CONFIG_HTTP_CLIENT_CON_TIMEOUT_MS("app_service_http_con_timeoutMs", "Connection timeout in milliseconds for the HTTP client (see " + HttpClient.class.getSimpleName() + ")"),
    CONFIG_HTTP_CLIENT_READ_TIMEOUT_MS("app_service_http_read_timeoutMs", "Read timeout in milliseconds for the HTTP client (see " + HttpClient.class.getSimpleName() + ")"),
    CONFIG_HTTP_CLIENT_FOLLOW_REDIRECTS("app_service_http_follow_redirects", "Follow redirects for the HTTP client (see " + HttpClient.class.getSimpleName() + ")"),

    // ########## METRIC SERVICES ##########
    CONFIG_METRIC_SERVICE_BASE_PATH("metrics_base_url", "Base path for the metric service"),
    CONFIG_METRIC_SERVICE_PROMETHEUS_PATH("prometheus_metrics_url", "Prometheus path for the metric service"),
    CONFIG_METRIC_SERVICE_INFLUX_PATH("influx_metrics_url", "Influx path for the metric service"),
    CONFIG_METRIC_SERVICE_WAVEFRONT_PATH("wavefront_metrics_url", "Wavefront path for the metric service"),
    CONFIG_METRIC_SERVICE_DYNAMO_PATH("dynamo_metrics_url", "Dynamo path for the metric service");

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

    @Override
    public String toString() {
        return id;
    }
}
