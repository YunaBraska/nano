package de.yuna.berlin.nativeapp.examples;

import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.helper.logger.logic.LogQueue;
import de.yuna.berlin.nativeapp.services.ShortTask;
import de.yuna.berlin.nativeapp.services.http.HttpService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_FORMATTER;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_THREAD_POOL_ALIVE_MS;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SHUTDOWN;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.INFO;

public class App {

    public static void main(final String[] args) {
        //TODO: Metrics
        //TODO: Dynamic Queues to Services
        //TODO: Dynamic Messages to Services
        //TODO: Chain & Async & Functional programming like streams/optionals - allow users to spawn and chain functions everywhere & send events from everywhere
        final Nano application = new Nano(Map.of(
                CONFIG_LOG_LEVEL, INFO,
                CONFIG_THREAD_POOL_ALIVE_MS, 1,
                CONFIG_LOG_FORMATTER, "console"
        ), new LogQueue(), new HttpService(8080));


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
                .async(new ShortTask(c -> c.logger().info(() -> "I was here"), -1, null))
                .async(500, c -> {
                    try {
                        c.logger().info(() -> "START HELLO");
                        Thread.sleep(5000);
                        //TODO: should be interrupted
                        c.logger().info(() -> "END WORLD");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                })
        ;

        application.schedule(() -> {
            final Context context = application.context(App.class);
            context.sendEvent(EVENT_APP_SHUTDOWN.id(), null);
            context.sendEvent(99, null);
        }, 10, 5, TimeUnit.SECONDS, () -> false);
//
    }
}
