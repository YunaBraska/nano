package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.helper.ExRunnable;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

public class NanoThread {

    protected final List<BiConsumer<NanoThread, Throwable>> onCompleteCallbacks = new CopyOnWriteArrayList<>();
    protected final Context context;
    protected final AtomicBoolean isComplete = new AtomicBoolean();

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

    public NanoThread onComplete(final BiConsumer<NanoThread, Throwable> onComplete) {
        synchronized (this) {
            if (!isComplete.get()) {
                onCompleteCallbacks.add(onComplete);
            } else {
                onComplete.accept(this, null);
            }
        }
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
                onCompleteCallbacks.forEach(onComplete -> onComplete.accept(this, null));
            } catch (final Throwable error) {
                //TODO: handle OutOfMemory
                //TODO: handle InternalError
                //TODO: create an unhandled element and check if the error was unhandled
                onCompleteCallbacks.forEach(onComplete -> onComplete.accept(this, error));
                if (context != null && onCompleteCallbacks.isEmpty()) {
                    // TODO: send unhandled event
                    context.logger().error(() -> "Unhandled Exception", error);
                }
            } finally {
                isComplete.set(true);
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
            .filter(info -> (info != null && info.getThreadName().startsWith("CarrierThread"))
                || (info != null && info.getLockName() != null && info.getLockName().startsWith("java.lang.VirtualThread"))) // Assuming CarrierThread naming convention
            .count();
    }

    public static NanoThread[] waitFor(final NanoThread... threads) {
        return waitFor(null, threads);
    }

    /**
     * Waits for the completion of all provided NanoThread instances. If an onDone callback is provided,
     * it executes the callback after all threads have completed without blocking; otherwise, it blocks
     * until all threads have finished.
     *
     * @param onComplete Optional callback to run after all threads have completed. If null, the method blocks.
     * @param threads    Array of NanoThread instances to wait for.
     * @return The array of NanoThread instances passed in.
     */
    public static NanoThread[] waitFor(final Runnable onComplete, final NanoThread... threads) {
        final CountDownLatch latch = new CountDownLatch(threads.length);

        for (final NanoThread thread : threads) {
            thread.onComplete((nt, th) -> {
                latch.countDown();
                if (latch.getCount() == 0 && onComplete != null) {
                    onComplete.run();
                }
            });
        }

        if (onComplete == null) {
            try {
                latch.await();
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        return threads;
    }

}
