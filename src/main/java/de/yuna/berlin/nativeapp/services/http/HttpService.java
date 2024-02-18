package de.yuna.berlin.nativeapp.services.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.yuna.berlin.nativeapp.core.model.Context;
import de.yuna.berlin.nativeapp.core.model.Service;
import de.yuna.berlin.nativeapp.core.model.Unhandled;
import de.yuna.berlin.nativeapp.services.http.model.ContentType;
import de.yuna.berlin.nativeapp.services.http.model.HttpHeaders;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

// UNDER CONSTRUCTION
//TODO: add option for HTTPS
//TODO: add option to get next free port automatically
public class HttpService extends Service {
    private HttpServer server;
    private final int port;
    private final Set<HttpRequestListener> registeredHandlers = ConcurrentHashMap.newKeySet();
    private final AtomicReference<HttpRequestListener> errorHandler = new AtomicReference<>(null);

    public HttpService(final int port) {
        super(null, false);
        this.port = port > 0 ? port : 8058;
    }

    @Override
    public void stop(final Supplier<Context> contextSub) {
        isReady.set(true, false, state -> {
            server.stop(0);
            logger.info(() -> "[{}] port [{}] stopped", name(), port);
            server = null;
        });
    }

    @Override
    public synchronized void start(final Supplier<Context> contextSub) {
        isReady.set(false, true, state -> {

            //TODO: Path to string, File to String <>
            //TODO: handle certificates
            final Context context = contextSub.get();
            final Optional<String> crt = context.gett("app.https.crt.path", String.class);
            final Optional<String> key = context.gett("app.https.key.path", String.class);
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


            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.createContext("/", exchange -> {
                    try {
                        for (final HttpRequestListener handler : registeredHandlers) {
                            if (handler.accept(exchange, context)) {
                                sendResponse(exchange, handler.handle(exchange, context));
                                return;
                            }
                        }
                    } catch (final Exception e) {
                        final HttpRequestListener handler = this.errorHandler.get();
                        if (handler != null && handler.accept(exchange, context)) {
                            sendResponse(exchange, handler.handle(exchange, context));
                            return;
                        }
                        sendResponse(exchange, new HttpResponse(500, ("Internal Server Error " + e.getMessage()).getBytes(), new HashMap<>()));
                    }
                    sendResponse(exchange, new HttpResponse(404, "Page not found".getBytes(), new HashMap<>()));
                });
                server.setExecutor(context.nano().threadPool());
                server.start();
                logger.info(() -> "[{}] starting on port [{}]", name(), port);
            } catch (final IOException e) {
                logger.error(e, () -> "[{}] failed to start with port [{}]", name(), port);
            }
        });
    }

    @Override
    public Object onFailure(final Unhandled error) {
        return null;
    }

    public HttpService registerHttpHandler(final HttpRequestListener handler) {
        registeredHandlers.add(handler);
        return this;
    }

    public HttpService unregisterHttpHandler(final HttpRequestListener handler) {
        registeredHandlers.remove(handler);
        return this;
    }

    private void sendResponse(final HttpExchange exchange, final HttpResponse response) {
        try {
            final byte[] body = response.body() != null ? response.body() : new byte[0];
            final int statusCode = response.statusCode > -1 && response.statusCode < 600 ? response.statusCode : 200;
            final Map<String, String> headers = new HashMap<>(response.headers);
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

    public interface HttpRequestListener {
        boolean accept(final HttpExchange exchange, final Context context);

        HttpResponse handle(final HttpExchange exchange, final Context context);
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

