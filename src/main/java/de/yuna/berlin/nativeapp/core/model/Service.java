package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.logic.NanoLogger;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static de.yuna.berlin.nativeapp.helper.event.model.EventType.*;
import static de.yuna.berlin.nativeapp.helper.threads.Executor.handleExecutionExceptions;

public abstract class Service {

    protected final String name;
    protected final long createdAtMs;
    protected final AtomicBoolean isReady;
    protected final NanoLogger logger = new NanoLogger(this);

    protected Service(final String name, final boolean isReady) {
        this.createdAtMs = System.currentTimeMillis();
        this.isReady = new AtomicBoolean(isReady);
        this.name = name != null ? name : this.getClass().getSimpleName();
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

    public long createdAtMs() {
        return createdAtMs;
    }

    public Service isReady(final boolean isReady) {
        this.isReady.compareAndSet(!isReady, isReady);
        return this;
    }

    //########## GLOBAL SERVICE METHODS ##########
    public NanoThread nanoThread(final Context context) {
        return new NanoThread().execute(context.nano() != null ? context.nano().threadPool() : null, () -> {
            this.logger().level(context.logLevel());
            this.logger().logQueue(context.nano().logger().logQueue());
            this.start(() -> context);
            context.nano().sendEvent(EVENT_APP_SERVICE_REGISTER.id(), context, this, null, false, false, true);
        }).onComplete((nanoThread, error) -> {
            // context.nano().sendEvent(EVENT_APP_SERVICE_UNREGISTER.id(), context, this, null, false, false, true);
            if (error != null)
                handleServiceException(context, error);
        });
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
}
