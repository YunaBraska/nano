package de.yuna.berlin.nativeapp.helper.event.model;

import org.junit.jupiter.api.Test;

import static de.yuna.berlin.nativeapp.helper.event.EventTypeRegister.*;
import static org.assertj.core.api.Assertions.assertThat;

class EventTypeRegisterTest {

    @Test
    void testEventTypeRegistration() {
        final int eventId = registerEventType(this.getClass().getSimpleName().toUpperCase());
        assertThat(isEventTypeAvailable(eventId)).isTrue();
        assertThat(eventNameOf(eventId)).isEqualTo(this.getClass().getSimpleName().toUpperCase());

        // duplicated registration should not be possible
        final int eventId2 = registerEventType(this.getClass().getSimpleName().toUpperCase());
        assertThat(isEventTypeAvailable(eventId2)).isTrue();
        assertThat(eventId).isEqualTo(eventId2);
        assertThat(eventNameOf(eventId)).isEqualTo(this.getClass().getSimpleName().toUpperCase());

        // should not find non registered eventIds
        assertThat(isEventTypeAvailable(-99)).isFalse();
    }
}
