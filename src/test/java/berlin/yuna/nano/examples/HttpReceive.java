package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.model.HttpObject;

import java.util.Map;

import static berlin.yuna.nano.core.model.Context.EVENT_APP_UNHANDLED;
import static berlin.yuna.nano.services.http.HttpService.EVENT_HTTP_REQUEST;

public class HttpReceive {

    public static void main(final String[] args) {
        final Nano app = new Nano(args, new HttpService());

        // Authorization
        app.subscribeEvent(EVENT_HTTP_REQUEST, HttpReceive::authorize);

        // Response
        app.subscribeEvent(EVENT_HTTP_REQUEST, HttpReceive::helloWorldController);

        // Error handling
        app.subscribeEvent(EVENT_APP_UNHANDLED, HttpReceive::controllerAdvice);
    }

    private static void authorize(final Event event) {
        event.payloadOpt(HttpObject.class)
            .filter(request -> request.pathMatch("/hello/**"))
            .filter(request -> !"mySecretToken".equals(request.authToken()))
            .ifPresent(request -> request.response().body(Map.of("message", "You are unauthorized")).statusCode(401).respond(event));
    }

    private static void helloWorldController(final Event event) {
        event.payloadOpt(HttpObject.class)
            .filter(HttpObject::isMethodGet)
            .filter(request -> request.pathMatch("/hello"))
            .ifPresent(request -> request.response().body(Map.of("Hello", System.getProperty("user.name"))).respond(event));
    }

    private static void controllerAdvice(final Event event) {
        event.payloadOpt(HttpObject.class).ifPresent(request ->
            request.response().body("Internal Server Error [" + event.error().getMessage() + "]").statusCode(500).respond(event));
    }
}
