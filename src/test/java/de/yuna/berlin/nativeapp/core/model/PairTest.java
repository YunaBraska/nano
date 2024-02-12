package de.yuna.berlin.nativeapp.core.model;

import de.yuna.berlin.nativeapp.helper.PrintTestNamesExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PrintTestNamesExtension.class)
class PairTest {

    @Test
    void testConstructor() {
        final Pair<Integer, String> pair = new Pair<>(111, "222");
        assertThat(pair.left()).isEqualTo(111);
        assertThat(pair.right()).isEqualTo("222");
        assertThat(pair.left(String.class)).isEqualTo("111");
        assertThat(pair.right(Integer.class)).isEqualTo(222);
    }
}
