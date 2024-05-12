package berlin.yuna.nano.services.metric.logic;

import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.services.http.model.HttpMethod;
import berlin.yuna.nano.services.http.model.HttpObject;
import org.junit.jupiter.api.Test;

import static berlin.yuna.nano.helper.event.model.EventType.EVENT_HTTP_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

class MetricServiceTest {

    @Test
    void testAddMetricsEndpoint() {
        final MetricService metricService = new MetricService();
        Event event = new Event(EVENT_HTTP_REQUEST, Context.createRootContext(), new HttpObject().methodType(HttpMethod.GET).path("/metrics/influx"), null);
        metricService.addMetricsEndpoint(event);
        assertThat(event.responseOpt(HttpObject.class)).isPresent();
        assertThat(((HttpObject) event.payload()).path()).isEqualTo("/metrics/influx");
        assertThat(((HttpObject) event.payload()).path()).isNotEqualTo("/metrics/prometheus");
        assertThat(event.response(HttpObject.class).statusCode()).isEqualTo(200);
    }

    @Test
    void testAnyMetricsEndpoint() {

        Event event= new Event(EVENT_HTTP_REQUEST, Context.createRootContext(), new HttpObject().methodType(HttpMethod.GET).path("/metrics/influx"), null);

        event.payloadOpt(HttpObject.class)
            .filter(HttpObject::isMethodGet)
            .filter(request -> request.pathMatch("/metrics/influx"))
            .ifPresent(request -> request.response().statusCode(200).body("application.listeners 8.0 source=nano ").send(event));

        assertThat(event.responseOpt(HttpObject.class)).isPresent();
        assertThat(event.response(HttpObject.class).statusCode()).isEqualTo(200);
        assertThat(event.response(HttpObject.class).bodyAsString()).isEqualTo("application.listeners 8.0 source=nano ");
    }

}
