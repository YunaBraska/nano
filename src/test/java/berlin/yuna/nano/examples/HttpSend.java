package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.model.HttpObject;

import java.util.Map;

import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_HTTP_REQUEST;
import static berlin.yuna.nano.services.http.model.HttpMethod.GET;

public class HttpSend {

    public static void main(final String[] args) {
        final Context context = new Nano(args, new HttpService()).context(HttpSend.class);
        final HttpObject response = context.sendEventReturn(EVENT_HTTP_REQUEST, new HttpObject()
            .methodType(GET)
            .path("http://localhost:8080/hello")
            .body("Hello World")
        ).response(HttpObject.class);
    }
}
