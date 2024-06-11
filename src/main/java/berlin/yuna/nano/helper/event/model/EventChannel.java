package berlin.yuna.nano.helper.event.model;

import berlin.yuna.nano.helper.event.EventChannelRegister;

/**
 * Event types that can be used for communicating between various components in the system.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class EventChannel {

    // Triggered when the application is started
    public static final int EVENT_APP_START = EventChannelRegister.registerChannelId("APP_START");

    // Triggered when the application is about to shut down
    public static final int EVENT_APP_SHUTDOWN = EventChannelRegister.registerChannelId("APP_SHUTDOWN");

    // Used to set the log level for the application
    public static final int EVENT_APP_LOG_LEVEL = EventChannelRegister.registerChannelId("APP_LOGLEVEL");

    // Event to handle asynchronous logging
    public static final int EVENT_APP_LOG_QUEUE = EventChannelRegister.registerChannelId("APP_LOG_QUEUE_EVENT");

    // Event to set the log formatter
    public static final int EVENT_APP_LOG_FORMATTER = EventChannelRegister.registerChannelId("APP_LOG_FORMATTER_EVENT");

    // Triggered when a new service is registered
    public static final int EVENT_APP_SERVICE_REGISTER = EventChannelRegister.registerChannelId("APP_SERVICE_REGISTER");

    // Triggered when a service is unregistered
    public static final int EVENT_APP_SERVICE_UNREGISTER = EventChannelRegister.registerChannelId("APP_SERVICE_UNREGISTER");

    // Triggered when a new scheduler is registered
    public static final int EVENT_APP_SCHEDULER_REGISTER = EventChannelRegister.registerChannelId("APP_SCHEDULER_REGISTER");

    // Triggered when a scheduler is unregistered
    public static final int EVENT_APP_SCHEDULER_UNREGISTER = EventChannelRegister.registerChannelId("APP_SCHEDULER_UNREGISTER");

    // Triggered when there is an unhandled event
    public static final int EVENT_APP_UNHANDLED = EventChannelRegister.registerChannelId("EVENT_APP_UNHANDLED");

    // Triggered periodically
    public static final int EVENT_APP_HEARTBEAT = EventChannelRegister.registerChannelId("EVENT_HEARTBEAT");

    // MetricService updates metrics, use MetricUpdate
    public static final int EVENT_METRIC_UPDATE = EventChannelRegister.registerChannelId("EVENT_METRIC_UPDATE");

    // Http request usually comes from HttpService
    public static final int EVENT_HTTP_REQUEST = EventChannelRegister.registerChannelId("HTTP_REQUEST");

    // Http request which was not handled by any listener or service. Usually comes from HttpService
    public static final int EVENT_HTTP_REQUEST_UNHANDLED = EventChannelRegister.registerChannelId("HTTP_REQUEST_UNHANDLED");

    private EventChannel() {
        // static util class
    }
}
