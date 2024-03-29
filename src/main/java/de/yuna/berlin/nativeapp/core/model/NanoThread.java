package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.helper.ExRunnable;
import de.yuna.berlin.nativeapp.helper.LockedBoolean;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class NanoThread {

    protected final List<BiConsumer<NanoThread, Throwable>> onCompleteCallbacks = new CopyOnWriteArrayList<>();
    protected final Context context;
    protected final LockedBoolean isComplete = new LockedBoolean();

    protected static final ExecutorService fallbackExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("nano-thread-", 0).factory());
    protected static final AtomicLong activeNanoThreadCount = new AtomicLong(0);

    public NanoThread() {
        this.context = null;
    }

    public NanoThread(final Context context) {
        this.context = context;
        if (context != null)
            context.setLogger(this.getClass());
    }

    public Context context() {
        return context;
    }

    public boolean isComplete() {
        return isComplete.get();
    }

    public NanoThread onComplete(final BiConsumer<NanoThread, Throwable> onComplete) {
        isComplete.run(state -> {
            if (Boolean.TRUE.equals(state)) {
                onComplete.accept(this, null);
            } else {
                onCompleteCallbacks.add(onComplete);
            }
        });
        return this;
    }

    public NanoThread execute(final ExRunnable task) {
        return execute(null, task);
    }

    public NanoThread await() {
        return await(null);
    }

    public NanoThread await(final Runnable onDone) {
        return waitFor(onDone, this)[0];
    }

    @SuppressWarnings("java:S1181")
    public NanoThread execute(final ExecutorService executor, final ExRunnable task) {
        (executor != null ? executor : fallbackExecutor).submit(() -> {
            try {
                activeNanoThreadCount.incrementAndGet();
                task.run();
                isComplete.set(true, state -> onCompleteCallbacks.forEach(onComplete -> onComplete.accept(this, null)));
            } catch (final Throwable error) {
                //TODO: handle OutOfMemory
                //TODO: handle InternalError
                //TODO: create an unhandled element and check if the error was unhandled
                isComplete.set(true, state -> onCompleteCallbacks.forEach(onComplete -> onComplete.accept(this, error)));
                if (context != null && onCompleteCallbacks.isEmpty()) {
                    // TODO: send unhandled event
                    context.logger().error(() -> "Unhandled Exception", error);
                }
            } finally {
                activeNanoThreadCount.decrementAndGet();
            }
        });
        return this;
    }

    public static long activeNanoThreads() {
        return activeNanoThreadCount.get();
    }

    public static long activeCarrierThreads() {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final long[] threadIds = threadMXBean.getAllThreadIds();
        return Arrays.stream(threadMXBean.getThreadInfo(threadIds))
            .filter(Objects::nonNull)
            .filter(info -> (info.getThreadName() != null && info.getThreadName().startsWith("CarrierThread"))
                || (info.getLockName() != null && info.getLockName().startsWith("java.lang.VirtualThread"))
                || (info.getLockOwnerName() != null && info.getLockName().startsWith("nano-thread-"))
            )
            .count();
    }

    /**
     * Blocks until all provided {@code NanoThread} instances have completed execution.
     * This method waits indefinitely for all threads to finish.
     *
     * @param threads An array of {@code NanoThread} instances to wait for.
     * @return The same array of {@code NanoThread} instances, allowing for method chaining or further processing.
     */
    public static NanoThread[] waitFor(final NanoThread... threads) {
        return waitFor(null, threads);
    }

    /**
     * Waits for all provided {@link NanoThread} instances to complete execution and optionally executes
     * a {@link Runnable} once all threads have finished. If {@code onComplete} is not null, it will be
     * executed asynchronously after all threads have completed. This variant allows for non-blocking
     * behavior if {@code onComplete} is provided, where the method returns immediately, and the
     * {@code onComplete} action is executed in the background once all threads are done.
     *
     * @param onComplete An optional {@link Runnable} to execute once all threads have completed.
     *                   If null, the method blocks until all threads are done. If non-null, the method
     *                   returns immediately, and the {@code Runnable} is executed asynchronously
     *                   after thread completion.
     * @param threads    An array of {@link NanoThread} instances to wait for.
     * @return The same array of {@link NanoThread} instances, allowing for method chaining or further processing.
     */
    public static NanoThread[] waitFor(final Runnable onComplete, final NanoThread... threads) {

        final CountDownLatch latch = new CountDownLatch(threads.length);
        for (final NanoThread thread : threads) {
            thread.onComplete((nt, error) -> {
                latch.countDown();
                if (latch.getCount() <= 0 && onComplete != null) {
                    onComplete.run();
                }
            });
        }

        if (onComplete == null) {
            try {
                //TODO configurable timeout
                final boolean completed = latch.await(10, TimeUnit.SECONDS);
                if (!completed) {
                    System.err.println(new Date() + " [FATAL] Threads did no complete");
                }
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return threads;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
            "onCompleteCallbacks=" + onCompleteCallbacks.size() +
            ", context=" + (context != null) +
            ", isComplete=" + isComplete() +
            '}';
    }
}
