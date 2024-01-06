package de.yuna.berlin.nativeapp.core;

import de.yuna.berlin.nativeapp.helper.event.model.Event;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;
import de.yuna.berlin.nativeapp.helper.threads.Executor;
import de.yuna.berlin.nativeapp.model.TestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import static de.yuna.berlin.nativeapp.core.model.Config.APP_HELP;
import static de.yuna.berlin.nativeapp.core.model.Config.APP_PARAMS;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_PARALLEL_SHUTDOWN;
import static de.yuna.berlin.nativeapp.helper.NoExitSecurityManager.catchSystemExit;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SCHEDULER_REGISTER;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SERVICE_REGISTER;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_SHUTDOWN;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_START;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_UNHANDLED;
import static de.yuna.berlin.nativeapp.helper.logger.model.LogLevel.INFO;
import static de.yuna.berlin.nativeapp.helper.threads.Executor.tryExecute;
import static de.yuna.berlin.nativeapp.model.TestService.TEST_EVENT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class NanoTest {

    //TODO: Move Service getters to Context
    //TODO: Move Schedulers getters to Context
    //TODO: Take care of long stack traces
    //TODO: Logger exclude package pattern config
    //TODO: extract logger as a service

    //Nano is fast but the assertions and setup of, IDE, JVM, Debugger, Profiler, etc. slows down the tests
    public static final int APPLICATION_RUN_TIMEOUT = 500;
    public static final LogLevel TEST_LOG_LEVEL = LogLevel.WARN;

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT, unit = MILLISECONDS)
    void stopViaMethod() {
        assertThat(new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL))
                .stop(this.getClass())
        ).isNotNull();
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT, unit = MILLISECONDS)
    void stopViaEvent() {
        assertThat(new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL))
                .context(this.getClass())
                .sendEvent(EVENT_APP_SHUTDOWN.id(), this)
        ).isNotNull();
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 2, unit = MILLISECONDS)
    void startMultipleTimes_shouldHaveNoIssues() {
        final TestService service1 = new TestService(true);
        final TestService service2 = new TestService(true);
        final Nano nano1 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service1);
        final Nano nano2 = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service2);
        assertThat(nano1).isNotEqualTo(nano2);
        stopAndTestNano(nano1, service1);
        stopAndTestNano(nano2, service2);
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 8, unit = MILLISECONDS)
    void parallelShutdownServiceTest() {
        final TestService testService = new TestService(true);
        testService.stopConsumer.set(event -> tryExecute(() -> Thread.sleep(100)));

        final long startTime1 = System.currentTimeMillis();
        new Nano(
                Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL),
                testService,
                testService,
                testService,
                testService
        ).shutdown(this.getClass());
        final long uptime1 = System.currentTimeMillis() - startTime1;

        final long startTime2 = System.currentTimeMillis();
        new Nano(
                Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, CONFIG_PARALLEL_SHUTDOWN, true),
                testService,
                testService,
                testService,
                testService
        ).shutdown(this.getClass());
        final long uptime2 = System.currentTimeMillis() - startTime2;
        assertThat(uptime1).isGreaterThan(uptime2);
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 8, unit = MILLISECONDS)
    void parallelShutdownServiceWithExceptionTest() {
        final TestService testService = new TestService(true);
        testService.stopConsumer.set(event -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, CONFIG_PARALLEL_SHUTDOWN, true), testService).shutdown(this.getClass());
        assertThat(nano).isNotNull();
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 8, unit = MILLISECONDS)
    void serviceTimeoutTest() {
        final TestService testService = new TestService(true);
        testService.stopConsumer.set(event -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), testService);
        nano.context(this.getClass()).async(APPLICATION_RUN_TIMEOUT, context -> {
            try {
                context.logger().info(() -> "START HELLO");
                Thread.sleep(APPLICATION_RUN_TIMEOUT * 2);
                context.logger().info(() -> "END WORLD");
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
        nano.shutdown(this.getClass());
        assertThat(nano).isNotNull();
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 4, unit = MILLISECONDS)
    void constructorTest() {
        final Nano noArgs = new Nano();
        assertThat(noArgs).isNotNull();
        assertThat(noArgs.logger().level()).isEqualTo(LogLevel.DEBUG);
        noArgs.setLogLevel(TEST_LOG_LEVEL);
        assertThat(noArgs.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        noArgs.shutdown(this.getClass());

        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        config.shutdown(this.getClass());

        final Nano configAndService = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), new TestService(true));
        assertThat(configAndService).isNotNull();
        assertThat(configAndService.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(configAndService.services()).hasSize(1);
        configAndService.shutdown(this.getClass());

        final Nano lazyServices = new Nano(context -> List.of(new TestService(true)), "-" + CONFIG_LOG_LEVEL.id() + "=" + TEST_LOG_LEVEL);
        assertThat(lazyServices).isNotNull();
        assertThat(lazyServices.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        assertThat(lazyServices.services()).hasSize(1);
        lazyServices.shutdown(this.getClass());
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 4, unit = MILLISECONDS)
    void printParameterTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, APP_PARAMS, true));
        assertThat(config).isNotNull();
        assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
        config.shutdown(this.getClass());
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT, unit = MILLISECONDS)
    void printHelpMenu() throws Exception {
        final int statusCode = catchSystemExit(() -> {
            final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, INFO, APP_HELP, true));
            assertThat(config).isNotNull();
            assertThat(config.logger().level()).isEqualTo(TEST_LOG_LEVEL);
            config.shutdown(this.getClass());
        });
        assertThat(statusCode).isEqualTo(0);
    }


    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 4, unit = MILLISECONDS)
    void toStringTest() {
        final Nano config = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL, APP_PARAMS, true));
        assertThat(config).isNotNull();
        assertThat(config.toString()).contains(
                "pid=",
                "schedulers=", "services=", "listeners=",
                "cores=", "usedMemory=",
                "threadsMin=", "threadsMax=", "threadsActive=", "threadsOther=",
                "java=", "arch=", "os="
        );
        config.shutdown(this.getClass());
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT, unit = MILLISECONDS)
    void sendEvent() {
        final List<Object> eventResults = new ArrayList<>();
        final TestService service = new TestService(true);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), service);
        service.eventConsumer.set(Event::acknowledge);

        // send to first service
        service.wait(EVENT_APP_START.id()).events.clear();
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 11111111, eventResults::add, true, true, true);
        assertThat(service.eventNames()).hasSize(1).contains("TEST_EVENT");
        assertThat(eventResults).hasSize(1);

        // send to first listener (listeners have priority)
        eventResults.clear();
        service.events.clear();
        nano.addEventListener(TEST_EVENT, Event::acknowledge);
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 22222222, eventResults::add, true, true, true);
        assertThat(service.eventNames()).isEmpty();
        assertThat(eventResults).hasSize(1);

        // send to all (listener and services)
        eventResults.clear();
        service.events.clear();
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 33333333, eventResults::add, false, true, true);
        assertThat(service.eventNames()).hasSize(1).contains("TEST_EVENT");
        assertThat(eventResults).hasSize(2);

        nano.shutdown(nano.context(this.getClass()));
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT, unit = MILLISECONDS)
    void sendEventWithEventExecutionException_shouldNotInterrupt() {
        final TestService service = new TestService(true);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, LogLevel.OFF), service);
        service.eventConsumer.set(event -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        });

        service.wait(EVENT_APP_START.id()).events.clear();
        nano.sendEvent(TEST_EVENT, nano.context(this.getClass()), 44444444, null, false, true, false);
        service.wait(EVENT_APP_UNHANDLED.id());
        assertThat(service.startCount.get()).isEqualTo(1);
        assertThat(service.stopCount.get()).isZero();
        assertThat(service.failures).hasSize(1);
        assertThat(service.eventNames()).containsExactly("TEST_EVENT", EVENT_APP_UNHANDLED.name());
        assertThat(nano.isReady()).isTrue();

        nano.shutdown(nano.context(this.getClass()));
        assertThat(service.stopCount.get()).isEqualTo(1);
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT, unit = MILLISECONDS)
    void addAndRemoveEventListener() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL));
        final Consumer<Event> listener = event -> {
        };

        assertThat(nano.listeners().get(TEST_EVENT)).isNull();
        nano.addEventListener(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).hasSize(1);
        nano.removeEventListener(TEST_EVENT, listener);
        assertThat(nano.listeners().get(TEST_EVENT)).isEmpty();

        nano.shutdown(nano.context(this.getClass()));
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 3, unit = MILLISECONDS)
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

        nano.shutdown(nano.context(this.getClass()));
    }

    @Test
    @Timeout(value = APPLICATION_RUN_TIMEOUT * 2, unit = MILLISECONDS)
    void throwExceptionInsideScheduler() {
        final long timer = 64;
        final TestService testService = new TestService(true);
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), testService);
        testService.wait(EVENT_APP_START.id()).events.clear();

        nano.schedule(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, MILLISECONDS);

        nano.schedule(() -> {
            throw new RuntimeException("Nothing to see here, just a test exception");
        }, timer, timer * 2, MILLISECONDS, () -> false);

        testService.wait(EVENT_APP_UNHANDLED.id());
        assertThat(testService.eventNames()).contains(EVENT_APP_UNHANDLED.name());
        assertThat(nano.schedulers()).hasSize(2);
        nano.shutdown(nano.context(this.getClass()));
    }

    //TODO: test configurations

    private static void stopAndTestNano(final Nano nano, final TestService testService) {
        assertThat(nano.isReady()).isTrue();
        assertThat(nano.createdAtMs()).isGreaterThan(System.currentTimeMillis() - 1000);
        assertThat(nano.pid()).isPositive();
        assertThat(nano.usedMemoryMB()).isPositive();
        assertThat(nano.usedMemoryMB()).isLessThan(100);
        assertThat(nano.services()).hasSize(1).contains(testService);
        assertThat(nano.service(TestService.class)).isEqualTo(testService);
        assertThat(nano.services(TestService.class)).hasSize(1).contains(testService);
        assertThat(testService.startCount.get()).isEqualTo(1);
        assertThat(testService.failures).isEmpty();
        assertThat(testService.stopCount.get()).isZero();

        // Stop
        nano.shutdown(nano.context(NanoTest.class));
        assertThat(nano.isReady()).isFalse();
        assertThat(nano.services()).isEmpty();
        assertThat(nano.listeners()).isEmpty();
        assertThat(nano.threadPool().getActiveCount()).isZero();
        assertThat(nano.schedulers()).isEmpty();
        assertThat(testService.startCount.get()).isEqualTo(1);
        assertThat(testService.failures).isEmpty();
        assertThat(testService.stopCount.get()).isEqualTo(1);
        assertThat(testService.eventIds()).hasSize(3).contains(EVENT_APP_SERVICE_REGISTER.id(), EVENT_APP_SCHEDULER_REGISTER.id(), EVENT_APP_START.id());
    }
}
