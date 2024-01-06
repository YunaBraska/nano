package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.core.Nano;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static de.yuna.berlin.nativeapp.core.NanoTest.TEST_LOG_LEVEL;
import static de.yuna.berlin.nativeapp.core.model.Config.CONFIG_LOG_LEVEL;
import static org.assertj.core.api.Assertions.assertThat;

class UnhandledTest {

    @Test
    void testConstructor() {
        final Nano nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL)).stop(this.getClass());
        final Context context = nano.context(this.getClass());
        final Unhandled error = new Unhandled(context, 111, null);
        assertThat(error).isNotNull();
        assertThat(error.nano()).isEqualTo(nano);
        assertThat(error.context()).isEqualTo(context);
        assertThat(error.payload()).isEqualTo(111);
        assertThat(error.payload(String.class)).isEqualTo("111");
        assertThat(error.payload(Integer.class)).isEqualTo(111);
        assertThat(error.exception()).isNull();
    }

    @Test
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
