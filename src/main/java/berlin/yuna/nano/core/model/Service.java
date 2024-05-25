package berlin.yuna.nano.core.model;

import berlin.yuna.nano.helper.LockedBoolean;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.logic.NanoLogger;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.metric.model.MetricType;
import berlin.yuna.nano.services.metric.model.MetricUpdate;

import java.util.Map;
import java.util.function.Supplier;

import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_APP_LOG_LEVEL;
import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_APP_LOG_QUEUE;
import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_APP_SERVICE_REGISTER;
import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_METRIC_UPDATE;
import static java.util.Arrays.stream;

public abstract class Service {

    protected final String name;
    protected final long createdAtMs;
    protected final LockedBoolean isReady;
    protected final NanoLogger logger = new NanoLogger(this);

    protected Service(final String name, final boolean isReady) {
        this.createdAtMs = System.currentTimeMillis();
        this.isReady = new LockedBoolean(isReady);
        this.name = name != null ? name : this.getClass().getSimpleName();
    }

    public abstract void start(final Supplier<Context> contextSub);

    public abstract void stop(final Supplier<Context> contextSub);

    public abstract Object onFailure(final Unhandled error);

    public void onEvent(final Event event) {
        event.ifPresent(EVENT_APP_LOG_LEVEL, LogLevel.class, logger::level);
        event.ifPresent(EVENT_APP_LOG_QUEUE, LogQueue.class, logger::logQueue);
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

    //########## GLOBAL SERVICE METHODS ##########
    public NanoThread nanoThread(final Context context) {
        return new NanoThread().run(context.nano() != null ? context.nano().threadPool() : null, () -> context.nano() != null ? context : null, () -> {
            final long startTime = System.currentTimeMillis();
            this.logger().level(context.logLevel());
            this.logger().logQueue(context.nano().logger().logQueue());
            this.start(() -> context);
            context.nano().sendEvent(EVENT_APP_SERVICE_REGISTER, context, this, null, true);
            context.sendEvent(EVENT_METRIC_UPDATE, new MetricUpdate(MetricType.GAUGE, "application.services.ready.time", System.currentTimeMillis() - startTime, Map.of("class", this.getClass().getSimpleName())), result -> {});
        }).onComplete((nanoThread, error) -> {
            if (error != null)
                handleServiceException(context, error);
        });
    }

    public void handleServiceException(final Context context, final Throwable exception) {
        try {
            final Unhandled unhandled = new Unhandled(context, this, exception);
            if (this.onFailure(unhandled) == null) {
                Context.handleExecutionExceptions(context, unhandled, () -> "Execution error [" + this.name() + "]");
            }
        } catch (final Exception e) {
            Context.handleExecutionExceptions(context, new Unhandled(context, this, e), () -> "Execution error [" + this.name() + "]");
        }
    }

    public static NanoThread[] threadsOf(final Context context, final Service... services) {
        return stream(services).map(service -> service.nanoThread(context)).toArray(NanoThread[]::new);
    }
}
