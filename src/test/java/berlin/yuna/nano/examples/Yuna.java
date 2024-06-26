package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.metric.logic.MetricService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_FORMATTER;
import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_LEVEL;
import static berlin.yuna.nano.core.model.NanoThread.VIRTUAL_THREAD_POOL;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_SHUTDOWN;

public class Yuna {

    public static void main(final String[] args) throws IOException, InterruptedException {

        HttpServer server = HttpServer.create(new InetSocketAddress(50505), 0);
        server.createContext("/", exchange -> {
            final byte[] response = "Hello World".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close(); // Close the response stream
        });
        server.start();


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
