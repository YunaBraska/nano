package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.logic.HttpClient;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.nano.services.metric.logic.MetricService;
import berlin.yuna.nano.services.metric.model.MetricType;
import berlin.yuna.nano.services.metric.model.MetricUpdate;
import org.junit.jupiter.api.Disabled;

import java.util.Map;

import static berlin.yuna.nano.services.http.HttpService.EVENT_HTTP_REQUEST;
import static berlin.yuna.nano.services.http.model.HttpMethod.GET;
import static berlin.yuna.nano.services.metric.logic.MetricService.EVENT_METRIC_UPDATE;
import static berlin.yuna.nano.services.metric.model.MetricType.GAUGE;
import static berlin.yuna.nano.services.metric.model.MetricType.TIMER_END;
import static berlin.yuna.nano.services.metric.model.MetricType.TIMER_START;

@Disabled
@SuppressWarnings("java:S1854") // Suppress "dead code" warning
public class MetricCreation {

    public static void main(final String[] args) {
        final Context context = new Nano(args, new MetricService()).context(MetricCreation.class);
        final Map<String, String> metricTags = Map.of("tag_key", "tag_value");

        // create counter
        context.sendEvent(EVENT_METRIC_UPDATE, new MetricUpdate(GAUGE, "my.counter.key", 130624, metricTags));
        // create gauge
        context.sendEvent(EVENT_METRIC_UPDATE, new MetricUpdate(GAUGE, "my.gauge.key", 200888, metricTags));
        // start timer
        context.sendEvent(EVENT_METRIC_UPDATE, new MetricUpdate(TIMER_START, "my.timer.key", null, metricTags));
        // end timer
        context.sendEvent(EVENT_METRIC_UPDATE, new MetricUpdate(TIMER_END, "my.timer.key", null, metricTags));
    }
}
