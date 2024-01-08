package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.logic.NanoLogger;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_LOG_QUEUE;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SERVICE_REGISTER;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SERVICE_UNREGISTER;
import static de.yuna.berlin.nativeapp.helper.threads.Executor.handleExecutionExceptions;

public abstract class Service {

    protected final String name;
    protected final long createdAtMs;
    protected final long timeoutMs;
    protected final boolean isSilent;
    protected final boolean isLongRunning;
    protected final AtomicBoolean isReady;
    protected final NanoLogger logger = new NanoLogger(this);

    protected Service(final String name, final long timeoutMs, final boolean isLongRunning, final boolean isSilent, final boolean isReady) {
        this.createdAtMs = System.currentTimeMillis();
        this.isReady = new AtomicBoolean(isReady);
        this.name = name != null ? name : this.getClass().getSimpleName();
        this.timeoutMs = timeoutMs > 0 ? timeoutMs : 30000L;
        this.isSilent = isSilent;
        this.isLongRunning = isLongRunning;
    }

    public abstract void start(final Supplier<Context> contextSub);
    public abstract void stop(final Supplier<Context> contextSub);
    public abstract Object onFailure(final Unhandled error);
    public void onEvent(final Event event) {
        event.ifPresent(EVENT_APP_LOG_LEVEL.id(), LogLevel.class, logger::level);
        event.ifPresent(EVENT_APP_LOG_QUEUE.id(), LogQueue.class, logger::logQueue);
    }

    public NanoLogger logger() {
        return logger;
    }

    public String name() {
        return name;
    }

    public boolean isReady() {
        return isReady.get();
    }

    public boolean isSilent() {
        return isSilent;
    }

    public boolean isLongRunning() {
        return isLongRunning;
    }

    public long timeoutMs() {
        return timeoutMs;
    }

    public long createdAtMs() {
        return createdAtMs;
    }

    public Service isReady(final boolean isReady) {
        this.isReady.set(isReady);
        return this;
    }

    //########## GLOBAL SERVICE METHODS ##########

    public CompletableFuture<Void> feature(final Context context) {
        // Execution
        final CompletableFuture<Void> async = context.nano().execute(() -> tryExecuteService(context));
        // Timeouts
        if (!this.isLongRunning()) {
            async.orTimeout(this.timeoutMs(), TimeUnit.MILLISECONDS);
        }
        async.handle((result, ex) -> {
            // Exceptions - should never happen!
            if (ex != null) {
                handleServiceException(context, ex);
            }
            // Unregister Service
            if (!this.isSilent() && (ex != null || !this.isLongRunning())) {
                context.nano().sendEvent(EVENT_APP_SERVICE_UNREGISTER.id(), context, this, null, false, false, true);
            }
            return result;
        });
        return async;
    }

    public void handleServiceException(final Context context, final Throwable exception) {
        try {
            final Unhandled unhandled = new Unhandled(context, this, exception);
            if (this.onFailure(unhandled) == null) {
                handleExecutionExceptions(context, unhandled, () -> "Execution error [" + this.name() + "]");
            }
        } catch (final Exception e) {
            handleExecutionExceptions(context, new Unhandled(context, this, e), () -> "Execution error [" + this.name() + "]");
        }
    }

    protected void tryExecuteService(final Context context) {
        try {
            this.logger().level(context.logLevel());
            this.logger().logQueue(context.nano().logger().logQueue());
            this.start(() -> context);
            if (!this.isSilent())
                context.nano().sendEvent(EVENT_APP_SERVICE_REGISTER.id(), context, this, null, false, false, true);
        } catch (final Exception exception) {
            handleServiceException(context, exception);
        }
    }
}
