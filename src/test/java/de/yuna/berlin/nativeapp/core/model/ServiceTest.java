package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.helper.PrintTestNamesExtension;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.model.TestService;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;

import static de.yuna.berlin.nativeapp.core.config.TestConfig.TEST_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.config.TestConfig.TEST_REPEAT;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.Context.tryExecute;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
class ServiceTest {

    @RepeatedTest(TEST_REPEAT)
    void testService() {
        final long startTime = System.currentTimeMillis() - 10;
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final Context context = nano.context(this.getClass());
        final TestService service = new TestService();
        final Unhandled error = new Unhandled(null, null, null);

        assertThat(service).isNotNull();
        assertThat(service.createdAtMs()).isGreaterThan(startTime);
        assertThat(service.startCount()).isEqualTo(0);
        assertThat(service.stopCount()).isZero();
        assertThat(service.events()).isEmpty();
        assertThat(service.failures()).isEmpty();

        service.start(() -> context);
        assertThat(service.startCount()).isEqualTo(1);

        service.stop(() -> context);
        assertThat(service.stopCount()).isEqualTo(1);

        service.onFailure(error);
        assertThat(service.failures()).hasSize(1).contains(error);

        final Event event = new Event(EVENT_APP_UNHANDLED.id(), context, error, null);
        service.onEvent(event);
        assertThat(service.getEvent(EVENT_APP_UNHANDLED.id())).isNotNull().has(new Condition<>(e -> e.payload(Unhandled.class) == error, "Should contain payload with error"));

        assertThat(nano.services()).isEmpty();
        service.nanoThread(context).execute(() -> {});
        service.handleServiceException(context, new RuntimeException("Nothing to see here, just a test exception"));
        tryExecute(() -> Thread.sleep(64)); //Safety cause of async
        assertThat(service.startCount()).isEqualTo(2);
        assertThat(service.failures()).hasSize(2);
        assertThat(nano.services()).hasSize(1);

        nano.stop(context);
    }
}
