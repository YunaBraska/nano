package de.yuna.berlin.nativeapp.services;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.core.model.Unhandled;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShortTask extends Service {

    protected final Consumer<Context> runner;
    protected final Consumer<Unhandled> onFailure;

    public ShortTask(final Consumer<Context> runner) {
        this(runner, -1, null);
    }

    public ShortTask(final Consumer<Context> runner, final long timeoutMs, final Consumer<Unhandled> onFailure) {
        super(null, timeoutMs, false, true, false);
        this.runner = runner;
        this.onFailure = onFailure;
    }

    @Override
    public void start(final Supplier<Context> contextSub) {
        if (runner != null) {
            runner.accept(contextSub.get().setLogger(this.getClass()));
        }
        isReady(true);
    }

    @Override
    public void stop(final Supplier<Context> contextSub) {
        isReady.compareAndSet(true, false);
    }

    @Override
    public Object onFailure(final Unhandled error) {
        if (onFailure != null) {
            onFailure.accept(error);
            return true;
        }
        return null;
    }
}
