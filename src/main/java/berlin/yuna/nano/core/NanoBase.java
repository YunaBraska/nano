package berlin.yuna.nano.core;

import berlin.yuna.nano.core.model.Config;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.helper.LockedBoolean;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.helper.logger.LogFormatRegister;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.logic.NanoLogger;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.typemap.logic.ArgsDecoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static berlin.yuna.nano.helper.NanoUtils.*;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_LOG_LEVEL;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_LOG_QUEUE;
import static berlin.yuna.typemap.logic.TypeConverter.convertObj;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;

/**
 * The abstract base class for {@link Nano} framework providing the core functionalities.
 *
 * @param <T> The type of the {@link NanoBase} implementation, used for method chaining.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class NanoBase<T extends NanoBase<T>> {

    protected final Context rootContext;
    protected final long createdAtMs;
    protected final NanoLogger logger;
    protected final Map<Integer, Set<Consumer<Event>>> listeners = new ConcurrentHashMap<>();
    protected final LockedBoolean isReady = new LockedBoolean(true);
    protected final AtomicInteger eventCount = new AtomicInteger(0);
    @SuppressWarnings("java:S2386")
    public static final Map<Integer, String> EVENT_TYPES = new ConcurrentHashMap<>();
    public static final AtomicInteger EVENT_ID_COUNTER = new AtomicInteger(0);

    /**
     * Initializes the NanoBase with provided configurations and arguments.
     *
     * @param configs Configuration settings in a key-value map.
     * @param args    Command line arguments.
     */
    protected NanoBase(final Map<Object, Object> configs, final String... args) {
        this.createdAtMs = System.currentTimeMillis();
        this.rootContext = readConfigs(args);
        if (configs != null)
            configs.forEach((key, value) -> rootContext.computeIfAbsent(convertObj(key, String.class), add -> ofNullable(convertObj(value, String.class)).orElse("")));
        this.logger = new NanoLogger(this)
            .level(rootContext.getOpt(LogLevel.class, Config.CONFIG_LOG_LEVEL.id()).orElse(LogLevel.DEBUG))
            .formatter(rootContext.getOpt(Formatter.class, Config.CONFIG_LOG_FORMATTER.id()).orElseGet(() -> LogFormatRegister.getLogFormatter("console")));
        displayHelpMenu();
        subscribeEvent(EVENT_APP_LOG_LEVEL, event -> event.payloadOpt(LogLevel.class).or(() -> event.payloadOpt(Level.class).map(LogLevel::nanoLogLevelOf)).map(this::setLogLevel).ifPresent(nano -> event.acknowledge()));
        subscribeEvent(EVENT_APP_LOG_QUEUE, event -> event.payloadOpt(LogQueue.class).map(logger::logQueue).ifPresent(nano -> event.acknowledge()));
    }

    /**
     * Creates a {@link Context} with {@link NanoLogger} for the specified class.
     *
     * @param clazz The class for which the {@link Context} is to be created.
     * @return A new {@link Context} instance associated with the given class.
     */
    abstract Context newContext(final Class<?> clazz);

    /**
     * Sends an event to {@link Nano#listeners} and {@link Nano#services}.
     * Used {@link Context#sendEvent(int, Object)} from {@link Nano#newContext(Class)} instead of the core method.
     *
     * @param type             The integer representing the type of the event. This typically corresponds to a specific kind of event.
     * @param context          The {@link Context} in which the event is created and processed. It provides environmental data and configurations.
     * @param payload          The data or object that is associated with this event. This can be any relevant information that needs to be passed along with the event.
     * @param responseListener A consumer that handles the response of the event processing. It can be used to execute actions based on the event's outcome or data.
     * @param broadcast        Whether to send the event only to the first listener or service that response.
     * @return Self for chaining
     */
    abstract T sendEvent(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean broadcast);

    /**
     * Processes an event with the given parameters and decides on the execution path based on the presence of a response listener and the broadcast flag.
     * If a response listener is provided, the event is processed asynchronously; otherwise, it is processed in the current thread. This method creates an {@link Event} instance and triggers the appropriate event handling logic.
     *
     * @param type             The integer representing the type of the event, identifying the nature or action of the event.
     * @param context          The {@link Context} associated with the event, encapsulating environment and configuration details.
     * @param payload          The payload of the event, containing data relevant to the event's context and purpose.
     * @param responseListener A consumer for handling the event's response. If provided, the event is handled asynchronously; if null, the handling is synchronous.
     * @param broadCast        Determines the event's distribution: if true, the event is made available to all listeners; if false, it targets specific listeners based on the implementation logic.
     * @return An instance of {@link Event} that represents the event being processed. This object can be used for further operations or tracking.
     */
    abstract Event sendEventReturn(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean broadCast);

    /**
     * Initiates the shutdown process for the {@link Nano} instance.
     *
     * @param clazz class for which the {@link Context} is to be created.
     * @return Self for chaining
     */
    public abstract T stop(final Class<?> clazz);

    /**
     * Initiates the shutdown process for the {@link Nano} instance.
     *
     * @param context The {@link Context} in which {@link Nano} instance shuts down.
     * @return The current instance of {@link Nano} for method chaining.
     */
    public abstract T stop(final Context context);

    /**
     * Retrieves the logger associated with this instance.
     *
     * @return The {@link NanoLogger} for this instance.
     */
    public NanoLogger logger() {
        return logger;
    }

    /**
     * Retrieves the registered event listeners.
     *
     * @return A map of event types to their respective listeners.
     */
    public Map<Integer, Set<Consumer<Event>>> listeners() {
        return listeners;
    }

    /**
     * Registers an event listener for a specific event type.
     *
     * @param eventType The integer identifier of the event type.
     * @param listener  The consumer function that processes the {@link Event}.
     * @return Self for chaining
     */
    @SuppressWarnings({"unchecked"})
    public T subscribeEvent(final int eventType, final Consumer<Event> listener) {
        listeners.computeIfAbsent(eventType, value -> new LinkedHashSet<>()).add(listener);
        return (T) this;
    }

    /**
     * Removes a registered event listener for a specific event type.
     *
     * @param eventType The integer identifier of the event type.
     * @param listener  The consumer function to be removed.
     * @return Self for chaining
     */
    @SuppressWarnings({"unchecked"})
    public T unsubscribeEvent(final int eventType, final Consumer<Event> listener) {
        listeners.computeIfAbsent(eventType, value -> new LinkedHashSet<>()).remove(listener);
        return (T) this;
    }

    /**
     * Retrieves the process ID of the current instance.
     *
     * @return The process ID.
     */
    public long pid() {
        return ProcessHandle.current().pid();
    }

    /**
     * Calculates the memory usage of the application in megabytes.
     *
     * @return Memory usage in megabytes, rounded to two decimal places.
     */
    public double usedMemoryMB() {
        final Runtime runtime = Runtime.getRuntime();
        return BigDecimal.valueOf((double) (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Retrieves the creation timestamp of the instance.
     *
     * @return The timestamp of creation in milliseconds.
     */
    public long createdAtMs() {
        return createdAtMs;
    }

    /**
     * Checks whether the instance is ready for operations.
     *
     * @return readiness state.
     */
    public boolean isReady() {
        return isReady.get();
    }

    public int eventCount() {
        return eventCount.get();
    }

    /**
     * Displays a help menu with available configuration keys and their descriptions and exits.
     */
    protected void displayHelpMenu() {
        if (rootContext.getOpt(Boolean.class, Config.APP_HELP.id()).filter(helpCalled -> helpCalled).isPresent()) {
            logger.info(() -> "Available configs keys: " + lineSeparator() + stream(Config.values()).map(config -> String.format("%-" + stream(Config.values()).map(Config::id).mapToInt(String::length).max().orElse(0) + "s  %s", config, config.description())).collect(Collectors.joining(lineSeparator())));
            System.exit(0);
        }
    }

    /**
     * Reads and initializes {@link Context} based on provided arguments.
     *
     * @param args Command-line arguments.
     * @return The {@link Context} initialized with the configurations.
     */
    protected Context readConfigs(final String... args) {
        final Context result = readConfigFiles(null, "");
        System.getenv().forEach((key, value) -> addConfig(result, key, value));
        System.getProperties().forEach((key, value) -> addConfig(result, key, value));
        ArgsDecoder.argsOf(String.join(" ", args)).forEach((key, value) -> addConfig(result, key, value));
        return resolvePlaceHolders(result);
    }

    /**
     * Sets the logging level for the NanoBase instance.
     *
     * @param level The logging level to be set.
     * @return True if the level was successfully set.
     */
    @SuppressWarnings("unchecked")
    protected T setLogLevel(final LogLevel level) {
        logger.level(level);
        rootContext.put(Config.CONFIG_LOG_LEVEL.id(), level);
        logger.trace(() -> "New {} [{}]", LogLevel.class.getSimpleName(), level);
        return (T) this;
    }

    /**
     * Standardizes a config key.
     *
     * @param key The config key to be standardized.
     */
    @SuppressWarnings("java:S3358") // Ternary operator should not be nested
    public static String standardiseKey(final Object key) {
        return key == null ? null : convertObj(key, String.class).replace('.', '_').replace('-', '_').trim().toLowerCase();
    }

}
