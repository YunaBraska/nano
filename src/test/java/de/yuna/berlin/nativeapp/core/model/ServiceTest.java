package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.model.TestService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static de.yuna.berlin.nativeapp.core.NanoTest.TEST_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static de.yuna.berlin.nativeapp.helper.threads.Executor.tryExecute;
import static org.assertj.core.api.Assertions.assertThat;

class ServiceTest {

    @Test
    void testService() {
        final long startTime = System.currentTimeMillis() - 10;
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final Context context = nano.context(this.getClass());
        final TestService service = new TestService(true);
        final Unhandled error = new Unhandled(null, null, null);
        final Event event = new Event(EVENT_APP_UNHANDLED.id(), context, error, null);

        assertThat(service).isNotNull();
        assertThat(service.createdAtMs()).isGreaterThan(startTime);
        assertThat(service.startCount.get()).isEqualTo(0);
        assertThat(service.stopCount.get()).isZero();
        assertThat(service.events).isEmpty();
        assertThat(service.failures).isEmpty();

        service.start(() -> context);
        assertThat(service.startCount.get()).isEqualTo(1);

        service.stop(() -> context);
        assertThat(service.stopCount.get()).isEqualTo(1);

        service.onFailure(error);
        assertThat(service.failures).hasSize(1).contains(error);

        service.onEvent(event);
        assertThat(service.events).hasSize(1).contains(event);

        assertThat(nano.services()).isEmpty();
        service.tryExecuteService(context);
        service.handleServiceException(context, new RuntimeException("Nothing to see here, just a test exception"));
        service.wait(EVENT_APP_UNHANDLED.id());
        tryExecute(() -> Thread.sleep(64)); //Safety cause of async
        assertThat(service.startCount.get()).isEqualTo(2);
        assertThat(service.failures).hasSize(2);
        assertThat(nano.services()).hasSize(1);

        nano.stop(context);
    }
}
