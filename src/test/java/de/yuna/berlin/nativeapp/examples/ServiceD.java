package de.yuna.berlin.nativeapp.examples;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.core.model.Unhandled;

import java.util.function.Supplier;

public class ServiceD extends Service {

    public ServiceD() {
        super(null, 10000, false, false,true);
    }

    @Override
    public void start(final Supplier<Context> context) {
        logger.info(this::name);
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
//        throw new RuntimeException("Error AA");
    }

    @Override
    public void stop(final Supplier<Context> context) {

    }

    @Override
    public Object onFailure(final Unhandled error) {
        return null;
    }
}
