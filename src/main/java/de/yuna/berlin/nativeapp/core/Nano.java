package de.yuna.berlin.nativeapp.core;

import berlin.yuna.typemap.model.FunctionOrNull;
import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.logic.NanoLogger;
import de.yuna.berlin.nativeapp.services.metric.model.MetricUpdate;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.yuna.berlin.nativeapp.core.model.Config.APP_PARAMS;
import static de.yuna.berlin.nativeapp.core.model.Context.tryExecute;
import static de.yuna.berlin.nativeapp.core.model.NanoThread.activeCarrierThreads;
import static de.yuna.berlin.nativeapp.core.model.NanoThread.activeNanoThreads;
import static de.yuna.berlin.nativeapp.helper.NanoUtils.formatDuration;
import static de.yuna.berlin.nativeapp.helper.NanoUtils.waitForCondition;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.*;
import static de.yuna.berlin.nativeapp.services.metric.model.MetricType.GAUGE;
import static java.lang.System.lineSeparator;
import static java.util.stream.Collectors.joining;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Nano extends NanoServices<Nano> {

    /**
     * Initializes {@link Nano} with a set of startup {@link Service}.
     *
     * @param startupServices Varargs parameter of startup {@link Service} to be initiated during the {@link Nano} creation.
     */
    public Nano(final Service... startupServices) {
        this(null, startupServices);
    }

    /**
     * Initializes  {@link Nano} with configurations and startup {@link Service}.
     *
     * @param config          Map of configuration parameters.
     * @param startupServices Varargs parameter of startup {@link Service} to be initiated.
     */
    public Nano(final Map<Object, Object> config, final Service... startupServices) {
        this(ctx -> List.of(startupServices), config);
    }

    /**
     * Initializes {@link Nano} with a function to provide startup {@link Service} based on the context.
     *
     * @param startupServices Function to provide startup {@link Service} based on the given context.
     * @param args            Command-line arguments passed during the application start.
     */
    public Nano(final FunctionOrNull<Context, List<Service>> startupServices, final String... args) {
        this(startupServices, null, args);
    }

    /**
     * Initializes {@link Nano} with a function to provide startup {@link Service}, configurations, and command-line arguments.
     *
     * @param startupServices Function to provide startup {@link Service} based on the given context.
     * @param config          Map of configuration parameters.
     * @param args            Command-line arguments passed during the application start.
     */
    public Nano(final FunctionOrNull<Context, List<Service>> startupServices, final Map<Object, Object> config, final String... args) {
        super(config, args);
        final long initTime = System.currentTimeMillis() - createdAtMs;
        logger.debug(() -> "Init {} in [{}]", this.getClass().getSimpleName(), formatDuration(initTime));
        printParameters();
        final Context context = this.newContext(this.getClass());
        final long service_startUpTime = System.currentTimeMillis();
        logger.debug(() -> "Start {}", this.getClass().getSimpleName());
        // INIT SHUTDOWN HOOK
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(newContext(this.getClass()))));
        if (startupServices != null) {
            final List<Service> services = startupServices.apply(context);
            if (services != null) {
                logger.debug(() -> "PreStartupServices count [{}] services [{}]", services.size(), services.stream().map(Service::name).distinct().collect(joining(", ")));
                final Map<Boolean, List<Service>> partitionedServices = services.stream().collect(Collectors.partitioningBy(LogQueue.class::isInstance));
                // INIT ASYNC LOGGING
                partitionedServices.getOrDefault(true, Collections.emptyList()).stream().findFirst().ifPresent(context::async);
                // INIT SERVICES
                context.asyncAwait(partitionedServices.getOrDefault(false, Collections.emptyList()).toArray(Service[]::new));
            }
        }
        schedule(() -> sendEvent(EVENT_APP_HEARTBEAT, context, this, result -> {}, true), 256, 256, TimeUnit.MILLISECONDS, () -> false);
        logger.info(() -> "Running Services [{}]", services().stream().collect(Collectors.groupingBy(Service::name, Collectors.counting())).entrySet().stream().map(entry -> entry.getValue() > 1 ? "(" + entry.getValue() + ") " + entry.getKey() : entry.getKey()).collect(joining(", ")));
        final long readyTime = System.currentTimeMillis() - service_startUpTime;
        logger.info(() -> "Started [{}] in [{}]", this.getClass().getSimpleName(), formatDuration(readyTime));
        printSystemInfo();
        sendEvent(EVENT_METRIC_UPDATE, context, new MetricUpdate(GAUGE, "application.started.time", initTime, null), result -> {}, false);
        sendEvent(EVENT_METRIC_UPDATE, context, new MetricUpdate(GAUGE, "application.ready.time", readyTime, null), result -> {}, false);
        addEventListener(EVENT_APP_SHUTDOWN, event -> event.acknowledge(() -> CompletableFuture.runAsync(() -> shutdown(newContext(this.getClass())))));
        // INIT CLEANUP TASK - just for safety
        addEventListener(EVENT_APP_HEARTBEAT, event -> new HashSet<>(schedulers).stream().filter(scheduler -> scheduler.isShutdown() || scheduler.isTerminated()).forEach(schedulers::remove));
    }

    /**
     * Creates a {@link Context} with {@link NanoLogger} for the specified class.
     *
     * @param clazz The class for which the {@link Context} is to be created.
     * @return A new {@link Context} instance associated with the given class.
     */
    public Context newContext(final Class<?> clazz) {
        return rootContext.newContext(clazz, this);
    }

    /**
     * Creates a empty {@link Context} with {@link NanoLogger} for the specified class.
     *
     * @param clazz The class for which the {@link Context} is to be created.
     * @return A new {@link Context} instance associated with the given class.
     */
    public Context newEmptyContext(final Class<?> clazz) {
        return rootContext.newEmptyContext(clazz, this);
    }

    /**
     * Initiates the shutdown process for the {@link Nano} instance.
     *
     * @param clazz class for which the {@link Context} is to be created.
     * @return The current instance of {@link Nano} for method chaining.
     */
    @Override
    public Nano stop(final Class<?> clazz) {
        return stop(clazz != null ? newContext(clazz) : null);
    }

    /**
     * Initiates the shutdown process for the {@link Nano} instance.
     *
     * @param context The {@link Context} in which {@link Nano} instance shuts down.
     * @return The current instance of {@link Nano} for method chaining.
     */
    @Override
    public Nano stop(final Context context) {
        sendEvent(EVENT_APP_SHUTDOWN, context != null ? context : newContext(this.getClass()), null, result -> {
        }, true);
        return this;
    }

    /**
     * This method blocks the current thread for max 10 seconds until the {@link Nano} instance is no longer ready.
     * This is useful in tests for ensuring that the application has fully stopped before proceeding with further operations.
     *
     * @return The current instance of {@link Nano} for method chaining.
     */
    public Nano waitForStop() {
        waitForCondition(() -> !isReady(), 10000);
        return this;
    }

    /**
     * Prints the configurations that have been loaded into the {@link Nano} instance.
     */
    public void printParameters() {
        if (rootContext.getOpt(Boolean.class, APP_PARAMS.id()).filter(helpCalled -> helpCalled).isPresent()) {
            final List<String> secrets = List.of("secret", "token", "pass", "pwd", "bearer", "auth", "private", "ssn");
            final int keyLength = rootContext.keySet().stream().map(String::valueOf).mapToInt(String::length).max().orElse(0);
            logger.info(() -> "Configs: " + lineSeparator() + rootContext.entrySet().stream().map(config -> String.format("%-" + keyLength + "s  %s", config.getKey(), secrets.stream().anyMatch(s -> String.valueOf(config.getKey()).toLowerCase().contains(s)) ? "****" : config.getValue())).collect(joining(lineSeparator())));
        }
    }

    /**
     * Sends an event with the specified parameters, either broadcasting it to all listeners or sending it to a targeted listener asynchronously if a response listener is provided.
     *
     * @param type             The integer representing the type of the event. This typically corresponds to a specific action or state change.
     * @param context          The {@link Context} in which the event is being sent, providing environmental data and configurations.
     * @param payload          The data or object associated with this event. This could be any relevant information that needs to be communicated through the event.
     * @param responseListener A consumer that handles the response of the event processing. If null, the event is processed in the same thread; otherwise, it's processed asynchronously.
     * @param broadcast        A boolean flag indicating whether the event should be broadcast to all listeners. If true, the event is broadcast; if false, it is sent to a targeted listener.
     * @return The {@link Nano} instance, allowing for method chaining.
     */
    public Nano sendEvent(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean broadcast) {
        sendEventReturn(type, context, payload, responseListener, broadcast);
        return this;
    }

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
    public Event sendEventReturn(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean broadCast) {
        final Event event = new Event(type, context, payload, responseListener);
        if (responseListener == null) {
            sendEventSameThread(event, broadCast);
        } else {
            //FIXME: batch processing to avoid too many threads?
            context.async(ctx -> sendEventSameThread(event, broadCast));
        }
        return event;
    }

    /**
     * Sends an event on the same thread and determines whether to process it to the first listener.
     * Used {@link Context#sendEvent(int, Object)} from {@link Nano#newContext(Class)} instead of the core method.
     *
     * @param event     The event to be processed.
     * @param broadcast Whether to send the event only to the first matching listener or to all.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void sendEventSameThread(final Event event, final boolean broadcast) {
        eventCount.incrementAndGet();
        tryExecute(() -> {
            final boolean match = listeners.getOrDefault(event.id(), Collections.emptySet()).stream().anyMatch(listener -> {
                tryExecute(() -> listener.accept(event), throwable -> event.context().logger().error(throwable, () -> "Error processing {} [{}] payload [{}]", Event.class.getSimpleName(), event.name(), event.payload()));
                return !broadcast && event.isAcknowledged();
            });
            if (!match) {
                services.stream().filter(Service::isReady).anyMatch(service -> {
                    tryExecute(() -> service.onEvent(event), throwable -> handleEventServiceException(event, service, throwable));
                    return !broadcast && event.isAcknowledged();
                });
            }
        });
        eventCount.decrementAndGet();
    }

    protected void handleEventServiceException(final Event event, final Service service, final Throwable throwable) {
        if (event.id() != EVENT_APP_UNHANDLED) {
            service.handleServiceException(event.context(), throwable);
        } else {
            // loop prevention
            event.context().logger().error(throwable, () -> "Error processing {} [{}] service [{}] payload [{}]", Event.class.getSimpleName(), event.name(), service.name(), event.payload());
        }
    }

    /**
     * Shuts down the {@link Nano} instance, ensuring all services and threads are gracefully terminated.
     *
     * @param clazz class for which the {@link Context} is to be created.
     * @return Self for chaining
     */
    protected Nano shutdown(final Class<?> clazz) {
        this.shutdown(newContext(clazz));
        return this;
    }

    /**
     * Shuts down the {@link Nano} instance, ensuring all services and threads are gracefully terminated.
     *
     * @param context The {@link Context} in which {@link Nano} instance shuts down.
     * @return Self for chaining
     */
    protected Nano shutdown(final Context context) {
        final Thread thread = new Thread(() -> isReady.set(true, false, run -> {
            final long startTimeMs = System.currentTimeMillis();
            logger.logQueue(null).info(() -> "Stop {} ...", this.getClass().getSimpleName());
            printSystemInfo();
            logger.debug(() -> "Shutdown Services count [{}] services [{}]", services.size(), services.stream().map(Service::getClass).map(Class::getSimpleName).distinct().collect(joining(", ")));
            shutdownServices(context);
            this.shutdownThreads();
            listeners.clear();
            printSystemInfo();
            logger.info(() -> "Stopped {} in [{}] with uptime [{}]", this.getClass().getSimpleName(), formatDuration(System.currentTimeMillis() - startTimeMs), formatDuration(System.currentTimeMillis() - createdAtMs));
            threadPool.shutdown();
            schedulers.clear();
        }), Nano.class.getSimpleName() + " Shutdown-Thread");
        thread.setDaemon(false); // JVM should wait until the thread is done
        thread.start();

        try {
            thread.join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error(e, () -> "Shutdown was interrupted");
        }
        return this;
    }

    /**
     * Prints system information for diagnostic purposes. Similar to {@link Nano#toString()}
     *
     * @return Self for chaining
     */
    public Nano printSystemInfo() {
        final long activeThreads = activeCarrierThreads();
        logger.debug(() -> "pid [{}] schedulers [{}] services [{}] listeners [{}] cores [{}] usedMemory [{}mb] threadsNano [{}], threadsActive [{}] threadsOther [{}] java [{}] arch [{}] os [{}]",
            pid(),
            schedulers.size(),
            services.size(),
            listeners.values().stream().mapToLong(Collection::size).sum(),
            Runtime.getRuntime().availableProcessors(),
            usedMemoryMB(),
            activeNanoThreads(),
            activeThreads,
            ManagementFactory.getThreadMXBean().getThreadCount() - activeThreads,
            System.getProperty("java.version"),
            System.getProperty("os.arch"),
            System.getProperty("os.name") + " - " + System.getProperty("os.version")
        );
        return this;
    }

    @Override
    public String toString() {
        final long activeThreads = activeCarrierThreads();
        return "Nano{" +
            "pid=" + pid() +
            ", schedulers=" + schedulers.size() +
            ", services=" + services.size() +
            ", listeners=" + listeners.values().stream().mapToLong(Collection::size).sum() +
            ", cores=" + Runtime.getRuntime().availableProcessors() +
            ", usedMemory=" + usedMemoryMB() + "mb" +
            ", threadsActive=" + activeNanoThreads() +
            ", threadsNano=" + activeThreads +
            ", threadsOther=" + (ManagementFactory.getThreadMXBean().getThreadCount() - activeThreads) +
            ", java=" + System.getProperty("java.version") +
            ", arch=" + System.getProperty("os.arch") +
            ", os=" + System.getProperty("os.name") + " - " + System.getProperty("os.version") +
            '}';
    }
}
