package de.yuna.berlin.nativeapp.examples;

import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;
import de.yuna.berlin.nativeapp.services.http.HttpService;
import de.yuna.berlin.nativeapp.services.metric.logic.MetricService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_FORMATTER;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.NanoThread.activeCarrierThreads;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SHUTDOWN;

public class App {

    public static void main(final String[] args) {
        //TODO: Dynamic Queues to Services
        //TODO: Dynamic Messages to Services
        //TODO: Chain & Async & Functional programming like streams/optionals - allow users to spawn and chain functions everywhere & send events from everywhere
        //TODO: support internationalization (logRecord.setResourceBundle(javaLogger.getResourceBundle());, logRecord.setResourceBundleName(javaLogger.getResourceBundleName()))
        final Nano application = new Nano(Map.of(
            CONFIG_LOG_LEVEL, LogLevel.INFO,
            CONFIG_LOG_FORMATTER, "console"
        ), new LogQueue(), new MetricService(), new HttpService());

        final Service serviceA = new ServiceA();
        final Service serviceB = new ServiceB();
        final Service serviceC = new ServiceC();
        final Service serviceD = new ServiceD();

        application.context(App.class)
            .async(serviceA)
            .async(serviceA)
            .asyncAwait(serviceB, serviceC)
            .asyncAwait(serviceD)
            .async(serviceD)
            .asyncAwait(serviceA)
            .asyncAwait(serviceD)
            .async(serviceD)
            .async(c -> c.logger().info(() -> "I was here"))
            .async(c -> {
                try {
                    c.logger().info(() -> "START HELLO");
                    Thread.sleep(5000);
                    new RuntimeException("Test Exception");
                    c.logger().info(() -> "END WORLD");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            })
        ;

        application.schedule(() -> {
            final Context context = application.context(App.class);
            context.sendEvent(EVENT_APP_SHUTDOWN.id(), null);
            context.sendEvent(99, null);
        }, 5, 5, TimeUnit.SECONDS, () -> false);


    }
}
