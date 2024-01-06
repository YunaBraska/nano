package de.yuna.berlin.nativeapp.helper.threads;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.helper.ExRunnable;
import de.yuna.berlin.nativeapp.services.ShortTask;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static java.util.Arrays.stream;

/**
 * The {@code Executor} class provides utility methods to execute {@link Service} instances
 * synchronously or asynchronously within a given {@link Context}. It handles the execution
 * lifecycle of services, including exception handling and synchronization.
 * <p>
 * This class offers static methods to execute one or more services in either synchronous
 * (blocking) or asynchronous (non-blocking) modes. It ensures that all runtime exceptions
 * are caught and handled appropriately. The class also provides mechanisms for post-execution
 * actions, such as invoking a consumer callback after asynchronous execution.
 * <p>
 * Usage of this class simplifies the management of service execution, allowing for
 * straightforward invocation of business logic encapsulated within services, along with
 * robust error handling and post-execution processing.
 *
 * <p><b>Note:</b> This class is designed to be used in a static context and is not intended
 * for instantiation.
 */
@SuppressWarnings({"unused", "UnusedReturnValue", "BooleanMethodIsAlwaysInverted"})
public class Executor {

    /**
     * Executes a collection of {@link Service} synchronously in the given context.
     * Waits for all {@link Service} to complete execution.
     * If any service throws an exception, it is handled by the global exception handler.
     *
     * @param context  The execution {@link Context} shared by all {@link Service}.
     * @param services Varargs parameter of {@link Service} to be executed.
     * @return {@link CompletableFuture}
     */
    public static CompletableFuture<Void> execAwait(final Context context, final Service... services) {
        try {
            final CompletableFuture<Void> future = execAsync(context, services);
            future.join();
            return future;
        } catch (final Exception exception) {
            handleExecutionExceptions(context, exception, () -> "Error while executing [" + stream(services).map(Service::name).distinct().collect(Collectors.joining()) + "]", services);
            return CompletableFuture.failedFuture(exception);
        }
    }

    /**
     * Executes a collection of {@link Service} asynchronously in the given context.
     * This method returns immediately and does not wait for the {@link Service} to complete.
     *
     * @param context  The execution {@link Context} shared by all {@link Service}.
     * @param services Varargs parameter of {@link Service} to be executed.
     * @return {@link CompletableFuture}
     */
    public static CompletableFuture<Void> execAsync(final Context context, final Service... services) {
        return CompletableFuture.allOf(stream(services).map(service -> service.feature(context)).toArray(CompletableFuture[]::new));
    }

    /**
     * Executes a collection of {@link Service} asynchronously and invokes a consumer
     * with the context once all {@link Service} are completed.
     *
     * @param context   The execution {@link Context} shared by all {@link Service}.
     * @param whenReady The consumer to be called with the context upon completion of all services.
     * @param services  Varargs parameter of {@link Service} to be executed.
     * @return {@link CompletableFuture}
     */
    public static CompletableFuture<Void> execAsync(final Context context, final Consumer<Context> whenReady, final Service... services) {
        return execAsync(context, whenReady == null ? services : new Service[]{new ShortTask(task -> {
            execAwait(context, services);
            whenReady.accept(context);
        })});
    }

    public static void handleExecutionExceptions(final Context context, final Throwable exception, final Supplier<String> errorMsg, final Object payload) {
        final AtomicBoolean wasHandled = new AtomicBoolean(false);
        context.nano().sendEvent(EVENT_APP_UNHANDLED.id(), context, payload, result -> wasHandled.set(true), true, true, true);
        if (!wasHandled.get()) {
            context.logger().error(exception, errorMsg);
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

    protected static boolean isConcurrentException(final Throwable ex) {
        return ex instanceof TimeoutException
                || ex instanceof ExecutionException
                || ex instanceof CancellationException
                || ex instanceof BrokenBarrierException
                || ex instanceof CompletionException
                || ex instanceof RejectedExecutionException;
    }

    private Executor() {
        // static util class
    }
}
