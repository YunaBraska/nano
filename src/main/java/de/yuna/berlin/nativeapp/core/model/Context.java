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
        return gett(CONTEXT_LOGGER_KEY, NanoLogger.class)
            .orElseGet(() -> setLogger(Context.class).get(CONTEXT_LOGGER_KEY, NanoLogger.class).warn(() -> "Fallback to generic logger used. It is recommended to provide a context-specific logger for improved traceability and context-aware logging. A context-specific logger allows for more granular control over logging behaviors, including level filtering, log format customization, and targeted log output, which enhances the debugging and monitoring capabilities. Using a generic logger might result in less optimal logging granularity and difficulty in tracing issues related to specific contexts.", new IllegalStateException("Context-specific logger not provided. Falling back to a generic logger.")));
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

    @Override
    public Context putt(final Object key, final Object value) {
        this.put(key, value);
        return this;
    }

    @Override
    public Context addd(final Object key, final Object value) {
        this.put(key, value);
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
     * Sends an event with the specified type and payload.
     *
     * @param eventType The type of the event.
     * @param payload   The payload of the event.
     * @return Self for chaining
     */
    public Context sendEvent(final int eventType, final Object payload) {
        nano.sendEvent(eventType, this, payload, null, true, false, false);
        return this;
    }

    /**
     * Sends an event with the specified type and payload and allows for a response listener.
     *
     * @param eventType        The type of the event.
     * @param payload          The payload of the event.
     * @param responseListener A consumer to handle responses from the event processing.
     * @return Self for chaining
     */
    public Context sendEvent(final int eventType, final Object payload, final Consumer<Object> responseListener) {
        nano.sendEvent(eventType, this, payload, responseListener, true, false, false);
        return this;
    }

    /**
     * Sends an event and specifies whether to wait for the event processing to complete.
     *
     * @param eventType The type of the event.
     * @param payload   The payload of the event.
     * @param await     Whether to wait for the event processing to complete.
     * @return Self for chaining
     */
    public Context sendEvent(final int eventType, final Object payload, final boolean await) {
        nano.sendEvent(eventType, this, payload, null, true, await, false);
        return this;
    }

    /**
     * Sends an event with the specified type and payload, and allows specifying
     * whether to send to the first listener and whether to await completion.
     *
     * @param eventType The type of the event.
     * @param payload   The payload of the event.
     * @param toFirst   If true, sends the event only to the first matching listener.
     * @param await     If true, waits for the event processing to complete.
     * @return Self for chaining
     */
    public Context sendEvent(final int eventType, final Object payload, final boolean toFirst, final boolean await) {
        nano.sendEvent(eventType, this, payload, null, toFirst, await, false);
        return this;
    }

    /**
     * Sends an event with the specified type and payload, allowing control over
     * targeting the first listener, waiting for completion, and executing on the same thread.
     *
     * @param eventType  The type of the event.
     * @param payload    The payload of the event.
     * @param toFirst    If true, sends the event only to the first matching listener.
     * @param await      If true, waits for the event processing to complete.
     * @param sameThread If true, processes the event on the same thread.
     * @return Self for chaining
     */
    public Context sendEvent(final int eventType, final Object payload, final boolean toFirst, final boolean await, final boolean sameThread) {
        nano.sendEvent(eventType, this, payload, null, toFirst, await, sameThread);
        return this;
    }

    /**
     * Sends an event with the specified type and payload, with options for first listener targeting,
     * awaiting completion, same thread execution, and handling responses.
     *
     * @param eventType        The type of the event.
     * @param payload          The payload of the event.
     * @param toFirst          If true, sends the event only to the first matching listener.
     * @param await            If true, waits for the event processing to complete.
     * @param sameThread       If true, processes the event on the same thread.
     * @param responseListener A consumer to handle responses from the event processing.
     * @return Self for chaining
     */
    public Context sendEvent(final int eventType, final Object payload, final boolean toFirst, final boolean await, final boolean sameThread, final Consumer<Object> responseListener) {
        nano.sendEvent(eventType, this, payload, responseListener, toFirst, await, sameThread);
        return this;
    }

    //########## EVENT RETURN HELPER ##########

    /**
     * Sends an event with the specified type and payload.
     *
     * @param eventType The type of the event.
     * @param payload   The payload of the event.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int eventType, final Object payload) {
        return nano.sendEventReturn(eventType, this, payload, null, true, false, false);
    }

    /**
     * Sends an event with the specified type and payload and allows for a response listener.
     *
     * @param eventType        The type of the event.
     * @param payload          The payload of the event.
     * @param responseListener A consumer to handle responses from the event processing.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int eventType, final Object payload, final Consumer<Object> responseListener) {
        return nano.sendEventReturn(eventType, this, payload, responseListener, true, false, false);
    }

    /**
     * Sends an event and specifies whether to wait for the event processing to complete.
     *
     * @param eventType The type of the event.
     * @param payload   The payload of the event.
     * @param await     Whether to wait for the event processing to complete.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int eventType, final Object payload, final boolean await) {
        return nano().sendEventReturn(eventType, this, payload, null, true, await, await);
    }

    /**
     * Sends an event with the specified type and payload, and allows specifying
     * whether to send to the first listener and whether to await completion.
     *
     * @param eventType The type of the event.
     * @param payload   The payload of the event.
     * @param toFirst   If true, sends the event only to the first matching listener.
     * @param await     If true, waits for the event processing to complete.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int eventType, final Object payload, final boolean toFirst, final boolean await) {
        return nano().sendEventReturn(eventType, this, payload, null, toFirst, await, await);
    }

    /**
     * Sends an event with the specified type and payload, allowing control over
     * targeting the first listener, waiting for completion, and executing on the same thread.
     *
     * @param eventType  The type of the event.
     * @param payload    The payload of the event.
     * @param toFirst    If true, sends the event only to the first matching listener.
     * @param await      If true, waits for the event processing to complete.
     * @param sameThread If true, processes the event on the same thread.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int eventType, final Object payload, final boolean toFirst, final boolean await, final boolean sameThread) {
        return nano().sendEventReturn(eventType, this, payload, null, toFirst, await, sameThread);
    }

    /**
     * Sends an event with the specified type and payload, with options for first listener targeting,
     * awaiting completion, same thread execution, and handling responses.
     *
     * @param eventType        The type of the event.
     * @param payload          The payload of the event.
     * @param toFirst          If true, sends the event only to the first matching listener.
     * @param await            If true, waits for the event processing to complete.
     * @param sameThread       If true, processes the event on the same thread.
     * @param responseListener A consumer to handle responses from the event processing.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int eventType, final Object payload, final boolean toFirst, final boolean await, final boolean sameThread, final Consumer<Object> responseListener) {
        return nano().sendEventReturn(eventType, this, payload, responseListener, toFirst, await, sameThread);
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
        context.nano().sendEvent(EVENT_APP_UNHANDLED.id(), context, payload, result -> wasHandled.set(true), true, true, true);
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
            ", loglevel=" + gett(CONTEXT_LOGGER_KEY, NanoLogger.class).map(NanoLogger::level).orElse(null) +
            ", logQueue=" + gett(CONTEXT_LOGGER_KEY, NanoLogger.class).map(NanoLogger::logQueue).isPresent() +
            '}';
    }
}
