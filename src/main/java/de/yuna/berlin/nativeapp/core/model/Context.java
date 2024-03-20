package de.yuna.berlin.nativeapp.core.model;

import berlin.yuna.typemap.model.ConcurrentTypeMap;
import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.helper.ExRunnable;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.logic.NanoLogger;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.yuna.berlin.nativeapp.core.model.NanoThread.waitFor;
import static de.yuna.berlin.nativeapp.core.model.Service.threadsOf;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static java.util.Arrays.stream;

@SuppressWarnings({"unused", "UnusedReturnValue", "java:S2160"})
public class Context extends ConcurrentTypeMap {

    public static final String CONTEXT_TRACE_ID_KEY = "app_core_context_trace_id";
    public static final String CONTEXT_LOGGER_KEY = "app_core_context_logger";
    public static final String EVENT_PAYLOAD = "app_event_payload";
    public static final String EVENT_RESPONSE = "app_event_response";

    private final transient Nano nano;

    public static Context createRootContext() {
        return new Context();
    }

    public static List<Object> newTraceId(final Collection<Object> traceIds, final Class<?> clazz) {
        final List<Object> result = traceIds != null ? new ArrayList<>(traceIds) : new ArrayList<>();
        result.add((clazz != null ? clazz.getSimpleName() : "Root" + Context.class.getSimpleName()) + "/" + UUID.randomUUID().toString().replace("-", ""));
        return result;
    }

    public Nano nano() {
        return nano;
    }

    public String traceId() {
        final List<String> list = getList(CONTEXT_TRACE_ID_KEY, String.class);
        return list.getLast();
    }

    public String traceId(final int index) {
        final List<String> list = getList(CONTEXT_TRACE_ID_KEY, String.class);
        return index > -1 && index < list.size() ? list.get(index) : list.getLast();
    }

    public List<String> traceIds() {
        return getList(CONTEXT_TRACE_ID_KEY, String.class);
    }

    protected IllegalArgumentException noKeyException(final String key, final String typeName) {
        return new IllegalArgumentException(Context.class.getSimpleName() + " does not contain [" + key + "] of type [" + typeName + "]");
    }

    public NanoLogger logger() {
        return getOpt(NanoLogger.class, CONTEXT_LOGGER_KEY)
            .orElseGet(() -> setLogger(Context.class).get(NanoLogger.class, CONTEXT_LOGGER_KEY).warn(() -> "Fallback to generic logger used. It is recommended to provide a context-specific logger for improved traceability and context-aware logging. A context-specific logger allows for more granular control over logging behaviors, including level filtering, log format customization, and targeted log output, which enhances the debugging and monitoring capabilities. Using a generic logger might result in less optimal logging granularity and difficulty in tracing issues related to specific contexts.", new IllegalStateException("Context-specific logger not provided. Falling back to a generic logger.")));
    }

    public Context copy(final Class<?> clazz, final Nano nano) {
        return clazz != null ? new Context(this, getNano(nano), clazz).setLogger(clazz) : new Context(this, getNano(nano), null);
    }

    public LogLevel logLevel() {
        return logger().level();
    }

    //########## CHAINING HELPERS ##########

    public Context addEventListener(final int eventType, final Consumer<Event> listener) {
        nano.addEventListener(eventType, listener);
        return this;
    }

    public Context removeEventListener(final int eventType, final Consumer<Event> listener) {
        nano.removeEventListener(eventType, listener);
        return this;
    }

    @Override
    public Context put(final Object key, final Object value) {
        // ConcurrentHashMap does not allow null keys or values.
        super.put(key, value != null ? value : "");
        return this;
    }

    //########## LOGGING HELPERS ##########

    public Context setLogger(final Class<?> clazz) {
        final NanoLogger coreLogger = nano().logger();
        final NanoLogger logger = new NanoLogger(clazz);
        logger.level(coreLogger.level()).logQueue(coreLogger.logQueue()).formatter(coreLogger.formatter());
        put(CONTEXT_LOGGER_KEY, logger);
        return this;
    }

    public NanoLogger setLoggerReturn(final Class<?> clazz) {
        final NanoLogger coreLogger = nano().logger();
        final NanoLogger logger = new NanoLogger(clazz);
        logger.level(coreLogger.level()).logQueue(coreLogger.logQueue()).formatter(coreLogger.formatter());
        put(CONTEXT_LOGGER_KEY, logger);
        return logger;
    }

    //########## ASYNC HELPERS ##########

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param runnable function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    @SafeVarargs
    public final Context async(final Consumer<Context>... runnable) {
        asyncReturn(runnable);
        return this;
    }

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    @SafeVarargs
    public final Context asyncHandled(final Consumer<Unhandled> onFailure, final Consumer<Context>... runnable) {
        asyncReturnHandled(onFailure, runnable);
        return this;
    }

    /**
     * Executes one or multiple {@link Service} asynchronously.
     *
     * @param services The {@link Service} to be appended.
     * @return The {@link Context} object for chaining further operations.
     */
    public Context async(final Service... services) {
        asyncReturn(services);
        return this;
    }

    //########## ASYNC RETURN HELPER ##########

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param runnable function to execute.
     * @return {@link NanoThread}s
     */
    @SafeVarargs
    public final NanoThread[] asyncReturn(final Consumer<Context>... runnable) {
        return stream(runnable).map(task -> new NanoThread(this).execute(() -> task.accept(this))).toArray(NanoThread[]::new);
    }

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return {@link NanoThread}s
     */
    @SafeVarargs
    public final NanoThread[] asyncReturnHandled(final Consumer<Unhandled> onFailure, final Consumer<Context>... runnable) {
        return stream(runnable).map(task -> new NanoThread(this)
            .onComplete((thread, error) -> {
                if (error != null)
                    onFailure.accept(new Unhandled(this, thread, error));
            })
            .execute(() -> task.accept(this))
        ).toArray(NanoThread[]::new);
    }

    /**
     * Executes one or multiple {@link Service} asynchronously.
     *
     * @param services The {@link Service} to be appended.
     * @return {@link NanoThread}s
     */
    public NanoThread[] asyncReturn(final Service... services) {
        try {
            return threadsOf(this, services);
        } catch (final Exception exception) {
            handleExecutionExceptions(this, new Unhandled(this, services.length == 1 ? services[0] : services, exception), () -> "Error while executing [" + stream(services).map(Service::name).distinct().collect(Collectors.joining()) + "]");
            Thread.currentThread().interrupt();
            return new NanoThread[0];
        }
    }

    //########## ASYNC AWAIT HELPER ##########

    /**
     * Executes and waits for all runnable to be ready
     *
     * @param runnable function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    @SafeVarargs
    public final Context asyncAwait(final Consumer<Context>... runnable) {
        waitFor(asyncReturn(runnable));
        return this;
    }

    /**
     * Executes and waits for all runnable to be ready
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    @SafeVarargs
    public final Context asyncAwaitHandled(final Consumer<Unhandled> onFailure, final Consumer<Context>... runnable) {
        waitFor(asyncReturnHandled(onFailure, runnable));
        return this;
    }

    /**
     * Executes and waits for all {@link Service} to be ready
     *
     * @return The {@link Context} object for chaining further operations.
     */
    public Context asyncAwait(final Service... services) {
        asyncAwaitReturn(services);
        return this;
    }

    //########## ASYNC AWAIT HELPER RETURN ##########

    /**
     * Executes and waits for all {@link Service} to be ready
     *
     * @param timeoutMs optional timeout for the current function. '-1' means use default
     * @param runnable  function to execute.
     * @return {@link NanoThread}s
     */
    @SafeVarargs
    public final NanoThread[] asyncAwaitReturn(final long timeoutMs, final Consumer<Context>... runnable) {
        return waitFor(asyncReturn(runnable));
    }

    /**
     * Executes and waits for all {@link Service} to be ready
     *
     * @param onFailure function to execute on failure
     * @param timeoutMs optional timeout for the current function. '-1' means use default
     * @param runnable  function to execute.
     * @return {@link NanoThread}s
     */
    @SafeVarargs
    public final NanoThread[] asyncAwaitReturnHandled(final Consumer<Unhandled> onFailure, final long timeoutMs, final Consumer<Context>... runnable) {
        return waitFor(asyncReturnHandled(onFailure, runnable));
    }

    /**
     * Executes and waits for all {@link Service} to be ready
     *
     * @return {@link NanoThread}s
     */
    public NanoThread[] asyncAwaitReturn(final Service... services) {
        return waitFor(asyncReturn(services));
    }

    //########## EVENT HELPER ##########

    /**
     * Sends an event of the specified type with the provided payload within this context without expecting a response.
     * This method is used for sending targeted events that do not require asynchronous processing or response handling.
     *
     * @param eventType The integer representing the type of the event, identifying the nature or action of the event.
     * @param payload   The payload of the event, containing data relevant to the event's context and purpose.
     * @return The current {@link Context} instance, allowing for method chaining and further configuration.
     */
    public Context sendEvent(final int eventType, final Object payload) {
        nano.sendEvent(eventType, this, payload, null, false);
        return this;
    }

    /**
     * Sends an event of the specified type with the provided payload within this context, expecting a response that is handled by the provided responseListener.
     * This method allows for asynchronous event processing and response handling through the specified consumer.
     *
     * @param eventType        The integer representing the type of the event.
     * @param payload          The payload of the event, containing the data to be communicated.
     * @param responseListener A consumer that processes the response of the event. This allows for asynchronous event handling and response processing.
     * @return The current {@link Context} instance, facilitating method chaining and further actions.
     */
    public Context sendEvent(final int eventType, final Object payload, final Consumer<Object> responseListener) {
        nano.sendEvent(eventType, this, payload, responseListener, false);
        return this;
    }

    /**
     * Broadcasts an event of the specified type with the provided payload to all listeners within this context without expecting a response.
     * This method is ideal for notifying all interested parties of a particular event where no direct response is required.
     *
     * @param eventType The integer representing the type of the event, used to notify all listeners interested in this type of event.
     * @param payload   The payload of the event, containing information relevant to the broadcast.
     * @return The current {@link Context} instance, enabling method chaining and additional configurations.
     */
    public Context broadcastEvent(final int eventType, final Object payload) {
        nano.sendEvent(eventType, this, payload, null, true);
        return this;
    }

    /**
     * Broadcasts an event of the specified type with the provided payload to all listeners within this context, expecting a response that is handled by the provided responseListener.
     * This method allows for the broad dissemination of an event while also facilitating asynchronous response processing.
     *
     * @param eventType        The integer representing the type of the event.
     * @param payload          The payload associated with the event, intended for widespread distribution.
     * @param responseListener A consumer that handles the response of the event, enabling asynchronous processing and response handling across multiple listeners.
     * @return The current {@link Context} instance, allowing for method chaining and further actions.
     */
    public Context broadcastEvent(final int eventType, final Object payload, final Consumer<Object> responseListener) {
        nano.sendEvent(eventType, this, payload, responseListener, true);
        return this;
    }

    //########## EVENT RETURN HELPER ##########

    /**
     * Sends an event of the specified type with the provided payload within this context without expecting a response.
     * This method is used for sending targeted events that do not require asynchronous processing or response handling.
     *
     * @param eventType The integer representing the type of the event, identifying the nature or action of the event.
     * @param payload   The payload of the event, containing data relevant to the event's context and purpose.
     * @return An instance of {@link Event} that represents the event being processed. This object can be used for further operations or tracking.
     */
    public Event sendEventReturn(final int eventType, final Object payload) {
        return nano.sendEventReturn(eventType, this, payload, null, false);
    }

    /**
     * Sends an event of the specified type with the provided payload within this context, expecting a response that is handled by the provided responseListener.
     * This method allows for asynchronous event processing and response handling through the specified consumer.
     *
     * @param eventType        The integer representing the type of the event.
     * @param payload          The payload of the event, containing the data to be communicated.
     * @param responseListener A consumer that processes the response of the event. This allows for asynchronous event handling and response processing.
     * @return An instance of {@link Event} that represents the event being processed. This object can be used for further operations or tracking.
     */
    public Event sendEventReturn(final int eventType, final Object payload, final Consumer<Object> responseListener) {
        return nano.sendEventReturn(eventType, this, payload, responseListener, false);
    }

    /**
     * Broadcasts an event of the specified type with the provided payload to all listeners within this context without expecting a response.
     * This method is ideal for notifying all interested parties of a particular event where no direct response is required.
     *
     * @param eventType The integer representing the type of the event, used to notify all listeners interested in this type of event.
     * @param payload   The payload of the event, containing information relevant to the broadcast.
     * @return An instance of {@link Event} that represents the event being processed. This object can be used for further operations or tracking.
     */
    public Event broadcastEventReturn(final int eventType, final Object payload) {
        return nano.sendEventReturn(eventType, this, payload, null, true);
    }

    /**
     * Broadcasts an event of the specified type with the provided payload to all listeners within this context, expecting a response that is handled by the provided responseListener.
     * This method allows for the broad dissemination of an event while also facilitating asynchronous response processing.
     *
     * @param eventType        The integer representing the type of the event.
     * @param payload          The payload associated with the event, intended for widespread distribution.
     * @param responseListener A consumer that handles the response of the event, enabling asynchronous processing and response handling across multiple listeners.
     * @return An instance of {@link Event} that represents the event being processed. This object can be used for further operations or tracking.
     */
    public Event broadcastEventReturn(final int eventType, final Object payload, final Consumer<Object> responseListener) {
        return nano.sendEventReturn(eventType, this, payload, responseListener, true);
    }

    protected Context() {
        nano = null;
        this.put(CONTEXT_TRACE_ID_KEY, newTraceId(null, null));
    }

    protected Context(final Map<?, ?> map, final Nano nano, final Class<?> clazz) {
        super(map);
        this.put(CONTEXT_TRACE_ID_KEY, newTraceId(getList(CONTEXT_TRACE_ID_KEY, Object.class), clazz));
        this.nano = nano;
    }

    protected Nano getNano(final Nano nano) {
        return nano != null ? nano : this.nano();
    }

    public static void handleExecutionExceptions(final Context context, final Unhandled payload, final Supplier<String> errorMsg) {
        final AtomicBoolean wasHandled = new AtomicBoolean(false);
        context.nano().sendEvent(EVENT_APP_UNHANDLED.id(), context, payload, result -> wasHandled.set(true), false);
        if (!wasHandled.get()) {
            context.logger().error(payload.exception(), errorMsg);
        }
    }

    public static void tryExecute(final ExRunnable operation) {
        tryExecute(operation, null);
    }

    public static void tryExecute(final ExRunnable operation, final Consumer<Throwable> consumer) {
        try {
            operation.run();
        } catch (final Exception exception) {
            if (consumer != null) {
                consumer.accept(exception);
            }
        }
    }

    @Override
    public String toString() {
        return "Context{" +
            "size=" + size() +
            ", loglevel=" + getOpt(NanoLogger.class, CONTEXT_LOGGER_KEY).map(NanoLogger::level).orElse(null) +
            ", logQueue=" + getOpt(NanoLogger.class, CONTEXT_LOGGER_KEY).map(NanoLogger::logQueue).isPresent() +
            '}';
    }
}
