package berlin.yuna.nano.core.model;

import berlin.yuna.nano.core.Nano;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.util.Map;

import static berlin.yuna.nano.core.config.TestConfig.*;
import static berlin.yuna.nano.core.model.Config.CONFIG_LOG_LEVEL;
import static berlin.yuna.nano.helper.NanoUtils.waitForCondition;
import static org.assertj.core.api.Assertions.assertThat;

@Execution(ExecutionMode.CONCURRENT)
class UnhandledTest {

    @RepeatedTest(TEST_REPEAT)
    void testConstructor() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL)).stop(this.getClass());
        final Context context = nano.newContext(this.getClass());
        final Unhandled error = new Unhandled(context, 111, null);
        assertThat(error).isNotNull();
        assertThat(error.nano()).isEqualTo(nano);
        assertThat(error.context()).isEqualTo(context);
        assertThat(error.payload()).isEqualTo(111);
        assertThat(error.payload(String.class)).isEqualTo("111");
        assertThat(error.payload(Integer.class)).isEqualTo(111);
        assertThat(error.exception()).isNull();
        assertThat(error).hasToString("Unhandled{payload=111, exception=null}");
        assertThat(nano.waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void testNullConstructor() {
        final Unhandled error = new Unhandled(null, null, null);
        assertThat(error).isNotNull();
        assertThat(error.nano()).isNull();
        assertThat(error.context()).isNull();
        assertThat(error.payload()).isNull();
        assertThat(error.payload(String.class)).isNull();
        assertThat(error.exception()).isNull();
    }
}
