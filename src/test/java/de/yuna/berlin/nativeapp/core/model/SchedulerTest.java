package de.yuna.berlin.nativeapp.core.model;

import org.junit.jupiter.api.RepeatedTest;

import static de.yuna.berlin.nativeapp.core.config.TestConfig.TEST_REPEAT;
import static org.assertj.core.api.Assertions.assertThat;

class SchedulerTest {

    @RepeatedTest(TEST_REPEAT)
    void testNewScheduler() {
        final Scheduler scheduler = new Scheduler("test");
        assertThat(scheduler).isNotNull();
        assertThat(scheduler.id()).isEqualTo("test");
        assertThat(scheduler).hasToString("Scheduler{id='test'}");
    }

}
