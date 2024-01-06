package de.yuna.berlin.nativeapp.core;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Scheduler;
import de.yuna.berlin.nativeapp.core.model.Unhandled;
import de.yuna.berlin.nativeapp.helper.ExRunnable;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_THREAD_POOL_ALIVE_MS;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_THREAD_POOL_MAX;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_THREAD_POOL_MIN;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_THREAD_POOL_TIMEOUT_MS;
import static de.yuna.berlin.nativeapp.helper.StringUtils.callerInfoStr;
import static de.yuna.berlin.nativeapp.helper.StringUtils.getThreadName;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SCHEDULER_REGISTER;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SCHEDULER_UNREGISTER;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static java.util.Collections.unmodifiableSet;

/**
 * The abstract base class for {@link Nano} framework providing thread handling functionalities.
 *
 * @param <T> The type of the {@link NanoThreads} implementation, used for method chaining.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public abstract class NanoThreads<T extends NanoThreads<T>> extends NanoBase<T> {

    protected final ThreadPoolExecutor threadPool;
    protected final Set<ScheduledExecutorService> schedulers;

    /**
     * Initializes {@link NanoThreads} with configurations and command-line arguments.
     *
     * @param config Configuration parameters for the {@link NanoThreads} instance.
     * @param args   Command-line arguments passed during the application start.
     */
    protected NanoThreads(final Map<Object, Object> config, final String... args) {
        super(config, args);
        final int corePoolSize = rootContext.gett(CONFIG_THREAD_POOL_MIN.id(), Integer.class).filter(value -> value > 0).orElse(Math.max(4, Runtime.getRuntime().availableProcessors() - 1));
        this.schedulers = ConcurrentHashMap.newKeySet();
        this.threadPool = new ThreadPoolExecutor(
                corePoolSize,
                rootContext.gett(CONFIG_THREAD_POOL_MAX.id(), Integer.class).filter(value -> value > 0).orElse(Math.max(corePoolSize, Math.min(256, Runtime.getRuntime().availableProcessors() * 4))),
                rootContext.gett(CONFIG_THREAD_POOL_ALIVE_MS.id(), Long.class).filter(value -> value > 0).orElse(60000L),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(corePoolSize),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        addEventListener(EVENT_APP_SCHEDULER_REGISTER.id(), event -> event.payloadOpt(ScheduledExecutorService.class).map(schedulers::add).ifPresent(nano -> event.acknowledge()));
        addEventListener(EVENT_APP_SCHEDULER_UNREGISTER.id(), event -> event.payloadOpt(ScheduledExecutorService.class).map(scheduler -> {
            scheduler.shutdown();
            schedulers.remove(scheduler);
            return this;
        }).ifPresent(nano -> event.acknowledge()));
    }

    /**
     * Gets the thread pool executor used by {@link Nano}.
     *
     * @return The ThreadPoolExecutor instance.
     */
    public ThreadPoolExecutor threadPool() {
        return threadPool;
    }

    /**
     * Provides an unmodifiable set of {@link ScheduledExecutorService}.
     *
     * @return An unmodifiable set of {@link ScheduledExecutorService} instances.
     */
    public Set<ScheduledExecutorService> schedulers() {
        return unmodifiableSet(schedulers);
    }

    /**
     * Executes a {@link Runnable} task asynchronously using the thread pool ({@link NanoThreads#threadPool}).
     *
     * @param runnable The {@link Runnable} task to execute.
     * @return A {@link CompletableFuture} representing the task's completion.
     */
    public CompletableFuture<Void> execute(final Runnable runnable) {
        return CompletableFuture.runAsync(runnable, threadPool);
    }

    /**
     * Schedules a task to be executed asynchronously after a specified delay.
     *
     * @param task     The task to execute.
     * @param delay    The delay before executing the task.
     * @param timeUnit The time unit of the delay parameter.
     * @return Self for chaining
     */
    @SuppressWarnings({"resource", "unchecked"})
    public T schedule(final ExRunnable task, final long delay, final TimeUnit timeUnit) {
        final ScheduledExecutorService scheduler = asyncFromPool();
        scheduler.schedule(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                final Context context = context(this.getClass());
                final AtomicBoolean handled = new AtomicBoolean(false);
                sendEvent(EVENT_APP_UNHANDLED.id(), context, new Unhandled(context, scheduler, e), response -> handled.set(true), false, true, true);
                if (!handled.get()) {
                    logger().error(e, () -> "Execution error scheduler [{}]", scheduler instanceof final Scheduler cron ? cron.id() : scheduler);
                }
            } finally {
                sendEvent(EVENT_APP_SCHEDULER_UNREGISTER.id(), context(this.getClass()), scheduler, null, false, true, true);
            }
        }, delay, timeUnit);
        return (T) this;
    }

    /**
     * Schedules a task to be executed periodically, starting after an initial delay.
     *
     * @param task         The task to execute.
     * @param initialDelay The initial delay before executing the task.
     * @param period       The period between successive task executions.
     * @param unit         The time unit of the initialDelay and period parameters.
     * @param until        A BooleanSupplier indicating the termination condition.
     * @return Self for chaining
     */
    @SuppressWarnings({"resource", "unchecked"})
    public T schedule(final Runnable task, final long initialDelay, final long period, final TimeUnit unit, final BooleanSupplier until) {
        final ScheduledExecutorService scheduler = asyncFromPool();

        // Periodic task
        scheduler.scheduleAtFixedRate(() -> {
            if (until.getAsBoolean()) {
                scheduler.shutdown();
            } else {
                task.run();
            }
        }, initialDelay, period, unit);
        return (T) this;
    }

    /**
     * Creates a {@link ScheduledExecutorService} from the thread pool.
     *
     * @return The newly created {@link ScheduledExecutorService}.
     */
    protected ScheduledExecutorService asyncFromPool() {
        final String schedulerId = callerInfoStr(this.getClass()) + "_" + UUID.randomUUID();
        final Scheduler scheduler = new Scheduler(schedulerId) {
            @Override
            protected void beforeExecute(final Thread t, final Runnable r) {
                threadPool.submit(r);
                t.setName("Scheduler_" + schedulerId);
            }
        };
        sendEvent(EVENT_APP_SCHEDULER_REGISTER.id(), context(Scheduler.class), scheduler, null, false, false, false);
        return scheduler;
    }

    /**
     * Shuts down all threads and scheduled executors gracefully.
     */
    protected void shutdownThreads() {
        final long timeoutMs = rootContext.gett(CONFIG_THREAD_POOL_TIMEOUT_MS.id(), Long.class).filter(l -> l > 0).orElse(500L);
        logger.debug(() -> "Shutdown schedulers [{}]", schedulers.size());
        shutdownExecutors(timeoutMs, schedulers.toArray(ScheduledExecutorService[]::new));
        logger.debug(() -> "Shutdown {} [{}]", threadPool.getClass().getSimpleName(), threadPool.getActiveCount());
        shutdownExecutors(timeoutMs, threadPool);
    }

    /**
     * Shuts down executors and handles timeout for forced termination.
     *
     * @param timeoutMs        The maximum time to wait for executor termination.
     * @param executorServices An array of ExecutorService instances to shut down.
     */
    protected void shutdownExecutors(final long timeoutMs, final ExecutorService... executorServices) {
        Arrays.stream(executorServices).parallel().forEach(executorService -> {
            executorService.shutdown();
            try {
                kill(executorService, timeoutMs);
                removeScheduler(executorService);
            } catch (final InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Removes a scheduler from the set of managed schedulers.
     *
     * @param executorService The {@link ExecutorService} to remove from the scheduler set.
     */
    protected void removeScheduler(final ExecutorService executorService) {
        if (executorService instanceof final ScheduledExecutorService scheduler) {
            schedulers.remove(scheduler);
        }
    }

    /**
     * Forces shutdown of an {@link ExecutorService} if it doesn't terminate within the specified timeout.
     *
     * @param executorService The executor service to shut down.
     * @param timeoutMs       The maximum time to wait for termination.
     * @throws InterruptedException if interrupted while waiting.
     */
    protected void kill(final ExecutorService executorService, final long timeoutMs) throws InterruptedException {
        if (!executorService.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
            logger.debug(() -> "Kill [{}]", getThreadName(executorService));
            executorService.shutdownNow();
            if (!executorService.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                logger.warn(() -> "[{}] did not terminate", getThreadName(executorService));
            }
        }
    }
}
