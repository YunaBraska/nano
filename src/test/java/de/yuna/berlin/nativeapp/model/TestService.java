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
import java.util.function.Function;
import java.util.function.Supplier;

import static de.yuna.berlin.nativeapp.core.config.TestConfig.*;
import static de.yuna.berlin.nativeapp.core.model.Context.tryExecute;
import static java.util.Optional.ofNullable;

public class TestService extends Service {

    private final AtomicInteger startCount = new AtomicInteger(0);
    private final AtomicInteger stopCount = new AtomicInteger(0);
    private final List<Unhandled> failures = new CopyOnWriteArrayList<>();
    private final List<Event> events = new CopyOnWriteArrayList<>();
    private final AtomicReference<Consumer<Event>> doOnEvent = new AtomicReference<>();
    private final AtomicReference<Consumer<Unhandled>> failureConsumer = new AtomicReference<>();
    private final AtomicReference<Consumer<Context>> startConsumer = new AtomicReference<>();
    private final AtomicReference<Consumer<Context>> stopConsumer = new AtomicReference<>();
    private long startTime = System.currentTimeMillis();
    public static int TEST_EVENT = EventTypeRegister.registerEventType("TEST_EVENT");

    public TestService() {
        super(null, false);
    }

    public TestService resetEvents() {
        events.clear();
        return this;
    }

    public List<Event> events(final int eventId) {
        getEvent(eventId);
        return events.stream().filter(event -> event.id() == eventId).toList();
    }

    public Event getEvent(final int eventId) {
        return getEvent(eventId, null, 2000);
    }

    public Event getEvent(final int eventId, final Function<Event, Boolean> condition) {
        return getEvent(eventId, condition, TEST_TIMEOUT);
    }

    public Event getEvent(final int eventId, final long timeoutMs) {
        return getEvent(eventId, null, timeoutMs);
    }

    public Event getEvent(final int eventId, final Function<Event, Boolean> condition, final long timeoutMs) {
        final AtomicReference<Event> result = new AtomicReference<>();
        waitForCondition(
            () -> {
                final Event event1 = events.stream()
                    .filter(event -> event.id() == eventId)
                    .filter(event -> condition != null ? condition.apply(event) : true)
                    .findFirst()
                    .orElse(null);
                if (event1 != null)
                    result.set(event1);
                return event1 != null;
            }
            , timeoutMs
        );
        return result.get();
    }

    public int startCount() {
        return startCount.get();
    }

    public int stopCount() {
        return stopCount.get();
    }

    public List<Unhandled> failures() {
        return failures;
    }

    public List<Event> events() {
        return events;
    }

    public Consumer<Event> doOnEvent() {
        return doOnEvent.get();
    }

    public TestService doOnEvent(final Consumer<Event> onEvent) {
        this.doOnEvent.set(onEvent);
        return this;
    }

    public Consumer<Unhandled> doOnFailure() {
        return failureConsumer.get();
    }

    public TestService doOnFailure(final Consumer<Unhandled> onFailure) {
        this.failureConsumer.set(onFailure);
        return this;
    }

    public Consumer<Context> doOnStart() {
        return startConsumer.get();
    }

    public TestService doOnStart(final Consumer<Context> onStart) {
        this.startConsumer.set(onStart);
        return this;
    }

    public Consumer<Context> doOnStop() {
        return stopConsumer.get();
    }

    public TestService doOnStop(final Consumer<Context> onStop) {
        this.stopConsumer.set(onStop);
        return this;
    }

    public long getStartTime() {
        return startTime;
    }

    // ########## DEFAULT METHODS ##########
    @Override
    public void start(final Supplier<Context> contextSub) {
        isReady.set(false, true, state -> startTime = System.currentTimeMillis());
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
        ofNullable(doOnEvent.get()).ifPresent(consumer -> consumer.accept(event));
    }

    public static <T> T waitFor(final Supplier<T> waitFor) {
        return waitFor(waitFor, 2000);
    }

    public static <T> T waitFor(final Supplier<T> waitFor, final long timeoutMs) {
        final long startTime = System.currentTimeMillis();
        final AtomicReference<T> result = new AtomicReference<>(null);
        while (result.get() == null && (System.currentTimeMillis() - startTime) < timeoutMs) {
            ofNullable(waitFor.get()).ifPresentOrElse(result::set, () -> tryExecute(() -> Thread.sleep(100)));
        }
        return result.get();
    }

}
