package de.yuna.berlin.nativeapp.helper.logger.logic;

import de.yuna.berlin.nativeapp.core.model.*;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_LOG_QUEUE;

@SuppressWarnings("UnusedReturnValue")
public class LogQueue extends Service {
    protected Future<Void> future;
    protected BlockingQueue<Pair<Logger, LogRecord>> queue;
    protected int queueCapacity;

    public LogQueue() {
        super(null, false);
    }

    public boolean log(final Logger logger, final LogRecord logRecord) {
        if (isReady() && queue != null) {
            try {
                queue.put(new Pair<>(logger, logRecord));
                return true;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    @Override
    public void start(final Supplier<Context> contextSub) {
        if (isReady.compareAndSet(false, true)) {
            final Context context = contextSub.get();
            queueCapacity = context.gett(Config.CONFIG_LOG_QUEUE_SIZE.id(), Integer.class).orElse(1000);
            queue = new LinkedBlockingQueue<>(queueCapacity);
            future = context.nano().execute(this::process);
            context.nano().schedule(this::checkQueueSizeAndWarn, 5, 5, TimeUnit.MINUTES, () -> !isReady());
            context.sendEvent(EVENT_APP_LOG_QUEUE.id(), this, false, false);
        }
    }

    @Override
    public void stop(final Supplier<Context> contextSub) {
        if (isReady.compareAndSet(true, false)) {
            try {
                contextSub.get().sendEvent(EVENT_APP_LOG_QUEUE.id(), null, false, true, true);
                logger.debug(() -> "Shutdown initiated - process last messages [{}]", queue.size());
                queue.put(new Pair<>(logger.logger(), new LogRecord(Level.INFO, "Shutdown Hook")));
                queue = null;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                future.cancel(true);
                future = null;
            }
        }
    }

    @Override
    public void onEvent(final Event event) {
        event.ifPresent(EVENT_APP_LOG_LEVEL.id(), LogLevel.class, level -> {
            logger.logQueue(null);
            logger.level(level);
        });
    }

    @Override
    public Object onFailure(final Unhandled error) {
        return null;
    }

    protected void process() {
        while (isReady() || !queue.isEmpty()) {
            try {
                final Pair<Logger, LogRecord> pair = queue.take();
                if (pair.left() != this.logger.logger()) {
                    pair.left().log(pair.right());
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void checkQueueSizeAndWarn() {
        if (queue != null && isReady.get()) {
            final int size = queue.size();
            final int percentage = size > 0 ? ((int) ((double) size / queueCapacity) * 100) : 0;
            if (percentage > 80) {
                logger.warn(() -> "Warning: Log queue is " + percentage + "% full.");
            }
        }
    }
}



