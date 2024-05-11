package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.metric.logic.MetricService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_FORMATTER;
import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_LEVEL;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_SHUTDOWN;

public class Kazim {

    public static void main(String[] args) {
        final Nano application = new Nano(Map.of(
            CONFIG_LOG_LEVEL, LogLevel.INFO,
            CONFIG_LOG_FORMATTER, "console"
        ), new LogQueue(), new MetricService(), new HttpService());

//        application.run(() -> {
//            final Context context = application.newContext(Kazim.class);
//            context.sendEvent(EVENT_APP_SHUTDOWN, null);
//            context.sendEvent(99, null);
//        }, 5, 5, TimeUnit.SECONDS, () -> false);
    }
}
