package de.yuna.berlin.nativeapp.helper.event.model;

import de.yuna.berlin.nativeapp.services.http.HttpService;
import de.yuna.berlin.nativeapp.services.metric.logic.MetricService;
import de.yuna.berlin.nativeapp.services.metric.model.MetricUpdate;

import static de.yuna.berlin.nativeapp.helper.event.EventTypeRegister.registerEventType;

/**
 * Event types that can be used for communicating between various components in the system.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public enum EventType {

    EVENT_APP_START(registerEventType("APP_START"), "Triggered when the application is started"),
    EVENT_APP_SHUTDOWN(registerEventType("APP_SHUTDOWN"), "Triggered when the application is about to shut down"),
    EVENT_APP_LOG_LEVEL(registerEventType("APP_LOGLEVEL"), "Used to set the log level for the application"),
    EVENT_APP_LOG_QUEUE(registerEventType("APP_LOG_QUEUE_EVENT"), "Event to handle asynchronous logging"),
    EVENT_APP_LOG_FORMATTER(registerEventType("APP_LOG_FORMATTER_EVENT"), "Event to set the log formatter"),
    EVENT_APP_SERVICE_REGISTER(registerEventType("APP_SERVICE_REGISTER"), "Triggered when a new service is registered"),
    EVENT_APP_SERVICE_UNREGISTER(registerEventType("APP_SERVICE_UNREGISTER"), "Triggered when a service is unregistered"),
    EVENT_APP_SCHEDULER_REGISTER(registerEventType("APP_SCHEDULER_REGISTER"), "Triggered when a new scheduler is registered"),
    EVENT_APP_SCHEDULER_UNREGISTER(registerEventType("APP_SCHEDULER_UNREGISTER"), "Triggered when a scheduler is unregistered"),
    EVENT_APP_UNHANDLED(registerEventType("EVENT_APP_UNHANDLED"), "Triggered when there is an unhandled event"),
    EVENT_APP_HEARTBEAT(registerEventType("EVENT_HEARTBEAT"), "Triggered periodically"),
    EVENT_METRIC_UPDATE(registerEventType("EVENT_METRIC_UPDATE"), MetricService.class.getSimpleName() + " updates metrics, use " + MetricUpdate.class.getSimpleName()),
    EVENT_HTTP_REQUEST(registerEventType("HTTP_REQUEST"), "Http request usually comes from " + HttpService.class.getSimpleName()),
    EVENT_HTTP_REQUEST_UNHANDLED(registerEventType("HTTP_REQUEST_UNHANDLED"), "Http request which was not handled by any listener or service. Usually comes from " + HttpService.class.getSimpleName()),
    ;

    private final int id;
    private final String description;

    EventType(final int id, final String description) {
        this.id = id;
        this.description = description;
    }

    public int id() {
        return id;
    }

    public String description() {
        return description;
    }
}
