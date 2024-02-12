package de.yuna.berlin.nativeapp.core;

import berlin.yuna.typemap.logic.ArgsDecoder;
import de.yuna.berlin.nativeapp.core.model.Config;
import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.event.model.EventType;
import de.yuna.berlin.nativeapp.helper.logger.LogFormatRegister;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.logic.NanoLogger;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static berlin.yuna.typemap.config.TypeConversionRegister.registerTypeConvert;
import static berlin.yuna.typemap.logic.TypeConverter.convertObj;
import static de.yuna.berlin.nativeapp.core.model.Config.*;
import static de.yuna.berlin.nativeapp.core.model.Context.createRootContext;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_LOG_QUEUE;
import static de.yuna.berlin.nativeapp.helper.logger.LogFormatRegister.getLogFormatter;
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
    protected final AtomicBoolean isReady = new AtomicBoolean(true);
    protected final AtomicInteger eventCount = new AtomicInteger(0);
    @SuppressWarnings("java:S2386")
    public static final Map<Integer, String> EVENT_TYPES = new ConcurrentHashMap<>();
    public static final AtomicInteger EVENT_ID_COUNTER = new AtomicInteger(0);

    static {
        registerTypeConvert(String.class, Formatter.class, LogFormatRegister::getLogFormatter);
        registerTypeConvert(String.class, LogLevel.class, LogLevel::nanoLogLevelOf);
        registerTypeConvert(LogLevel.class, String.class, Enum::name);
        registerTypeConvert(Config.class, String.class, Config::id);
        registerTypeConvert(EventType.class, String.class, eventType -> String.valueOf(eventType.id()));
        registerTypeConvert(String.class, EventType.class, eventType -> stream(EventType.values()).filter(et -> et.name().equals(eventType) || (et.id() + "").equals(eventType)).findFirst().orElse(null));
    }

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
            .level(rootContext.gett(CONFIG_LOG_LEVEL.id(), LogLevel.class).orElse(LogLevel.DEBUG))
            .formatter(rootContext.gett(CONFIG_LOG_FORMATTER.id(), Formatter.class).orElseGet(() -> getLogFormatter("console")));
        displayHelpMenu();
        addEventListener(EVENT_APP_LOG_LEVEL.id(), event -> event.payloadOpt(LogLevel.class).or(() -> event.payloadOpt(Level.class).map(LogLevel::nanoLogLevelOf)).map(this::setLogLevel).ifPresent(nano -> event.acknowledge()));
        addEventListener(EVENT_APP_LOG_QUEUE.id(), event -> event.payloadOpt(LogQueue.class).map(logger::logQueue).ifPresent(nano -> event.acknowledge()));
    }

    /**
     * Creates a {@link Context} with {@link NanoLogger} for the specified class.
     *
     * @param clazz The class for which the {@link Context} is to be created.
     * @return A new {@link Context} instance associated with the given class.
     */
    abstract Context context(final Class<?> clazz);

    /**
     * Sends an event to {@link Nano#listeners} and {@link Nano#services}.
     * Used {@link Context#sendEvent(int, Object)} from {@link Nano#context(Class)} instead of the core method.
     *
     * @param type             The integer representing the type of the event. This typically corresponds to a specific kind of event.
     * @param context          The {@link Context} in which the event is created and processed. It provides environmental data and configurations.
     * @param payload          The data or object that is associated with this event. This can be any relevant information that needs to be passed along with the event.
     * @param responseListener A consumer that handles the response of the event processing. It can be used to execute actions based on the event's outcome or data.
     * @param toFirst          Whether to send the event only to the first listener or service that response.
     * @param await            Whether to wait for the event processing to complete.
     * @param sameThread       Whether to process the event on the same thread.
     * @return Self for chaining
     */
    abstract T sendEvent(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean toFirst, final boolean await, final boolean sameThread);

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
    public T addEventListener(final int eventType, final Consumer<Event> listener) {
        listeners.computeIfAbsent(eventType, value -> new HashSet<>()).add(listener);
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
    public T removeEventListener(final int eventType, final Consumer<Event> listener) {
        listeners.computeIfAbsent(eventType, value -> new HashSet<>()).remove(listener);
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
        if (rootContext.gett(APP_HELP.id(), Boolean.class).filter(helpCalled -> helpCalled).isPresent()) {
            logger.info(() -> "Available configs keys: " + lineSeparator() + stream(Config.values()).map(config -> String.format("%-" + stream(Config.values()).map(Config::id).mapToInt(String::length).max().orElse(0) + "s  %s", config.id(), config.description())).collect(Collectors.joining(lineSeparator())));
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
        final Context result = createRootContext();
        System.getenv().forEach((key, value) -> addStandardisedKey(key, value, result));
        System.getProperties().forEach((key, value) -> addStandardisedKey(String.valueOf(key), value, result));
        ArgsDecoder.argsOf(String.join(" ", args)).forEach((key, value) -> addStandardisedKey(key, value, result));
        return result;
    }

    /**
     * Standardizes and adds a key-value pair to the {@link Context}.
     *
     * @param key    The key to be standardized and added.
     * @param value  The value associated with the key.
     * @param result The {@link Context} to which the key-value pair is added.
     */
    protected void addStandardisedKey(final String key, final Object value, final Context result) {
        result.put(key.replace('.', '_').replace('-', '_').toLowerCase(), value);
    }

    /**
     * Sets the logging level for the NanoBase instance.
     *
     * @param level The logging level to be set.
     * @return True if the level was successfully set.
     */
    protected boolean setLogLevel(final LogLevel level) {
        logger.level(level);
        rootContext.put(CONFIG_LOG_LEVEL.id(), level);
        logger.trace(() -> "New {} [{}]", LogLevel.class.getSimpleName(), level);
        return true;
    }
}
