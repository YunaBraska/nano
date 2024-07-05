package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import org.junit.jupiter.api.Disabled;

import static berlin.yuna.nano.core.model.Context.EVENT_APP_UNHANDLED;

@Disabled
public class ErrorHandling {

    public static void main(final String[] args) {
        final Context context = new Nano(args).context(ErrorHandling.class);

        // Listen to exceptions
        context.subscribeEvent(EVENT_APP_UNHANDLED, event -> {
            // Print error message
            event.context().logger().warn(() -> "Caught event [{}] with error [{}] ", event.nameOrg(), event.error().getMessage());
            event.acknowledge(); // Set exception as handled (prevent further processing)
        });

        // Throw an exception
        context.run(() -> {
            throw new RuntimeException("Test Exception");
        });
    }
}
