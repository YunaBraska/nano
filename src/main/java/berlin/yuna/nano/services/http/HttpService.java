package berlin.yuna.nano.services.http;

import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.core.model.Unhandled;
import berlin.yuna.nano.services.http.model.ContentType;
import berlin.yuna.nano.services.http.model.HttpHeaders;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Supplier;

import static berlin.yuna.nano.helper.event.model.EventType.*;

public class HttpService extends Service {
    private HttpServer server;

    public HttpService() {
        super(null, false);
    }

    @Override
    public void stop(final Supplier<Context> contextSub) {
        isReady.set(true, false, state -> {
            server.stop(0);
            logger.info(() -> "[{}] port [{}] stopped", name(), (server == null ? null : server.getAddress().getPort()));
            server = null;
        });
    }

    @Override
    public synchronized void start(final Supplier<Context> contextSub) {
        isReady.set(false, true, state -> {
            final Context context = contextSub.get().newContext(HttpService.class, null);
            //TODO: use next free port instead of hardcoded 8080
            final int port = context.getOpt(Integer.class, "app_service_http_port").filter(p -> p > 0).orElse(8080);
            handleHttps(context);
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.setExecutor(context.nano().threadPool());
                server.createContext("/", exchange -> {
                    try {
                        //TODO: #1 Create own request object instead of the exchange as there is no control tp prevent the user to use `exchange.sendResponseHeaders` which breaks the following logic
                        context.sendEventReturn(EVENT_HTTP_REQUEST, exchange).responseOpt(HttpResponse.class).ifPresentOrElse(
                            response -> sendResponse(exchange, response),
                            () -> context.sendEventReturn(EVENT_HTTP_REQUEST_UNHANDLED, exchange).responseOpt(HttpResponse.class).ifPresentOrElse(
                                response -> sendResponse(exchange, response),
                                () -> sendResponse(exchange, new HttpResponse(404, "Page not found".getBytes(), new HashMap<>()))
                            )
                        );
                    } catch (final Exception e) {
                        context.sendEventReturn(EVENT_APP_UNHANDLED, new Unhandled(context, exchange, e)).responseOpt(HttpResponse.class).ifPresentOrElse(
                            response -> sendResponse(exchange, response),
                            () -> new HttpResponse(500, ("Internal Server Error " + e.getMessage()).getBytes(), new HashMap<>())
                        );
                    }
                });
                server.start();
                logger.info(() -> "[{}] starting on port [{}]", name(), port);
            } catch (final IOException e) {
                logger.error(e, () -> "[{}] failed to start with port [{}]", name(), port);
            }
        });
    }

    private static void handleHttps(final Context context) {
        //TODO: add option for HTTPS
        //TODO: handle certificates
        final Optional<String> crt = context.getOpt(String.class, "app.https.crt.path");
        final Optional<String> key = context.getOpt(String.class, "app.https.key.path");
        if (crt.isPresent() && key.isPresent()) {
//            // Load the certificate
//            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//            X509Certificate cert = (X509Certificate) cf.generateCertificate(new FileInputStream(crtFilePath));
//
//            // Load the private key
//            final byte[] keyBytes = Files.readAllBytes(Paths.get(keyFilePath));
//            final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//            KeyFactory kf = KeyFactory.getInstance("RSA"); // TODO: TRY & ERROR loop for all Algorithmn
//            final PrivateKey privateKey = kf.generatePrivate(spec);
//
//            // Create a keystore
//            final KeyStore keyStore = KeyStore.getInstance("JKS");
//            keyStore.load(null);
//            keyStore.setCertificateEntry("cert", cert);
//            keyStore.setKeyEntry("key", privateKey, "password".toCharArray(), new Certificate[]{cert});
//
//            // Initialize the SSL context
//            final KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
//            kmf.init(keyStore, "password".toCharArray());
//            final SSLContext sslContext = SSLContext.getInstance("TLS");
//            sslContext.init(kmf.getKeyManagers(), null, null);
//
//            // Set up the HTTPS server
//            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(port), 0);
//            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext));
//            this.server = httpsServer;
        }
    }

    @Override
    public Object onFailure(final Unhandled error) {
        return null;
    }

    private void sendResponse(final HttpExchange exchange, final HttpResponse response) {
        try {
            final byte[] body = response.body() != null ? response.body() : new byte[0];
            final int statusCode = response.statusCode > -1 && response.statusCode < 600 ? response.statusCode : 200;
            final Map<String, String> headers = response.headers == null ? new HashMap<>() : new HashMap<>(response.headers);
            headers.computeIfAbsent(HttpHeaders.CONTENT_TYPE, value -> {
                final String str = new String(body, Charset.defaultCharset());
                return (str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]")) ? ContentType.APPLICATION_JSON.value() : ContentType.TEXT_PLAIN.value();
            });
            headers.forEach((key, value) -> exchange.getResponseHeaders().put(key, List.of(value)));
            exchange.sendResponseHeaders(statusCode, body.length);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (final IOException ignored) {
            // Response was already sent
        }
    }

    public record HttpResponse(int statusCode, byte[] body, Map<String, String> headers) {
        @Override
        public boolean equals(final Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            final HttpResponse that = (HttpResponse) object;
            return statusCode == that.statusCode && Arrays.equals(body, that.body) && Objects.equals(headers, that.headers);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(statusCode, headers);
            result = 31 * result + Arrays.hashCode(body);
            return result;
        }

        @Override
        public String toString() {
            return new StringJoiner(", ", HttpResponse.class.getSimpleName() + "[", "]")
                .add("statusCode=" + statusCode)
                .add("body=" + Arrays.toString(body))
                .add("headers=" + headers)
                .toString();
        }
    }
}

