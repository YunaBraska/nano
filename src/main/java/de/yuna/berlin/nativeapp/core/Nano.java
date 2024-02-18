package de.yuna.berlin.nativeapp.core;

import berlin.yuna.typemap.model.FunctionOrNull;
import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.helper.event.EventTypeRegister;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.logic.NanoLogger;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static de.yuna.berlin.nativeapp.core.model.Config.APP_PARAMS;
import static de.yuna.berlin.nativeapp.core.model.Context.tryExecute;
import static de.yuna.berlin.nativeapp.core.model.NanoThread.*;
import static de.yuna.berlin.nativeapp.helper.NanoUtils.formatDuration;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.*;
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
        logger.debug(() -> "Init {} in [{}]", this.getClass().getSimpleName(), formatDuration(System.currentTimeMillis() - createdAtMs));
        printParameters();
        final Context context = this.context(this.getClass());
        final long service_startUpTime = System.currentTimeMillis();
        logger.debug(() -> "Start {}", this.getClass().getSimpleName());
        // INIT SHUTDOWN HOOK
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown(context(this.getClass()))));
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
        // INIT CLEANUP TASK - just for safety
        schedule(() -> new HashSet<>(schedulers).stream().filter(scheduler -> scheduler.isShutdown() || scheduler.isTerminated()).forEach(schedulers::remove), 256, 256, TimeUnit.MILLISECONDS, () -> true);
        logger.info(() -> "Running Services [{}]", services().stream().collect(Collectors.groupingBy(Service::name, Collectors.counting())).entrySet().stream().map(entry -> entry.getValue() > 1 ? "(" + entry.getValue() + ") " + entry.getKey() : entry.getKey()).collect(joining(", ")));
        logger.info(() -> "Started [{}] in [{}]", this.getClass().getSimpleName(), formatDuration(System.currentTimeMillis() - service_startUpTime));
        printSystemInfo();
        sendEvent(EVENT_APP_START.id(), context, null, null, false, false, false);
        addEventListener(EVENT_APP_SHUTDOWN.id(), event -> event.acknowledge(() -> CompletableFuture.runAsync(() -> shutdown(context(this.getClass())))));
    }

    /**
     * Creates a {@link Context} with {@link NanoLogger} for the specified class.
     *
     * @param clazz The class for which the {@link Context} is to be created.
     * @return A new {@link Context} instance associated with the given class.
     */
    public Context context(final Class<?> clazz) {
        return rootContext.copy(clazz, this);
    }

    /**
     * Initiates the shutdown process for the {@link Nano} instance.
     *
     * @param clazz class for which the {@link Context} is to be created.
     * @return The current instance of {@link Nano} for method chaining.
     */
    @Override
    public Nano stop(final Class<?> clazz) {
        return stop(clazz != null ? context(clazz) : null);
    }

    /**
     * Initiates the shutdown process for the {@link Nano} instance.
     *
     * @param context The {@link Context} in which {@link Nano} instance shuts down.
     * @return The current instance of {@link Nano} for method chaining.
     */
    @Override
    public Nano stop(final Context context) {
        sendEvent(EVENT_APP_SHUTDOWN.id(), context != null ? context : context(this.getClass()), null, null, false, false, false);
        return this;
    }

    /**
     * Prints the configurations that have been loaded into the {@link Nano} instance.
     */
    public void printParameters() {
        if (rootContext.gett(APP_PARAMS.id(), Boolean.class).filter(helpCalled -> helpCalled).isPresent()) {
            final List<String> secrets = List.of("secret", "token", "pass", "pwd", "bearer", "auth", "private", "ssn");
            final int keyLength = rootContext.keySet().stream().map(String::valueOf).mapToInt(String::length).max().orElse(0);
            logger.info(() -> "Configs: " + lineSeparator() + rootContext.entrySet().stream().map(config -> String.format("%-" + keyLength + "s  %s", config.getKey(), secrets.stream().anyMatch(s -> String.valueOf(config.getKey()).toLowerCase().contains(s)) ? "****" : config.getValue())).collect(joining(lineSeparator())));
        }
    }

    /**
     * Sends an event to {@link Nano#listeners} and {@link Service}.
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
    @Override
    public Nano sendEvent(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean toFirst, final boolean await, final boolean sameThread) {
        sendEventReturn(type, context, payload, responseListener, toFirst, await, sameThread);
        return this;
    }

    /**
     * Sends an event based on the provided Event object and processing flags.
     * Used {@link Context#sendEventReturn(int, Object)} from {@link Nano#context(Class)} instead of the core method.
     *
     * @param toFirst    Whether to send the event only to the first matching listener.
     * @param await      Whether to wait for the event processing to complete.
     * @param sameThread Whether to process the event on the same thread.
     * @return The event that was sent.
     */
    public Event sendEventReturn(final int type, final Context context, final Object payload, final Consumer<Object> responseListener, final boolean toFirst, final boolean await, final boolean sameThread) {
        final Event event = new Event(type, context, payload, responseListener);
        if (await && sameThread) {
            sendEventSameThread(event, toFirst);
        } else if (await) {
            event.context().asyncAwait(ctx -> sendEventSameThread(event, toFirst));
        } else {
            event.context().async(ctx -> sendEventSameThread(event, toFirst));
        }
        return event;
    }

    /**
     * Sends an event on the same thread and determines whether to process it to the first listener.
     * Used {@link Context#sendEvent(int, Object)} from {@link Nano#context(Class)} instead of the core method.
     *
     * @param event   The event to be processed.
     * @param toFirst Whether to send the event only to the first matching listener.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void sendEventSameThread(final Event event, final boolean toFirst) {
        eventCount.incrementAndGet();
        tryExecute(() -> {
            final boolean match = listeners.getOrDefault(event.id(), Collections.emptySet()).stream().anyMatch(listener -> {
                tryExecute(() -> listener.accept(event), throwable -> event.context().logger().error(throwable, () -> "Error processing {} [{}] payload [{}]", Event.class.getSimpleName(), event.name(), event.payload()));
                return toFirst && event.isAcknowledged();
            });
            if (!match) {
                services.stream().filter(Service::isReady).anyMatch(service -> {
                    tryExecute(() -> service.onEvent(event), throwable -> handleEventServiceException(event, service, throwable));
                    return toFirst && event.isAcknowledged();
                });
            }
        });
        eventCount.decrementAndGet();
    }

    protected void handleEventServiceException(final Event event, final Service service, final Throwable throwable) {
        if (event.id() != EVENT_APP_UNHANDLED.id()) {
            service.handleServiceException(event.context(), throwable);
        } else {
            // loop prevention
            event.context().logger().error(throwable, () -> "Error processing {} [{}] service [{}] payload [{}]", Event.class.getSimpleName(), EventTypeRegister.eventNameOf(event.id()), service.name(), event.payload());
        }
    }

    /**
     * Shuts down the {@link Nano} instance, ensuring all services and threads are gracefully terminated.
     *
     * @param clazz class for which the {@link Context} is to be created.
     * @return Self for chaining
     */
    protected Nano shutdown(final Class<?> clazz) {
        this.shutdown(context(clazz));
        return this;
    }

    /**
     * Shuts down the {@link Nano} instance, ensuring all services and threads are gracefully terminated.
     *
     * @param context The {@link Context} in which {@link Nano} instance shuts down.
     * @return Self for chaining
     */
    protected Nano shutdown(final Context context) {
        final Thread thread = new Thread(() -> {
            if (isReady.compareAndSet(true, false)) {
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
            }
        }, Nano.class.getSimpleName() + " Shutdown-Thread");
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
