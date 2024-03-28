package de.yuna.berlin.nativeapp.helper.event.model;

import static de.yuna.berlin.nativeapp.helper.event.EventTypeRegister.registerEventType;

/**
 * Event types that can be used for communicating between various components in the system.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class EventType {

    // Triggered when the application is started
    public static final int EVENT_APP_START = registerEventType("APP_START");

    // Triggered when the application is about to shut down
    public static final int EVENT_APP_SHUTDOWN = registerEventType("APP_SHUTDOWN");

    // Used to set the log level for the application
    public static final int EVENT_APP_LOG_LEVEL = registerEventType("APP_LOGLEVEL");

    // Event to handle asynchronous logging
    public static final int EVENT_APP_LOG_QUEUE = registerEventType("APP_LOG_QUEUE_EVENT");

    // Event to set the log formatter
    public static final int EVENT_APP_LOG_FORMATTER = registerEventType("APP_LOG_FORMATTER_EVENT");

    // Triggered when a new service is registered
    public static final int EVENT_APP_SERVICE_REGISTER = registerEventType("APP_SERVICE_REGISTER");

    // Triggered when a service is unregistered
    public static final int EVENT_APP_SERVICE_UNREGISTER = registerEventType("APP_SERVICE_UNREGISTER");

    // Triggered when a new scheduler is registered
    public static final int EVENT_APP_SCHEDULER_REGISTER = registerEventType("APP_SCHEDULER_REGISTER");

    // Triggered when a scheduler is unregistered
    public static final int EVENT_APP_SCHEDULER_UNREGISTER = registerEventType("APP_SCHEDULER_UNREGISTER");

    // Triggered when there is an unhandled event
    public static final int EVENT_APP_UNHANDLED = registerEventType("EVENT_APP_UNHANDLED");

    // Triggered periodically
    public static final int EVENT_APP_HEARTBEAT = registerEventType("EVENT_HEARTBEAT");

    // MetricService updates metrics, use MetricUpdate
    public static final int EVENT_METRIC_UPDATE = registerEventType("EVENT_METRIC_UPDATE");

    // Http request usually comes from HttpService
    public static final int EVENT_HTTP_REQUEST = registerEventType("HTTP_REQUEST");

    // Http request which was not handled by any listener or service. Usually comes from HttpService
    public static final int EVENT_HTTP_REQUEST_UNHANDLED = registerEventType("HTTP_REQUEST_UNHANDLED");

    private EventType() {
        // static util class
    }
}
