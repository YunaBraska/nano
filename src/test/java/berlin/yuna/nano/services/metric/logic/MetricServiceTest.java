package berlin.yuna.nano.services.metric.logic;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.model.HttpMethod;
import berlin.yuna.nano.services.http.model.HttpObject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static berlin.yuna.nano.core.model.Config.*;
import static org.assertj.core.api.Assertions.assertThat;

class MetricServiceTest {

    protected static String serverUrl = "http://localhost:";

    @Test
    void metricEndpointsWithoutBasePath() {
        Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.INFO, CONFIG_LOG_FORMATTER, "console"), new LogQueue(), new MetricService(), new HttpService());

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.GET)
            .path(serverUrl + nano.service(HttpService.class).port() + "/metrics/prometheus")
            .send(nano.newContext(MetricServiceTest.class));

        assertThat(result).isNotNull();
        assertThat(result.bodyAsString()).contains("java_version 21.0");
        assertThat(nano.stop(MetricServiceTest.class).waitForStop().isReady()).isFalse();

    }

    @Test
    void metricEndpointsWithCustomBasePath() {
        Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.INFO, CONFIG_LOG_FORMATTER, "console", CONFIG_METRIC_SERVICE_BASE_PATH, "/custom-metrics"), new LogQueue(), new MetricService(), new HttpService());

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.GET)
            .path(serverUrl + nano.service(HttpService.class).port() + "/custom-metrics/prometheus")
            .send(nano.newContext(MetricServiceTest.class));

        assertThat(result).isNotNull();
        assertThat(result.bodyAsString()).contains("java_version 21.0");
        assertThat(nano.stop(MetricServiceTest.class).waitForStop().isReady()).isFalse();
    }


    @Test
    void metricEndpointsWithPrometheus() {
        Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.INFO, CONFIG_LOG_FORMATTER, "console", CONFIG_METRIC_SERVICE_PROMETHEUS_PATH, "/prometheus"), new LogQueue(), new MetricService(), new HttpService());

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.GET)
            .path(serverUrl + nano.service(HttpService.class).port() + "/prometheus")
            .send(nano.newContext(MetricServiceTest.class));

        assertThat(result).isNotNull();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(nano.stop(MetricServiceTest.class).waitForStop().isReady()).isFalse();
    }

    @Test
    void metricEndpointsWithBasePath() {
        Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.INFO, CONFIG_LOG_FORMATTER, "console", CONFIG_METRIC_SERVICE_BASE_PATH, "/metrics"), new LogQueue(), new MetricService(), new HttpService());

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.GET)
            .path(serverUrl + nano.service(HttpService.class).port() + "/metrics/prometheus")
            .send(nano.newContext(MetricServiceTest.class));

        assertThat(result).isNotNull();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(nano.stop(MetricServiceTest.class).waitForStop().isReady()).isFalse();

    }

    @Test
    void withoutMetricService() {
        Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.INFO, CONFIG_LOG_FORMATTER, "console"), new LogQueue(), new HttpService());

        final HttpObject result = new HttpObject()
            .methodType(HttpMethod.GET)
            .path(serverUrl + nano.service(HttpService.class).port() + "/metrics/prometheus")
            .send(nano.newContext(MetricServiceTest.class));

        assertThat(result).isNotNull();
        assertThat(result.statusCode()).isEqualTo(404);
        assertThat(nano.stop(MetricServiceTest.class).waitForStop().isReady()).isFalse();

    }
}
