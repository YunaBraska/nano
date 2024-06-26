package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.metric.logic.MetricService;

import java.util.Map;

import static berlin.yuna.nano.core.model.Config.*;

public class Kazim {

    public static void main(String[] args) {
        final Nano application = new Nano(Map.of(
            CONFIG_LOG_LEVEL, LogLevel.INFO,
            CONFIG_LOG_FORMATTER, "console"
//            CONFIG_METRIC_SERVICE_BASE_PATH, "/metrics",
//            CONFIG_METRIC_SERVICE_PROMETHEUS_PATH, "/influx"
        ), new LogQueue(), new MetricService(), new HttpService());

    }
}
