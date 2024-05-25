package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.metric.logic.MetricService;

import java.io.IOException;
import java.util.Map;

import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_FORMATTER;
import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_LEVEL;

public class Yuna {

    public static void main(final String[] args) throws IOException, InterruptedException {

//
//        // Plain Nano
//        final Nano nano = new Nano();
//
//        // Nano with configuration
//        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.INFO));
//
//        // Nano with startup services
//        final Nano nano = new Nano(new HttpService());
//
//        // Nano adding "Hello World" API
//        final Nano nano = new Nano(new HttpService())
//            .subscribeEvent(EVENT_HTTP_REQUEST, event -> event.payloadOpt(HttpObject.class)
//                .filter(HttpObject::isMethodGet)
//                .filter(request -> request.pathMatch("/hello"))
//                .ifPresent(request -> request.response().body(System.getProperty("user.name")).send(event))
//            );



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

        final Context ctx = application.newContext(Yuna.class);
        ctx.run(serviceA)
            .run(serviceA)
            .runAwait(serviceB, serviceC)
            .runAwait(serviceD)
            .run(serviceD)
            .runAwait(serviceA)
            .runAwait(serviceD)
            .run(serviceD)
            .run(() -> ctx.logger().info(() -> "I was here"))
            .run(() -> {
                try {
                    ctx.logger().info(() -> "START HELLO");
                    Thread.sleep(5000);
                    new RuntimeException("Test Exception");
                    ctx.logger().info(() -> "END WORLD");
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            })
        ;

//        application.run(() -> {
//            final Context context = application.newContext(Yuna.class);
//            context.sendEvent(EVENT_APP_SHUTDOWN, null);
//            context.sendEvent(99, null);
//        }, 5, 5, TimeUnit.SECONDS, () -> false);


    }
}
