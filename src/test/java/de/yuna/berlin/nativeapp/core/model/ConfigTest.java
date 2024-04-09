package de.yuna.berlin.nativeapp.core.model;

import org.junit.jupiter.api.RepeatedTest;

import static de.yuna.berlin.nativeapp.core.config.TestConfig.TEST_REPEAT;
import static de.yuna.berlin.nativeapp.core.model.Config.APP_HELP;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

    @RepeatedTest(TEST_REPEAT)
    void testNewConfig() {
        assertThat(APP_HELP).isNotNull();
        assertThat(APP_HELP).hasToString("help");
        assertThat(APP_HELP.id()).isEqualTo("help");
        assertThat(APP_HELP.description()).isEqualTo("Lists available config keys (see " + Config.class.getSimpleName() + ")");
    }
}
