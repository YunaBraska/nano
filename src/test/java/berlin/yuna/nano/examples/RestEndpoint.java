package berlin.yuna.nano.examples;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.NanoThread;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.typemap.model.LinkedTypeMap;
import berlin.yuna.typemap.model.TypeInfo;

import java.util.Date;

import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_HTTP_REQUEST;

public class RestEndpoint {

    public static void main(final String[] args) {
        final Nano app = new Nano(args, new HttpService());

        app.subscribeEvent(EVENT_HTTP_REQUEST, event -> event
            .payloadOpt(HttpObject.class)
            .filter(HttpObject::isMethodGet)
            .filter(request -> request.pathMatch("/hello"))
            .ifPresent(request -> request.response().body(createBody(request)).send(event))
        );
    }

    private static TypeInfo<?> createBody(final HttpObject request) {
        return new LinkedTypeMap()
            .putReturn("hello", System.getProperty("user.name"))
            .putReturn("threads", NanoThread.activeNanoThreads() + NanoThread.activeCarrierThreads())
            .putReturn("Method", request.method())
            .putReturn("Path", request.path())
            .putReturn("Port", request.port())
            .putReturn("Host", request.host())
            .putReturn("Address", request.address())
            .putReturn("FrontendCall", request.isFrontendCall())
            .putReturn("MobileCall", request.isMobileCall())
            .putReturn("Language", request.acceptLanguage().getDisplayName())
            .putReturn("AuthToken", request.authToken())
            .putReturn("ContentType", request.contentType())
            .putReturn("Encoding", request.encoding())
            .putReturn("Accept", request.accept())
            .putReturn("Accept-Encoding", request.acceptEncoding())
            .putReturn("QueryParam_date", request.queryParams().get(Date.class, "date"))
            .putReturn("QueryParam_number", request.queryParams().get(Number.class, "number"))
            .putReturn("Header_date", request.headerMap().get(Date.class, "date"))
            .putReturn("Header_number", request.headerMap().get(Number.class, "number"))
            .putReturn("Body_date", request.bodyAsJson().get(Date.class, "date"))
            .putReturn("Body_number", request.bodyAsJson().get(Number.class, "number"))
            ;
    }
}
