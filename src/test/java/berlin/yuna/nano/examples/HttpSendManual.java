package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.logic.HttpClient;
import berlin.yuna.nano.services.http.model.HttpObject;

import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_HTTP_REQUEST;
import static berlin.yuna.nano.services.http.model.HttpMethod.GET;

public class HttpSendManual {

    public static void main(final String[] args) {
        final Context context = new Nano(args, new HttpService()).context(HttpSendManual.class);

        // With context
        new HttpObject()
            .methodType(GET)
            .path("http://localhost:8080/hello")
            .body("Hello World")
            .send(context);

        // Without context
        new HttpClient().send(new HttpObject()
            .methodType(GET)
            .path("http://localhost:8080/hello")
            .body("Hello World")
        );
    }
}
