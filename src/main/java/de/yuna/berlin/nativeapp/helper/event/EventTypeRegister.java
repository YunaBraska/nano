package de.yuna.berlin.nativeapp.helper.event;

import de.yuna.berlin.nativeapp.helper.NanoUtils;

import java.util.Map;
import java.util.Optional;

import static de.yuna.berlin.nativeapp.core.NanoBase.EVENT_ID_COUNTER;
import static de.yuna.berlin.nativeapp.core.NanoBase.EVENT_TYPES;
import static de.yuna.berlin.nativeapp.helper.NanoUtils.hasText;
import static java.util.Optional.ofNullable;

public class EventTypeRegister {

    public static int registerEventType(final String typeName) {
        return ofNullable(typeName).filter(NanoUtils::hasText).map(name -> evenIdOf(typeName).orElseGet(() -> {
            final int typeId = EVENT_ID_COUNTER.incrementAndGet();
            EVENT_TYPES.put(typeId, typeName);
            return typeId;
        })).orElse(-1);
    }

    public static String eventNameOf(final int typeId) {
        return EVENT_TYPES.get(typeId);
    }

    // more used for debugging - not performant
    public static Optional<Integer> evenIdOf(final String typeName) {
        return hasText(typeName) ? EVENT_TYPES.entrySet().stream().filter(type -> type.getValue().equals(typeName)).map(Map.Entry::getKey).findFirst() : Optional.empty();
    }

    public static boolean isEventTypeAvailable(final int typeId) {
        return EVENT_TYPES.containsKey(typeId);
    }

    private EventTypeRegister() {
        // static util class
    }
}
