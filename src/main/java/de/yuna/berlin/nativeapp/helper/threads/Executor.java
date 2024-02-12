package de.yuna.berlin.nativeapp.helper.threads;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.NanoThread;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.core.model.Unhandled;
import de.yuna.berlin.nativeapp.helper.ExRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static de.yuna.berlin.nativeapp.core.model.NanoThread.waitFor;
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
     * @return {@link NanoThread}s
     */
    public static NanoThread[] execAwait(final Context context, final Service... services) {
        try {
            return waitFor(execAsync(context, services));
        } catch (final Exception exception) {
            handleExecutionExceptions(context, new Unhandled(context, services.length == 1 ? services[0] : services, exception), () -> "Error while executing [" + stream(services).map(Service::name).distinct().collect(Collectors.joining()) + "]");
            Thread.currentThread().interrupt();
            return new NanoThread[0];
        }
    }

    /**
     * Executes a collection of {@link Service} asynchronously in the given context.
     * This method returns immediately and does not wait for the {@link Service} to complete.
     *
     * @param context  The execution {@link Context} shared by all {@link Service}.
     * @param services Varargs parameter of {@link Service} to be executed.
     * @return array of {@link NanoThread}
     */
    public static NanoThread[] execAsync(final Context context, final Service... services) {
        return stream(services).map(service -> service.nanoThread(context)).toArray(NanoThread[]::new);
    }

    /**
     * Executes a collection of {@link Service} asynchronously and invokes a consumer
     * with the context once all {@link Service} are completed.
     *
     * @param context   The execution {@link Context} shared by all {@link Service}.
     * @param whenReady The consumer to be called with the context upon completion of all services.
     * @param services  Varargs parameter of {@link Service} to be executed.
     * @return {@link NanoThread}s
     */
    public static NanoThread[] execAsync(final Context context, final Consumer<Context> whenReady, final Service... services) {
        final NanoThread[] threads = execAsync(context, services);
        waitFor(() -> whenReady.accept(context), threads);
        return threads;
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

    private Executor() {
        // static util class
    }
}
