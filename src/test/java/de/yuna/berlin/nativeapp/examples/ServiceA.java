package de.yuna.berlin.nativeapp.examples;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.core.model.Unhandled;

import java.util.function.Supplier;

public class ServiceA extends Service {

    public ServiceA() {
        super(null, true);
    }

    @Override
    public void start(final Supplier<Context> context) {
        logger.info(this::name);
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop(final Supplier<Context> context) {

    }

    @Override
    public Object onFailure(final Unhandled error) {
        return null;
    }

}
