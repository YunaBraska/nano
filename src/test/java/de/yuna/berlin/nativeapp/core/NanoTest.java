package de.yuna.berlin.nativeapp.core;

import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Unhandled;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;
import de.yuna.berlin.nativeapp.model.TestService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static de.yuna.berlin.nativeapp.core.config.TestConfig.*;
import static de.yuna.berlin.nativeapp.core.model.Config.*;
import static de.yuna.berlin.nativeapp.core.model.Context.*;
import static de.yuna.berlin.nativeapp.helper.NanoUtils.waitForCondition;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SHUTDOWN;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static de.yuna.berlin.nativeapp.model.TestService.TEST_EVENT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
class NanoTest {

    //TODO: Move Service getters to Context
    //TODO: Move Schedulers getters to Context
    //TODO: Take care of long stack traces
    //TODO: Logger: change format on runtime
    //TODO: Logger exclude package pattern config
    //TODO: extract logger as a service

    @RepeatedTest(TEST_REPEAT)
    void stopViaMethod() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        assertThat(nano.stop(this.getClass()).waitForStop()
        ).isNotNull().isEqualTo(nano);
    }

    @RepeatedTest(TEST_REPEAT)
    void stopViaEvent() {
        assertThat(new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL))
            .newContext(this.getClass())
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
        testService.doOnStop(context -> tryExecute(latch::countDown));

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
    void constructor_withLazyServices_Test() {
        final Nano lazyServices = new Nano(context -> List.of(new TestService()), "-" + CONFIG_LOG_LEVEL + "=" + TEST_LOG_LEVEL);
        assertThat(lazyServices).isNotNull();
        assertThat(lazyServices.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        waitForStartUp(lazyServices);
        lazyServices.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void printParameterTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, APP_PARAMS, true));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(config.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

//    @RepeatedTest(TEST_REPEAT)
//    void printHelpMenu() throws Exception {
//        final int statusCode = catchSystemExit(() -> {
//            final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, INFO, APP_HELP, true));
//            assertThat(config).isNotNull();
//            assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
//            config.stop(this.getClass());
//        });
//        assertThat(statusCode).isEqualTo(0);
//    }


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
    void sendEvent_Sync() {
        final List<Object> eventResults = new ArrayList<>();
        final TestService service = new TestService().doOnEvent(Event::acknowledge);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);
        waitForStartUp(nano);

        // send to first service
        nano.sendEvent(TEST_EVENT, nano.newContext(this.getClass()), 11111111, eventResults::add, false);
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 11111111)).isNotNull();
        waitForCondition(() -> eventResults.size() == 1, TEST_TIMEOUT);

        // send to first listener (listeners have priority)
        eventResults.clear();
        service.resetEvents();
        nano.addEventListener(TEST_EVENT, Event::acknowledge);
        nano.sendEvent(TEST_EVENT, nano.newContext(this.getClass()), 22222222, eventResults::add, false);
        assertThat(service.getEvent(TEST_EVENT, 256)).isNull();
        assertThat(eventResults).hasSize(1);

        // send to all (listener and services)
        eventResults.clear();
        service.resetEvents();
        nano.sendEvent(TEST_EVENT, nano.newContext(this.getClass()), 33333333, eventResults::add, true);
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 33333333)).isNotNull();
        assertThat(eventResults).hasSize(2);

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

        final Context context = nano.newEmptyContext(this.getClass());
        assertThat(context).hasSize(2).containsKey(CONTEXT_TRACE_ID_KEY).containsKey(CONTEXT_LOGGER_KEY);

        nano.sendEvent(TEST_EVENT, context, 44444444, result -> {
        }, false);
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 44444444)).isNotNull();
        assertThat(service.getEvent(EVENT_APP_UNHANDLED, event -> event.payload(Unhandled.class) != null)).isNotNull();
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
        nano.addEventListener(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).hasSize(1);
        nano.removeEventListener(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).isEmpty();

        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void runSchedulers() throws InterruptedException {
        final long timer = 64;
        final CountDownLatch scheduler1Triggered = new CountDownLatch(1);
        final CountDownLatch scheduler2Triggered = new CountDownLatch(1);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));

        nano.schedule(scheduler1Triggered::countDown, timer, MILLISECONDS);
        nano.schedule(scheduler2Triggered::countDown, timer, timer * 2, MILLISECONDS, () -> false);

        assertThat(scheduler1Triggered.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(scheduler2Triggered.await(TEST_TIMEOUT, MILLISECONDS)).isTrue();
        assertThat(nano.stop(this.getClass()).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void throwExceptionInsideScheduler() {
        final long timer = 64;
        final TestService service = new TestService();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);
        waitForStartUp(nano);

        nano.schedule(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, MILLISECONDS);

        nano.schedule(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, timer * 2, MILLISECONDS, () -> false);

        assertThat(service.getEvent(EVENT_APP_UNHANDLED, event -> event.payload() != null)).isNotNull();
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
        nano.shutdown(nano.newContext(NanoTest.class));
        assertThat(nano.isReady()).isFalse();
        assertThat(nano.services()).isEmpty();
        assertThat(nano.listeners()).isEmpty();
        assertThat(nano.threadPool.isTerminated()).isTrue();
        // assertThat(activeCarrierThreads()).isZero(); Not possible due parallel tests
        assertThat(nano.schedulers()).isEmpty();
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.failures()).isEmpty();
        assertThat(service.stopCount()).isEqualTo(1);
//        assertThat(service.events()).hasSizeBetween(1, 3);
    }
}
