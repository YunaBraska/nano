package berlin.yuna.nano.core.model;

import berlin.yuna.nano.helper.event.EventTypeRegister;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.typemap.model.ConcurrentTypeMap;
import berlin.yuna.typemap.model.TypeList;
import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.helper.ExRunnable;
import berlin.yuna.nano.helper.logger.logic.NanoLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static berlin.yuna.nano.core.model.Service.threadsOf;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

@SuppressWarnings({"unused", "UnusedReturnValue", "java:S2160"})
public class Context extends ConcurrentTypeMap {

    public static final String CONTEXT_TRACE_ID_KEY = "app_core_context_trace_id";
    public static final String CONTEXT_LOGGER_KEY = "app_core_context_logger";

    private final transient Nano nano;

    /**
     * Creates a new root context with a unique trace ID.
     *
     * @return The newly created root context.
     */
    public static Context createRootContext() {
        return new Context();
    }

    /**
     * Adds the trace IDs to the context. Creates new unique trace ID if empty or null.
     *
     * @param traceIds The trace IDs to add to the context.
     * @return The newly created root context.
     */
    public static List<Object> newTraceId(final Collection<Object> traceIds, final Class<?> clazz) {
        final List<Object> result = traceIds != null ? new TypeList(traceIds) : new TypeList();
        result.add((clazz != null ? clazz.getSimpleName() : "Root" + Context.class.getSimpleName()) + "/" + UUID.randomUUID().toString().replace("-", ""));
        return result;
    }

    /**
     * Retrieves the {@link Nano} instance associated with this context.
     *
     * @return The {@link Nano} instance associated with this context.
     */
    public Nano nano() {
        return nano;
    }

    /**
     * Retrieves the last created trace ID of the context.
     *
     * @return The last created trace ID of the context.
     */
    public String traceId() {
        final List<String> list = getList(CONTEXT_TRACE_ID_KEY, String.class);
        return list.getLast();
    }

    /**
     * Retrieves the trace ID at the specified index.
     *
     * @param index The index of the trace ID to retrieve.
     * @return The trace ID at the specified index, or the last trace ID if the index is out of bounds.
     */
    public String traceId(final int index) {
        final List<String> list = getList(CONTEXT_TRACE_ID_KEY, String.class);
        return index > -1 && index < list.size() ? list.get(index) : list.getLast();
    }

    /**
     * Retrieves all trace IDs associated with this context.
     *
     * @return A list of all trace IDs associated with this context.
     */
    public List<String> traceIds() {
        return getList(CONTEXT_TRACE_ID_KEY, String.class);
    }

    /**
     * Retrieves the logger associated with this context.
     *
     * @return The logger associated with this context.
     */
    public NanoLogger logger() {
        return getOpt(NanoLogger.class, CONTEXT_LOGGER_KEY)
            .orElseGet(() -> logger(Context.class).get(NanoLogger.class, CONTEXT_LOGGER_KEY).warn(() -> "Fallback to generic logger used. It is recommended to provide a context-specific logger for improved traceability and context-aware logging. A context-specific logger allows for more granular control over logging behaviors, including level filtering, log format customization, and targeted log output, which enhances the debugging and monitoring capabilities. Using a generic logger might result in less optimal logging granularity and difficulty in tracing issues related to specific contexts.", new IllegalStateException("Context-specific logger not provided. Falling back to a generic logger.")));
    }

    /**
     * Creates new Context with a new logger and trace ID with the given Nano instance.
     *
     * @param clazz The class to use for the logger name. If null, the logger name will be the class of the context.
     * @param nano  The nano instance to use for the context.
     * @return The newly created context.
     */
    public Context newContext(final Class<?> clazz, final Nano nano) {
        return clazz != null ? new Context(this, getNano(nano), clazz).logger(clazz) : new Context(this, getNano(nano), null);
    }

    /**
     * Creates new Context with a new logger and trace ID.
     *
     * @param clazz The class to use for the logger name. If null, the logger name will be the class of the context.
     * @return The newly created context.
     */
    public Context newContext(final Class<?> clazz) {
        return newContext(clazz, null);
    }

    /**
     * Creates new empty Context with a new logger and trace ID.
     *
     * @param clazz The class to use for the logger name. If null, the logger name will be the class of the context.
     * @return The newly created context.
     */
    public Context newEmptyContext(final Class<?> clazz) {
        return newEmptyContext(clazz, null);
    }

    /**
     * Creates new empty Context with a new logger and trace IDwith the given Nano instance.
     *
     * @param clazz The class to use for the logger name. If null, the logger name will be the class of the context.
     * @param nano  The nano instance to use for the context.
     * @return The newly created context.
     */
    public Context newEmptyContext(final Class<?> clazz, final Nano nano) {
        return clazz != null
            ? new Context(Map.of(CONTEXT_TRACE_ID_KEY, this.get(CONTEXT_TRACE_ID_KEY)), getNano(nano), clazz).logger(clazz)
            : new Context(Map.of(CONTEXT_TRACE_ID_KEY, this.get(CONTEXT_TRACE_ID_KEY)), getNano(nano), null);
    }

    /**
     * Retrieves the log level of the context logger.
     *
     * @return The log level of the context logger.
     */
    public LogLevel logLevel() {
        return logger().level();
    }

    //########## CHAINING HELPERS ##########

    /**
     * Puts a key-value pair into the context.
     *
     * @param key   The key to put into the context. Null keys are interpreted as empty strings.
     * @param value The value to associate with the key.
     * @return The current {@link Context} instance, allowing for method chaining and further configuration.
     */
    @Override
    public Context put(final Object key, final Object value) {
        // ConcurrentHashMap does not allow null keys or values.
        super.put(key, value != null ? value : "");
        return this;
    }

    /**
     * Registers an event listener for a specific event type.
     *
     * @param eventType The integer identifier of the event type.
     * @param listener  The consumer function that processes the {@link Event}.
     * @return Self for chaining
     */
    public Context subscribeEvent(final int eventType, final Consumer<Event> listener) {
        nano.subscribeEvent(eventType, listener);
        return this;
    }

    /**
     * Removes a registered event listener for a specific event type.
     *
     * @param eventType The integer identifier of the event type.
     * @param listener  The consumer function to be removed.
     * @return Self for chaining
     */
    public Context unsubscribeEvent(final int eventType, final Consumer<Event> listener) {
        nano.unsubscribeEvent(eventType, listener);
        return this;
    }

    /**
     * Executes a task asynchronously after a specified delay.
     *
     * @param task     The task to execute.
     * @param delay    The delay before executing the task.
     * @param timeUnit The time unit of the delay parameter.
     * @return Self for chaining
     */
    public Context run(final ExRunnable task, final long delay, final TimeUnit timeUnit) {
        nano.run(task, delay, timeUnit);
        return this;
    }

    /**
     * Executes a task periodically, starting after an initial delay.
     *
     * @param task   The task to execute.
     * @param delay  The initial delay before executing the task.
     * @param period The period between successive task executions.
     * @param unit   The time unit of the initialDelay and period parameters.
     * @param until  A BooleanSupplier indicating the termination condition. <code>true</code> stops the next execution.
     * @return Self for chaining
     */
    public Context run(final ExRunnable task, final long delay, final long period, final TimeUnit unit, final BooleanSupplier until) {
        nano.run(task, delay, period, unit, until);
        return this;
    }


    //########## LOGGING HELPERS ##########

    /**
     * Sets the logger name for the context logger.
     *
     * @param clazz The class to use for the logger name.
     * @return Self for chaining
     */
    public Context logger(final Class<?> clazz) {
        final NanoLogger logger = new NanoLogger(clazz);
        final NanoLogger coreLogger = nano == null ? logger : nano.logger();
        logger.level(coreLogger.level()).logQueue(coreLogger.logQueue()).formatter(coreLogger.formatter());
        put(CONTEXT_LOGGER_KEY, logger);
        return this;
    }

    /**
     * Sets the logger name for the context logger.
     *
     * @param clazz The class to use for the logger name.
     * @return The created {@link NanoLogger}
     */
    public NanoLogger loggerReturn(final Class<?> clazz) {
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
    public final Context run(final ExRunnable... runnable) {
        runReturn(runnable);
        return this;
    }

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    public final Context runHandled(final Consumer<Unhandled> onFailure, final ExRunnable... runnable) {
        runReturnHandled(onFailure, runnable);
        return this;
    }

    /**
     * Executes one or multiple {@link Service} asynchronously.
     *
     * @param services The {@link Service} to be appended.
     * @return The {@link Context} object for chaining further operations.
     */
    public Context run(final Service... services) {
        runReturn(services);
        return this;
    }

    //########## ASYNC RETURN HELPER ##########

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param runnable function to execute.
     * @return {@link NanoThread}s
     */
    public final NanoThread[] runReturn(final ExRunnable... runnable) {
        return stream(runnable).map(task -> new NanoThread(this).run(
            this.nano == null ? null : nano.threadPool(),
            () -> this.nano == null ? null : nano.newEmptyContext(this.getClass()),
            task
        )).toArray(NanoThread[]::new);
    }

    /**
     * Executes one or multiple runnable asynchronously.
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return {@link NanoThread}s
     */
    public final NanoThread[] runReturnHandled(final Consumer<Unhandled> onFailure, final ExRunnable... runnable) {
        return stream(runnable).map(task -> new NanoThread(this)
            .onComplete((thread, error) -> {
                if (error != null)
                    onFailure.accept(new Unhandled(this, thread, error));
            }).run(
                this.nano == null ? null : nano.threadPool(),
                () -> this.nano == null ? null : nano.newEmptyContext(this.getClass()),
                task
            )
        ).toArray(NanoThread[]::new);
    }

    /**
     * Executes one or multiple {@link Service} asynchronously.
     *
     * @param services The {@link Service} to be appended.
     * @return {@link NanoThread}s
     */
    public NanoThread[] runReturn(final Service... services) {
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
     * Executes asynchronously and waits for all runnable to be ready
     *
     * @param runnable function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    public final Context runAwait(final ExRunnable... runnable) {
        NanoThread.waitFor(runReturn(runnable));
        return this;
    }

    /**
     * Executes asynchronously and waits for all runnable to be ready
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return The {@link Context} object for chaining further operations.
     */
    public final Context runAwaitHandled(final Consumer<Unhandled> onFailure, final ExRunnable... runnable) {
        NanoThread.waitFor(runReturnHandled(onFailure, runnable));
        return this;
    }

    /**
     * Executes asynchronously and waits for all {@link Service} to be ready
     *
     * @return The {@link Context} object for chaining further operations.
     */
    public Context runAwait(final Service... services) {
        runAwaitReturn(services);
        return this;
    }

    //########## ASYNC AWAIT HELPER RETURN ##########

    /**
     * Executes asynchronously and waits for all {@link Service} to be ready
     *
     * @param runnable function to execute.
     * @return {@link NanoThread}s
     */
    public final NanoThread[] runAwaitReturn(final ExRunnable... runnable) {
        return NanoThread.waitFor(runReturn(runnable));
    }

    /**
     * Executes and waits for all {@link Service} to be ready
     *
     * @param onFailure function to execute on failure
     * @param runnable  function to execute.
     * @return {@link NanoThread}s
     */
    public final NanoThread[] runAwaitReturnHandled(final Consumer<Unhandled> onFailure, final ExRunnable... runnable) {
        return NanoThread.waitFor(runReturnHandled(onFailure, runnable));
    }

    /**
     * Executes and waits for all {@link Service} to be ready
     *
     * @return {@link NanoThread}s
     */
    public NanoThread[] runAwaitReturn(final Service... services) {
        return NanoThread.waitFor(runReturn(services));
    }

    //########## EVENT HELPER ##########

    /**
     * Sends an unhandled event with the provided, nullable payload and exception. If the event is not acknowledged, the error message is logged.
     *
     * @param payload   The payload of the unhandled event, containing data relevant to the event's context and purpose.
     * @param error The exception that occurred during the event processing.
     * @param message   The error message to log in case the event is not acknowledged.
     * @return self for chaining
     */
    public Context sendEventError(final Object payload, final Throwable error, final Supplier<String> message, final Object... params) {
        if (!sendEventReturn(EVENT_APP_UNHANDLED, new Unhandled(this, payload, error)).isAcknowledged()) {
            logger().error(error, () -> ofNullable(message).map(Supplier::get).orElse("Execution error [{}]"), (params == null || params.length == 0) && payload != null ? new Object[]{payload} : params);
        }
        return this;
    }

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
        broadcastEvent(eventType, payload, null);
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
        return sendEventReturn(eventType, payload, null);
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
        return broadcastEventReturn(eventType, payload, null);
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

    /**
     * Registers a new event type with a given name if it does not already exist.
     * If the event type already exists, it returns the existing event type's ID.
     *
     * @param typeName The name of the event type to register.
     * @return The ID of the newly registered event type, or the ID of the existing event type
     * if it already exists. Returns -1 if the input is null or empty.
     */
    public int registerEventType(final String typeName) {
        return EventTypeRegister.registerEventType(typeName);
    }

    /**
     * Retrieves the name of an event type given its ID.
     *
     * @param typeId The ID of the event type.
     * @return The name of the event type associated with the given ID, or null if not found.
     */
    public String eventNameOf(final int typeId) {
        return EventTypeRegister.eventNameOf(typeId);
    }

    /**
     * Attempts to find the ID of an event type based on its name.
     * This method is primarily used for debugging purposes or startup and is not optimized for performance.
     *
     * @param typeName The name of the event type.
     * @return An {@link Optional} containing the ID of the event type if found, or empty if not found
     * or if the input is null or empty.
     */
    public Optional<Integer> eventIdOf(final String typeName) {
        return EventTypeRegister.evenIdOf(typeName);
    }

    /**
     * Retrieves a {@link Service} of a specified type.
     *
     * @param <S>          The type of the service to retrieve, which extends {@link Service}.
     * @param serviceClass The class of the {@link Service} to retrieve.
     * @return The first instance of the specified {@link Service}, or null if not found.
     */
    public <S extends Service> S service(final Class<S> serviceClass) {
        return nano.service(serviceClass);
    }

    /**
     * Retrieves a list of services of a specified type.
     *
     * @param <S>          The type of the service to retrieve, which extends {@link Service}.
     * @param serviceClass The class of the service to retrieve.
     * @return A list of services of the specified type. If no services of this type are found,
     * an empty list is returned.
     */
    public <S extends Service> List<S> services(final Class<S> serviceClass) {
        return nano.services(serviceClass);
    }

    /**
     * Provides an unmodifiable list of all registered {@link Service}.
     *
     * @return An unmodifiable list of {@link Service} instances.
     */
    public List<Service> services() {
        return nano.services();
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
        context.nano().sendEvent(EVENT_APP_UNHANDLED, context, payload, result -> wasHandled.set(true), false);
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
