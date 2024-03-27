package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.model.TestService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static de.yuna.berlin.nativeapp.core.config.TestConfig.*;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.Context.CONTEXT_LOGGER_KEY;
import static de.yuna.berlin.nativeapp.core.model.Context.CONTEXT_TRACE_ID_KEY;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_HEARTBEAT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SuppressWarnings("java:S5778")
@Execution(ExecutionMode.CONCURRENT)
class ContextTest {

    @RepeatedTest(TEST_REPEAT)
    void testNewContext_withNano() throws InterruptedException {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final Context context = new Context(null, nano, null);
        final Consumer<Event> myListener = event -> {};
        assertContextBehaviour(context);

        //Verify logger
        assertThat(context.logger().logger().getName()).isEqualTo(Context.class.getCanonicalName());
        assertThat(context.setLoggerReturn(Nano.class).logger().getName()).isEqualTo(Nano.class.getCanonicalName());

        //Verify event listener
        assertThat(nano.listeners().get(EVENT_APP_HEARTBEAT)).hasSize(1);
        assertThat(context.addEventListener(EVENT_APP_HEARTBEAT, myListener)).isEqualTo(context);
        assertThat(nano.listeners().get(EVENT_APP_HEARTBEAT)).hasSize(2);
        assertThat(context.removeEventListener(EVENT_APP_HEARTBEAT, myListener)).isEqualTo(context);
        assertThat(nano.listeners().get(EVENT_APP_HEARTBEAT)).hasSize(1);

        //Verify event sending
        final CountDownLatch eventLatch = new CountDownLatch(4);
        final int eventType = context.registerEventType("TEST_EVENT");
        context.addEventListener(eventType, event -> eventLatch.countDown());
        context.sendEvent(eventType, "AA");
        final Event event = context.sendEventReturn(eventType, "BB");
        context.broadcastEvent(eventType, "CC");
        context.broadcastEventReturn(eventType, "DD");
        assertThat(event).isNotNull();
        assertThat(event.payload()).isEqualTo("BB");
        assertThat(event.name()).isEqualTo("TEST_EVENT");
        assertThat(event.id()).isEqualTo(eventType);
        assertThat(event.context()).isEqualTo(context);
        assertThat(event.isAcknowledged()).isFalse();
        assertThat(eventLatch.await(1000, MILLISECONDS)).isTrue();
        assertThat(eventLatch.getCount()).isZero();
        assertThat(context.eventIdOf("TEST_EVENT")).contains(eventType);
        assertThat(context.eventNameOf(eventType)).isEqualTo("TEST_EVENT");

        //Verify services
        final TestService testService = new TestService();
        assertThat(context.async(testService)).isEqualTo(context);
        assertThat(waitForCondition(() -> context.services().contains(testService))).isTrue();
        assertThat(context.service(testService.getClass())).isEqualTo(testService);
        assertThat(context.services(TestService.class)).containsExactly(testService);

        nano.stop(this.getClass());
        //TODO: bring schedulers to context
    }

    @RepeatedTest(TEST_REPEAT)
    void testNewContext_withoutNano() {
        final Context context = Context.createRootContext();
        final Consumer<Event> myListener = event -> {};
        assertContextBehaviour(context);
        assertThatThrownBy(() -> context.addEventListener(EVENT_APP_HEARTBEAT, myListener)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> context.removeEventListener(EVENT_APP_HEARTBEAT, myListener)).isInstanceOf(NullPointerException.class);
    }

    @RepeatedTest(TEST_REPEAT)
    void testNewEmptyContext_withoutClass_willCreateRootContext() {
        final Context context = Context.createRootContext();
        assertContextBehaviour(context);
        final Context subContext = context.newEmptyContext(null, null);
        assertThat(subContext.traceId()).startsWith("RootContext/");
    }

    @RepeatedTest(TEST_REPEAT)
    void testAsyncHandled_withException() throws InterruptedException {
        final Context context = Context.createRootContext();
        final CountDownLatch latch = new CountDownLatch(2);
        assertContextBehaviour(context);
        assertThat(context.asyncHandled(unhandled -> latch.countDown(), xtx -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        })).isEqualTo(context);
        assertThat(context.asyncReturnHandled(unhandled -> latch.countDown(), xtx -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        })).isNotNull();
        assertThat(latch.await(1000, MILLISECONDS)).isTrue();
        assertThat(latch.getCount()).isZero();
    }


    @RepeatedTest(TEST_REPEAT)
    void testAsyncAwaitHandled_withException() throws InterruptedException {
        final Context context = Context.createRootContext();
        final CountDownLatch latch = new CountDownLatch(1);
        assertContextBehaviour(context);
        context.asyncAwaitHandled(unhandled -> latch.countDown(), xtx -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });
        assertThat(latch.await(1000, MILLISECONDS)).isTrue();
        assertThat(latch.getCount()).isZero();
    }

    @RepeatedTest(TEST_REPEAT)
    void testAsyncAwait_withException() {
        final Context context = Context.createRootContext();
        assertContextBehaviour(context);
        context.asyncAwait(ctx -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });
        //TODO: create an unhandled element and check if the error was unhandled
    }

    private void assertContextBehaviour(final Context context) {
        assertThat(context)
            .hasSize(1)
            .containsKey(CONTEXT_TRACE_ID_KEY);

        context.put("AA", "BB");
        assertThat(context)
            .hasSize(2)
            .containsKey(CONTEXT_TRACE_ID_KEY)
            .containsKey("AA");

        assertThat(context.newContext(this.getClass()))
            .hasSize(3)
            .containsKey(CONTEXT_TRACE_ID_KEY)
            .containsKey(CONTEXT_LOGGER_KEY)
            .containsKey("AA");

        assertThat(context.newEmptyContext(this.getClass()))
            .hasSize(2)
            .containsKey(CONTEXT_TRACE_ID_KEY)
            .containsKey(CONTEXT_LOGGER_KEY);

        //Verify trace id is shared between contexts
        assertThat(context.newContext(this.getClass()).getList(CONTEXT_TRACE_ID_KEY)).hasSize(2).contains(context.traceId());
        final Context subContext = context.newEmptyContext(this.getClass());
        assertThat(subContext.getList(CONTEXT_TRACE_ID_KEY)).hasSize(2).contains(context.traceId());
        assertThat(subContext.traceId()).isNotEqualTo(context.traceId());
        assertThat(subContext.traceId(0)).isEqualTo(context.traceId()).isNotEqualTo(subContext.traceId());
        assertThat(subContext.traceId(1)).isEqualTo(subContext.traceId()).isNotEqualTo(context.traceId());
        assertThat(subContext.traceIds()).containsExactlyInAnyOrder(context.traceId(), subContext.traceId());
        assertThat(subContext.logger()).isNotNull();
        assertThat(subContext.logger().level()).isNotNull().isEqualTo(subContext.logLevel());
        assertThat(subContext.logger().logQueue()).isNull();
    }


}
