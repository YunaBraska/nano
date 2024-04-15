package berlin.yuna.nano.core.model;

import org.junit.jupiter.api.RepeatedTest;

import static berlin.yuna.nano.core.config.TestConfig.TEST_REPEAT;
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
