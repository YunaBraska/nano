package berlin.yuna.nano.services.http;

import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.core.model.Unhandled;
import berlin.yuna.nano.services.http.model.ContentType;
import berlin.yuna.nano.services.http.model.HttpHeaders;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.typemap.logic.TypeConverter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static berlin.yuna.nano.core.model.Config.CONFIG_SERVICE_HTTP_PORT;
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
            final int port = context.getOpt(Integer.class, CONFIG_SERVICE_HTTP_PORT.id()).filter(p -> p > 0).orElseGet(() -> nextFreePort(8080));
            context.put(CONFIG_SERVICE_HTTP_PORT, port);
            handleHttps(context);
            try {
                server = HttpServer.create(new InetSocketAddress(port), 0);
                server.setExecutor(context.nano().threadPool());
                server.createContext("/", exchange -> {
                    final HttpObject httpRequest = new HttpObject(exchange);
                    try {
                        context.sendEventReturn(EVENT_HTTP_REQUEST, httpRequest).responseOpt(HttpObject.class).ifPresentOrElse(
                            response -> sendResponse(exchange, response),
                            () -> context.sendEventReturn(EVENT_HTTP_REQUEST_UNHANDLED, httpRequest).responseOpt(HttpObject.class).ifPresentOrElse(
                                response -> sendResponse(exchange, response),
                                () -> sendResponse(exchange, new HttpObject().statusCode(404).body("Page not found".getBytes()).headers(new HashMap<>()))
                            )
                        );
                    } catch (final Exception e) {
                        context.sendEventReturn(EVENT_APP_UNHANDLED, new Unhandled(context, httpRequest, e)).responseOpt(HttpObject.class).ifPresentOrElse(
                            response -> sendResponse(exchange, response),
                            () -> new HttpObject().statusCode(500).body("Internal Server Error".getBytes()).headers(new HashMap<>())
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

    protected void sendResponse(final HttpExchange exchange, final HttpObject response) {
        try {
            final byte[] body = response.body() != null ? response.body() : new byte[0];
            final int statusCode = response.statusCode() > -1 && response.statusCode() < 600 ? response.statusCode() : 200;
            response.headers().computeIfAbsent(HttpHeaders.CONTENT_TYPE, value -> {
                final String str = new String(body, Charset.defaultCharset());
                return (str.startsWith("{") && str.endsWith("}")) || (str.startsWith("[") && str.endsWith("]")) ? ContentType.APPLICATION_JSON.value() : ContentType.TEXT_PLAIN.value();
            });
            // Fixme: TypeMap needs working method `getMap(String.class, String.class)` to convert headers to Map<String, String>
            response.headers().keySet().forEach(rawKey -> {
                final String key = TypeConverter.convertObj(rawKey, String.class);
                if (key != null) {
                    final List<String> value = response.headers().getList(String.class, rawKey);
                    if (value != null) {
                        exchange.getResponseHeaders().put(key, value);
                    }
                }
            });
            exchange.sendResponseHeaders(statusCode, body.length);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (final IOException ignored) {
            // Response was already sent
        }
    }

    public static int nextFreePort(final int startPort) {
        for (int i = 0; i < 1024; i++) {
            final int port = i + (Math.max(startPort, 1));
            if (!isPortInUse(port)) {
                return port;
            }
        }
        throw new IllegalStateException("Could not find any free port");
    }

    public static boolean isPortInUse(final int portNumber) {
        try {
            new Socket("localhost", portNumber).close();
            return true;
        } catch (final Exception e) {
            return false;
        }
    }
}

