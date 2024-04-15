package berlin.yuna.nano.core.model;

import berlin.yuna.nano.core.config.TestConfig;
import org.junit.jupiter.api.RepeatedTest;

import static berlin.yuna.nano.core.model.Config.APP_HELP;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigTest {

    @RepeatedTest(TestConfig.TEST_REPEAT)
    void testNewConfig() {
        assertThat(APP_HELP).isNotNull();
        assertThat(APP_HELP).hasToString("help");
        assertThat(APP_HELP.id()).isEqualTo("help");
        assertThat(APP_HELP.description()).isEqualTo("Lists available config keys (see " + Config.class.getSimpleName() + ")");
    }
}
