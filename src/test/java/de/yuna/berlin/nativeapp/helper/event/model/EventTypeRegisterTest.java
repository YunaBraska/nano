package de.yuna.berlin.nativeapp.helper.event.model;

import de.yuna.berlin.nativeapp.helper.event.EventTypeRegister;
import org.junit.jupiter.api.Test;

import static de.yuna.berlin.nativeapp.helper.event.EventTypeRegister.eventNameOf;
import static de.yuna.berlin.nativeapp.helper.event.EventTypeRegister.isEventTypeAvailable;
import static de.yuna.berlin.nativeapp.helper.event.EventTypeRegister.registerEventType;
import static de.yuna.berlin.nativeapp.helper.event.model.EventType.EVENT_APP_START;
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

    @Test
    void testEventType() {
        final EventType eventType = EVENT_APP_START;
        assertThat(eventType.id()).isPositive();
        assertThat(eventType.description()).isNotNull().isNotBlank().isNotEmpty();
        assertThat(eventType.name()).isEqualTo("EVENT_APP_START");
    }
}
