package berlin.yuna.nano.helper.event;

import berlin.yuna.nano.helper.NanoUtils;

import java.util.Map;
import java.util.Optional;

import static berlin.yuna.nano.core.NanoBase.EVENT_ID_COUNTER;
import static berlin.yuna.nano.core.NanoBase.EVENT_TYPES;
import static java.util.Optional.ofNullable;

public class EventTypeRegister {

    /**
     * Registers a new event type with a given name if it does not already exist.
     * If the event type already exists, it returns the existing event type's ID.
     *
     * @param typeName The name of the event type to register.
     * @return The ID of the newly registered event type, or the ID of the existing event type
     *         if it already exists. Returns -1 if the input is null or empty.
     */
    public static int registerEventType(final String typeName) {
        return ofNullable(typeName).filter(NanoUtils::hasText).map(name -> evenIdOf(typeName).orElseGet(() -> {
            final int typeId = EVENT_ID_COUNTER.incrementAndGet();
            EVENT_TYPES.put(typeId, typeName);
            return typeId;
        })).orElse(-1);
    }

    /**
     * Retrieves the name of an event type given its ID.
     *
     * @param typeId The ID of the event type.
     * @return The name of the event type associated with the given ID, or null if not found.
     */
    public static String eventNameOf(final int typeId) {
        return EVENT_TYPES.get(typeId);
    }

    /**
     * Attempts to find the ID of an event type based on its name.
     * This method is primarily used for debugging or startup purposes and is not optimized for performance.
     *
     * @param typeName The name of the event type.
     * @return An {@link Optional} containing the ID of the event type if found, or empty if not found
     *         or if the input is null or empty.
     */
    public static Optional<Integer> evenIdOf(final String typeName) {
        return NanoUtils.hasText(typeName) ? EVENT_TYPES.entrySet().stream().filter(type -> type.getValue().equals(typeName)).map(Map.Entry::getKey).findFirst() : Optional.empty();
    }

    /**
     * Checks if an event type with the given ID exists.
     *
     * @param typeId The ID of the event type to check.
     * @return true if an event type with the given ID exists, false otherwise.
     */
    public static boolean isEventTypeAvailable(final int typeId) {
        return EVENT_TYPES.containsKey(typeId);
    }

    private EventTypeRegister() {
        // static util class
    }
}
