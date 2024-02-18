package de.yuna.berlin.nativeapp.core.config;

import de.yuna.berlin.nativeapp.core.Nano;
import de.yuna.berlin.nativeapp.helper.logger.model.LogLevel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class TestConfig {

    /**
     * Defines the log level for testing purposes, allowing for easy adjustment during debugging.
     * This can be particularly useful when trying to isolate or identify specific issues within tests.
     */
    public static final LogLevel TEST_LOG_LEVEL = LogLevel.WARN;

    /**
     * Specifies the number of times tests should be repeated to ensure concurrency reliability. This setting aims to strike a balance between thorough testing and practical execution times.
     * It's advised to maintain this value around 100 repeats. Higher values might affect the reliability of timing-sensitive assertions due to the varying capabilities of different testing environments.
     * <p>
     * This concurrency configuration supports the following objectives:
     * - Thread Safety: Ensures that components behave correctly when accessed by multiple threads simultaneously.
     * - Validates Performance Under Load: Confirms that the system can handle high levels of concurrency without significant performance degradation.
     * - Guarantees Correct Event Handling: Verifies that events are processed accurately and in order even when handled concurrently.
     * - Ensures Robustness and Stability: Checks for the resilience of the system under concurrent usage, ensuring it remains stable and performs consistently.
     * - Prepares for Real-World Scenarios: Mimics real-world application usage to ensure the system can handle concurrent operations effectively.
     * - Promotes Confidence in Security: Helps identify potential security vulnerabilities that could be exploited through concurrent execution.
     */
    public static final int TEST_REPEAT = 256;
    public static final int TEST_TIMEOUT = 1000 + (int) (Math.sqrt(TEST_REPEAT) * 50);

    public static Nano waitForStartUp(final Nano nano) {
        return waitForStartUp(nano, 1);
    }

    public static Nano waitForStartUp(final Nano nano, final int numberOfServices) {
        assertThat(waitForCondition(() -> nano.services().size() == numberOfServices)).isTrue();
        return nano;
    }

    /**
     * Waits for a condition to become true, with actions on success or timeout.
     *
     * @param condition The condition to wait for, returning true when met.
     * @return true if the condition was met within the timeout, false otherwise.
     */
    public static boolean waitForCondition(final BooleanSupplier condition) {
        return waitForCondition(condition, TEST_TIMEOUT);
    }

    /**
     * Waits for a condition to become true, with actions on success or timeout.
     *
     * @param condition The condition to wait for, returning true when met.
     * @param timeout   stops waiting after period of time to unblock the test.
     * @return true if the condition was met within the timeout, false otherwise.
     */
    public static boolean waitForCondition(final BooleanSupplier condition, final long timeout) {
        final long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeout) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return false;
    }

    public static boolean await(final CountDownLatch latch) throws InterruptedException {
        return latch.await(TEST_TIMEOUT, MILLISECONDS);
    }
}
