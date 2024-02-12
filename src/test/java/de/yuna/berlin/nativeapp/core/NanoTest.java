package de.yuna.berlin.nativeapp.core;

import de.yuna.berlin.nativeapp.core.model.Unhandled;
import de.yuna.berlin.nativeapp.helper.PrintTestNamesExtension;
import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;
import de.yuna.berlin.nativeapp.helper.threads.Executor;
import de.yuna.berlin.nativeapp.model.TestService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static de.yuna.berlin.nativeapp.core.model.Config.*;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SHUTDOWN;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static de.yuna.berlin.nativeapp.helper.threads.Executor.tryExecute;
import static de.yuna.berlin.nativeapp.model.TestService.TEST_EVENT;
import static de.yuna.berlin.nativeapp.model.TestService.waitFor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(PrintTestNamesExtension.class)
public class NanoTest {

    //TODO: Move Service getters to Context
    //TODO: Move Schedulers getters to Context
    //TODO: Take care of long stack traces
    //TODO: Logger: change format on runtime
    //TODO: Logger exclude package pattern config
    //TODO: extract logger as a service

    //Nano is fast but the assertions and setup of, IDE, JVM, Debugger, Profiler, etc. slows down the tests
    public static final LogLevel TEST_LOG_LEVEL = LogLevel.ALL;
    // Testing consistency
    public static final int TEST_REPEAT = 10;

    @RepeatedTest(TEST_REPEAT)
    void stopViaMethod() {
        assertThat(new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL))
            .stop(this.getClass())
        ).isNotNull();
    }

    @RepeatedTest(TEST_REPEAT)
    void stopViaEvent() {
        assertThat(new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL))
            .context(this.getClass())
            .sendEvent(EVENT_APP_SHUTDOWN.id(), this)
        ).isNotNull();
    }

    @RepeatedTest(TEST_REPEAT)
    void startMultipleTimes_shouldHaveNoIssues() {
        final TestService service1 = new TestService();
        final TestService service2 = new TestService();
        final Nano nano1 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service1);
        final Nano nano2 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service2);
        assertThat(nano1).isNotEqualTo(nano2);
        stopAndTestNano(nano1, service1);
        stopAndTestNano(nano2, service2);
    }

    @RepeatedTest(TEST_REPEAT)
    void shutdownServicesInParallelTest_Sync() {
        final AtomicInteger stopCount = new AtomicInteger(0);
        final TestService testService = new TestService();
        testService.doOnStop(context -> tryExecute(stopCount::incrementAndGet));

        final Nano nano1 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), testService, testService, testService, testService);
        final Nano nano2 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, CONFIG_PARALLEL_SHUTDOWN, true), testService, testService, testService, testService);
        nano1.shutdown(this.getClass());
        nano2.shutdown(this.getClass());
        assertThat(stopCount).hasValue(8);
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
        noArgs.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withConfigTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        config.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withConfigAndServiceTest() {
        final Nano configAndService = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), new TestService());
        assertThat(configAndService).isNotNull();
        assertThat(configAndService.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(configAndService.services()).hasSize(1);
        configAndService.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withLazyServices_Test() {
        final Nano lazyServices = new Nano(context -> List.of(new TestService()), "-" + CONFIG_LOG_LEVEL.id() + "=" + TEST_LOG_LEVEL);
        assertThat(lazyServices).isNotNull();
        assertThat(lazyServices.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(lazyServices.services()).hasSize(1);
        lazyServices.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void printParameterTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, APP_PARAMS, true));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        config.stop(this.getClass());
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
        config.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void sendEvent_Sync() {
        final List<Object> eventResults = new ArrayList<>();
        final TestService service = new TestService();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);
        service.doOnEvent(Event::acknowledge);

        // send to first service
        service.resetEvents();
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 11111111, eventResults::add, true, true, false);
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 11111111)).isNotNull();
        assertThat(eventResults).hasSize(1);

        // send to first listener (listeners have priority)
        eventResults.clear();
        service.resetEvents();
        nano.addEventListener(TEST_EVENT, Event::acknowledge);
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 22222222, eventResults::add, true, true, true);
        assertThat(service.getEvent(TEST_EVENT, 256)).isNull();
        assertThat(eventResults).hasSize(1);

        // send to all (listener and services)
        eventResults.clear();
        service.resetEvents();
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 33333333, eventResults::add, false, true, true);
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 33333333)).isNotNull();
        assertThat(eventResults).hasSize(2);

        nano.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void sendEventWithEventExecutionException_shouldNotInterrupt() {
        final TestService service = new TestService();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.OFF), service);

        service.doOnEvent(event -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 44444444, null, false, true, false);
        assertThat(service.getEvent(TEST_EVENT, event -> event.payload(Integer.class) == 44444444)).isNotNull();
        assertThat(service.getEvent(EVENT_APP_UNHANDLED.id(), event -> event.payload(Unhandled.class) != null)).isNotNull();
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.stopCount()).isZero();
        assertThat(service.failures()).isNotEmpty();
        assertThat(nano.isReady()).isTrue();

        nano.shutdown(nano.context(this.getClass()));
        assertThat(service.stopCount()).isEqualTo(1);
    }

    @RepeatedTest(TEST_REPEAT)
    void addAndRemoveEventListener() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final Consumer<Event> listener = event -> {
        };

        assertThat(nano.listeners().get(TEST_EVENT)).isNull();
        nano.addEventListener(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).hasSize(1);
        nano.removeEventListener(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).isEmpty();

        nano.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void runSchedulers() {
        final long timer = 64;
        final AtomicLong scheduler1Triggered = new AtomicLong(-1);
        final AtomicLong scheduler2Triggered = new AtomicLong(-1);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));

        final long scheduler1Start = System.currentTimeMillis();
        nano.schedule(() -> scheduler1Triggered.compareAndSet(-1, System.currentTimeMillis()), timer, MILLISECONDS);

        final long scheduler2Start = System.currentTimeMillis();
        nano.schedule(() -> scheduler2Triggered.compareAndSet(-1, System.currentTimeMillis()), timer, timer * 2, MILLISECONDS, () -> false);
        Executor.tryExecute(() -> Thread.sleep(timer * 2));

        assertThat(nano.schedulers()).hasSize(2);
        assertThat(scheduler1Triggered.get())
            .isNotNegative()
            .isGreaterThan(scheduler1Start)
            .isLessThan(System.currentTimeMillis());

        assertThat(scheduler2Triggered.get())
            .isNotNegative()
            .isGreaterThan(scheduler2Start)
            .isLessThan(System.currentTimeMillis());

        nano.stop(this.getClass());
    }

    @RepeatedTest(TEST_REPEAT)
    void throwExceptionInsideScheduler() {
        final long timer = 64;
        final TestService service = new TestService();
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);

        nano.schedule(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, MILLISECONDS);

        nano.schedule(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, timer * 2, MILLISECONDS, () -> false);

        assertThat(nano.schedulers()).hasSizeBetween(2, 3);
        assertThat(service.getEvent(EVENT_APP_UNHANDLED.id(), event -> event.payload() != null)).isNotNull();
        nano.stop(this.getClass());
    }

    //TODO: test configurations

    private static void stopAndTestNano(final Nano nano, final TestService service) {
        assertThat(nano.isReady()).isTrue();
        assertThat(nano.createdAtMs()).isPositive();
        assertThat(nano.pid()).isPositive();
        assertThat(nano.usedMemoryMB()).isPositive();
        assertThat(nano.usedMemoryMB()).isLessThan(500);
        assertThat(nano.services()).hasSize(1).contains(service);
        assertThat(nano.service(TestService.class)).isEqualTo(service);
        assertThat(nano.services(TestService.class)).hasSize(1).contains(service);
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.failures()).isEmpty();
        assertThat(service.stopCount()).isZero();

        // Stop
        waitFor(() -> !service.events().isEmpty());
        nano.shutdown(nano.context(NanoTest.class));
        assertThat(nano.isReady()).isFalse();
        assertThat(nano.services()).isEmpty();
        assertThat(nano.listeners()).isEmpty();
        assertThat(nano.threadPool.isTerminated()).isTrue();
        // assertThat(activeCarrierThreads()).isZero(); FIXME: find a way to count active threads
        assertThat(nano.schedulers()).isEmpty();
        assertThat(service.startCount()).isEqualTo(1);
        assertThat(service.failures()).isEmpty();
        assertThat(service.stopCount()).isEqualTo(1);
        assertThat(service.events()).hasSizeBetween(1, 3);
    }
}
