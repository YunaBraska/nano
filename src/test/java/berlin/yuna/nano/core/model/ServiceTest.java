package berlin.yuna.nano.core.model;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.config.TestConfig;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.model.TestService;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;

import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_LEVEL;
import static berlin.yuna.nano.helper.NanoUtils.waitForCondition;
import static berlin.yuna.nano.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
class ServiceTest {

    @RepeatedTest(TestConfig.TEST_REPEAT)
    void testService() {
        final long startTime = System.currentTimeMillis() - 10;
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TestConfig.TEST_LOG_LEVEL));
        final Context context = nano.newContext(this.getClass());
        final TestService service = new TestService();
        final Unhandled error = new Unhandled(null, null, null);

        assertThat(service).isNotNull();
        assertThat(service.createdAtMs()).isGreaterThan(startTime);
        assertThat(service.startCount()).isZero();
        assertThat(service.stopCount()).isZero();
        assertThat(service.events()).isEmpty();
        assertThat(service.failures()).isEmpty();

        service.start(() -> context);
        assertThat(service.startCount()).isEqualTo(1);

        service.stop(() -> context);
        assertThat(service.stopCount()).isEqualTo(1);

        service.onFailure(error);
        assertThat(service.failures()).hasSize(1).contains(error);

        final Event event = new Event(EVENT_APP_UNHANDLED, context, error, null);
        service.onEvent(event);
        assertThat(service.getEvent(EVENT_APP_UNHANDLED)).isNotNull().has(new Condition<>(e -> e.payload(Unhandled.class) == error, "Should contain payload with error"));

        assertThat(nano.services()).isEmpty();
        service.nanoThread(context).run(null, () -> context, () -> {});
        service.handleServiceException(context, new RuntimeException("Nothing to see here, just a test exception"));
        assertThat(waitForCondition(() -> service.startCount() == 2, TestConfig.TEST_TIMEOUT)).isTrue();
        waitForCondition(() -> nano.services().size() == 1, TestConfig.TEST_TIMEOUT);
        assertThat(service.startCount()).isEqualTo(2);
        assertThat(service.failures()).hasSize(2);
        assertThat(nano.services()).size().isEqualTo(1);

        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }
}
