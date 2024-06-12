package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.metric.logic.MetricService;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.util.Map;

import static berlin.yuna.nano.core.model.Context.CONFIG_LOG_FORMATTER;
import static berlin.yuna.nano.core.model.Context.CONFIG_LOG_LEVEL;

@Disabled
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
        //TODO: support internationalization (logRecord.setResourceBundle(javaLogger.getResourceBundle());, logRecord.setResourceBundleName(javaLogger.getResourceBundleName()))
        final Nano application = new Nano(Map.of(
            CONFIG_LOG_LEVEL, LogLevel.INFO,
            CONFIG_LOG_FORMATTER, "console"
        ), new LogQueue(), new MetricService(), new HttpService());


    }
}
