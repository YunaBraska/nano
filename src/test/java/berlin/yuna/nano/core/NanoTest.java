package berlin.yuna.nano.core;

import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.helper.logger.logic.LogQueue;
import berlin.yuna.nano.helper.logger.model.LogLevel;
import berlin.yuna.nano.model.TestService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static berlin.yuna.nano.core.config.TestConfig.TEST_LOG_LEVEL;
import static berlin.yuna.nano.core.config.TestConfig.TEST_REPEAT;
import static berlin.yuna.nano.core.config.TestConfig.TEST_TIMEOUT;
import static berlin.yuna.nano.core.config.TestConfig.await;
import static berlin.yuna.nano.core.config.TestConfig.waitForStartUp;
import static berlin.yuna.nano.core.model.Context.APP_HELP;
import static berlin.yuna.nano.core.model.Context.APP_PARAMS;
import static berlin.yuna.nano.core.model.Context.CONFIG_LOG_LEVEL;
import static berlin.yuna.nano.core.model.Context.CONFIG_PARALLEL_SHUTDOWN;
import static berlin.yuna.nano.core.model.Context.CONFIG_PROFILES;
import static berlin.yuna.nano.core.model.Context.CONTEXT_CLASS_KEY;
import static berlin.yuna.nano.core.model.Context.CONTEXT_LOG_QUEUE_KEY;
import static berlin.yuna.nano.core.model.Context.CONTEXT_NANO_KEY;
import static berlin.yuna.nano.core.model.Context.CONTEXT_PARENT_KEY;
import static berlin.yuna.nano.core.model.Context.CONTEXT_TRACE_ID_KEY;
import static berlin.yuna.nano.core.model.Context.EVENT_APP_SHUTDOWN;
import static berlin.yuna.nano.core.model.Context.EVENT_APP_UNHANDLED;
import static berlin.yuna.nano.helper.NanoUtils.waitForCondition;
import static berlin.yuna.nano.model.TestService.TEST_EVENT;
import static berlin.yuna.nano.services.http.HttpService.EVENT_HTTP_REQUEST;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
class NanoTest {

    @Test
    void configFilesTest() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        assertThat(nano.context().get(String.class, CONFIG_PROFILES)).isEqualTo("default, local, dev, prod");
        assertThat(nano.context().getList(String.class, "_scanned_profiles")).containsExactly("local", "default", "dev", "prod");
        assertThat(nano.context().get(String.class, "test_placeholder_fallback")).isEqualTo("fallback should be used 1");
        assertThat(nano.context().get(String.class, "test_placeholder_key_empty")).isEqualTo("fallback should be used 2");
        assertThat(nano.context().get(String.class, "test_placeholder_value")).isEqualTo("used placeholder value");
        assertThat(nano.context().get(String.class, "resource_key1")).isEqualTo("AA");
        assertThat(nano.context().get(String.class, "resource_key2")).isEqualTo("CC");
        assertThat(nano.context()).doesNotContainKey("test_placeholder_fallback_empty");
        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void stopViaMethod() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        assertThat(nano.stop(this.getClass()).waitForStop()).isNotNull().isEqualTo(nano);
    }

    @RepeatedTest(TEST_REPEAT)
    void stopViaEvent() {
        assertThat(new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL))
            .context(this.getClass())
            .sendEvent(EVENT_APP_SHUTDOWN, this)
        ).isNotNull();
    }

    @RepeatedTest(TEST_REPEAT)
    void startMultipleTimes_shouldHaveNoIssues() {
        final TestService service1 = new TestService();
        final TestService service2 = new TestService();
        final Nano nano1 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service1);
        final Nano nano2 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service2);
        waitForStartUp(nano1);
        waitForStartUp(nano2);
        assertThat(nano1).isNotEqualTo(nano2);
        stopAndTestNano(nano1, service1);
        stopAndTestNano(nano2, service2);
    }

    @RepeatedTest(TEST_REPEAT)
    void shutdownServicesInParallelTest_Sync() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(8);
        final TestService testService = new TestService();
        testService.doOnStop(context -> context.tryExecute(latch::countDown));

        final Nano nano1 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), testService, testService, testService, testService);
        final Nano nano2 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, CONFIG_PARALLEL_SHUTDOWN, true), testService, testService, testService, testService);
        waitForStartUp(nano1, 4);
        waitForStartUp(nano2, 4);
        assertThat(nano1.stop(this.getClass())).isEqualTo(nano1);
        assertThat(nano2.stop(this.getClass())).isEqualTo(nano2);
        assertThat(nano1.waitForStop().isReady()).isFalse();
        assertThat(nano2.waitForStop().isReady()).isFalse();
        nano1.shutdown(this.getClass());
        assertThat(await(latch)).isTrue();
    }

    @RepeatedTest(TEST_REPEAT)
    void shutdownServicesInParallelWithExceptionTest() {
        final TestService testService = new TestService();
        testService.doOnStop(context -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, CONFIG_PARALLEL_SHUTDOWN, true), testService).shutdown(this.getClass());
        assertThat(nano).isNotNull();
    }

    @Disabled("No args constructor test is changing the log level of the test. Since the java logger is not stateless, it affects the other tests.")
    @RepeatedTest(TEST_REPEAT)
    void constructorNoArgsTest() {
        final Nano noArgs = new Nano();
        assertThat(noArgs).isNotNull();
        assertThat(noArgs.logger().level()).isEqualTo(LogLevel.DEBUG);
        noArgs.setLogLevel(TEST_LOG_LEVEL);
        assertThat(noArgs.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(noArgs.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withConfigTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(config.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withConfigAndServiceTest() {
        final Nano configAndService = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), new TestService());
        assertThat(configAndService).isNotNull();
        assertThat(configAndService.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        waitForStartUp(configAndService);
        assertThat(configAndService.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withConfigAndLazyServices_Test() {
        final Nano configAndService = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), context -> List.of(new TestService()));
        assertThat(configAndService).isNotNull();
        assertThat(configAndService.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        waitForStartUp(configAndService);
        assertThat(configAndService.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withArgsAndLazyServices_Test() {
        final Nano lazyServices = new Nano(new String[]{"-" + CONFIG_LOG_LEVEL + "=" + TEST_LOG_LEVEL}, context -> List.of(new TestService()));
        assertThat(lazyServices).isNotNull();
        assertThat(lazyServices.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        waitForStartUp(lazyServices);
        lazyServices.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withLazyServices_Test() {
        final Nano lazyServices = new Nano(context -> List.of(new TestService()), null, "-" + CONFIG_LOG_LEVEL + "=" + TEST_LOG_LEVEL);
        assertThat(lazyServices).isNotNull();
        assertThat(lazyServices.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        waitForStartUp(lazyServices);
        lazyServices.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void printParameterTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, APP_PARAMS, true, APP_HELP, true));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(config.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void toStringTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, APP_PARAMS, true));
        assertThat(config).isNotNull();
        assertThat(config.toString()).contains(
            "pid=",
            "schedulers=", "services=", "listeners=",
            "cores=", "usedMemory=",
            "threadsNano=", "threadsActive=", "threadsOther=",
            "java=", "arch=", "os="
        );
        assertThat(config.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void sendEvent_Sync() throws InterruptedException {
        final TestService service = new TestService().doOnEvent(Event::acknowledge);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);
        waitForStartUp(nano);

        // send to first service
        final CountDownLatch latch1 = new CountDownLatch(1);
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 11111111, response -> latch1.countDown(), false);
        assertThat(latch1.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 11111111)).isNotNull();

        // send to first listener (listeners have priority)
        service.resetEvents();
        final CountDownLatch latch2 = new CountDownLatch(1);
        nano.subscribeEvent(TEST_EVENT, Event::acknowledge);
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 22222222, response -> latch2.countDown(), false);
        assertThat(latch2.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(service.getEvent(TEST_EVENT, 256)).isNull();

        // send to all (listener and services)
        service.resetEvents();
        final CountDownLatch latch3 = new CountDownLatch(1);
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 33333333, response -> latch3.countDown(), true);
        assertThat(latch3.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 33333333)).isNotNull();

        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void sendEventWithEventExecutionException_shouldNotInterrupt() {
        final TestService service = new TestService();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.OFF), service);
        waitForStartUp(nano);

        service.doOnEvent(event -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        final Context context = nano.contextEmpty(this.getClass());
        assertThat(context).hasSize(4).containsKeys(CONTEXT_NANO_KEY, CONTEXT_TRACE_ID_KEY, CONTEXT_CLASS_KEY, CONTEXT_PARENT_KEY);

        nano.sendEvent(TEST_EVENT, context, 44444444, result -> {
        }, false);
        assertThat(service.getEvent(EVENT_APP_UNHANDLED, event -> event.payload(Integer.class) != null && event.payload(Integer.class) == 44444444)).isNotNull();
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.stopCount()).isZero();
        assertThat(service.failures()).isNotEmpty();
        assertThat(nano.isReady()).isTrue();

        nano.shutdown(context);
        assertThat(service.stopCount()).isEqualTo(1);
    }

    @RepeatedTest(TEST_REPEAT)
    void addAndRemoveEventListener() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final Consumer<Event> listener = event -> {};

        assertThat(nano.listeners().get(TEST_EVENT)).isNull();
        nano.subscribeEvent(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).hasSize(1);
        nano.unsubscribeEvent(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).isEmpty();

        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void runSchedulers() throws InterruptedException {
        final long timer = 64;
        final CountDownLatch scheduler1Triggered = new CountDownLatch(1);
        final CountDownLatch scheduler2Triggered = new CountDownLatch(1);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));

        nano.run(null, scheduler1Triggered::countDown, timer, MILLISECONDS);
        nano.run(null, scheduler2Triggered::countDown, timer, timer * 2, MILLISECONDS, () -> false);

        assertThat(scheduler1Triggered.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(scheduler2Triggered.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void throwExceptionInsideScheduler() throws InterruptedException {
        final long timer = 64;
        final TestService service = new TestService();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);
        final CountDownLatch trigger = new CountDownLatch(2);
        waitForStartUp(nano);

        nano.run(nano::context, () -> {
            trigger.countDown();
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, MILLISECONDS);

        nano.run(nano::context, () -> {
            trigger.countDown();
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, timer * 2, MILLISECONDS, () -> false);

        assertThat(trigger.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(service.getEvent(EVENT_APP_UNHANDLED, event -> event.payload() != null)).isNotNull();
        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void errorHandlerTest() throws InterruptedException {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final CountDownLatch trigger = new CountDownLatch(2);

        nano.subscribeEvent(EVENT_APP_UNHANDLED, event -> {
            trigger.countDown();
            event.acknowledge();
        });

        // Event with error
        nano.subscribeEvent(EVENT_HTTP_REQUEST, event -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        nano.context(NanoTest.class).sendEvent(EVENT_HTTP_REQUEST, "test");

        // Execution with error
        nano.context(NanoTest.class).run(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        assertThat(trigger.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void setLogQueue() {
        final LogQueue logQueue = new LogQueue();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), logQueue);

        assertThat(nano.context()).containsEntry(CONTEXT_LOG_QUEUE_KEY, logQueue);
        assertThat(nano.context(NanoTest.class)).containsEntry(CONTEXT_LOG_QUEUE_KEY, logQueue);

        waitForStartUp(nano, 1);
        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    private static void stopAndTestNano(final Nano nano, final TestService service) {
        assertThat(nano.isReady()).isTrue();
        assertThat(nano.createdAtMs()).isPositive();
        assertThat(nano.pid()).isPositive();
        assertThat(nano.usedMemoryMB()).isPositive();
        //assertThat(nano.usedMemoryMB()).isLessThan(TEST_REPEAT * 20); // Really hard to configure due  parallel tests
        assertThat(nano.services()).hasSize(1).contains(service);
        assertThat(nano.service(TestService.class)).isEqualTo(service);
        assertThat(nano.services(TestService.class)).hasSize(1).contains(service);
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.failures()).isEmpty();
        assertThat(service.stopCount()).isZero();

        // Stop
        waitForCondition(() -> !nano.services().isEmpty(), TEST_TIMEOUT);
        nano.shutdown(nano.context(NanoTest.class));
        assertThat(nano.isReady()).isFalse();
        assertThat(nano.services()).isEmpty();
        assertThat(nano.listeners()).isEmpty();
        assertThat(nano.threadPool.isTerminated()).isTrue();
        // assertThat(activeCarrierThreads()).isZero(); Not possible due parallel tests
        assertThat(nano.schedulers()).isEmpty();
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.failures()).isEmpty();
        assertThat(service.stopCount()).isEqualTo(1);
    }
}
