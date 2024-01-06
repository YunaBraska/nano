package de.yuna.berlin.nativeapp.model;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.core.model.Unhandled;
import de.yuna.berlin.nativeapp.helper.event.EventTypeRegister;
import de.yuna.berlin.nativeapp.helper.event.model.Event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class TestService extends Service {

    public final AtomicInteger startCount = new AtomicInteger(0);
    public final AtomicInteger stopCount = new AtomicInteger(0);
    public final List<Unhandled> failures = new CopyOnWriteArrayList<>();
    public final List<Event> events = new CopyOnWriteArrayList<>();
    public final AtomicReference<Consumer<Event>> eventConsumer = new AtomicReference<>();
    public final AtomicReference<Consumer<Unhandled>> failureConsumer = new AtomicReference<>();
    public final AtomicReference<Consumer<Context>> startConsumer = new AtomicReference<>();
    public final AtomicReference<Consumer<Context>> stopConsumer = new AtomicReference<>();
    public static int TEST_EVENT = EventTypeRegister.registerEventType("TEST_EVENT");

    public TestService(final boolean isLongRunning) {
        super(null, -1, isLongRunning, false, true);
    }

    public List<Integer> eventIds() {
        return events.stream().map(Event::id).toList();
    }

    public List<String> eventNames() {
        return events.stream().map(Event::name).toList();
    }

    public TestService wait(final int eventId) {
        final long startTime = System.currentTimeMillis();
        while (!eventIds().contains(eventId) && (System.currentTimeMillis() - startTime) < 2000) {
            try {
                Thread.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        if (!eventIds().contains(eventId)) {
            throw new RuntimeException("Timeout waiting for event");
        }
        return this;
    }

    @Override
    public void start(final Supplier<Context> contextSub) {
        startCount.incrementAndGet();
        if (startConsumer.get() != null)
            startConsumer.get().accept(contextSub.get());
    }

    @Override
    public void stop(final Supplier<Context> contextSub) {
        stopCount.incrementAndGet();
        if (stopConsumer.get() != null)
            stopConsumer.get().accept(contextSub.get());
    }

    @Override
    public Object onFailure(final Unhandled error) {
        failures.add(error);
        ofNullable(failureConsumer.get()).ifPresent(consumer -> consumer.accept(error));
        return null;
    }

    @Override
    public void onEvent(final Event event) {
        events.add(event);
        ofNullable(eventConsumer.get()).ifPresent(consumer -> consumer.accept(event));
    }
}
