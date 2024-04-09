package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.core.model.Unhandled;

import java.util.function.Supplier;

public class ServiceB extends Service {

    public ServiceB() {
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
