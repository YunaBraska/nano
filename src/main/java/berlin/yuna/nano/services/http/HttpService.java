package berlin.yuna.nano.services.http;

import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.core.model.Service;
import berlin.yuna.nano.core.model.Unhandled;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.services.http.logic.HttpClient;
import berlin.yuna.nano.services.http.model.HttpObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.http.HttpRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static berlin.yuna.nano.core.model.Config.CONFIG_SERVICE_HTTP_PORT;
import static berlin.yuna.nano.helper.event.model.EventType.*;
import static berlin.yuna.nano.services.http.model.HttpHeaders.ACCEPT_ENCODING;
import static berlin.yuna.nano.services.http.model.HttpHeaders.CONTENT_ENCODING;
import static berlin.yuna.nano.services.http.model.HttpObject.CONTEXT_HTTP_CLIENT_KEY;
import static berlin.yuna.typemap.logic.TypeConverter.collectionOf;
import static java.util.Optional.ofNullable;

public class HttpService extends Service {
    protected HttpServer server;
    protected Context context;

    public HttpService() {
        super(null, false);
    }

    public InetSocketAddress address() {
        return server == null ? null : server.getAddress();
    }

    public int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    public HttpServer server() {
        return server;
    }

    // important for port finding when using multiple HttpServers
    protected static final Lock STARTUP_LOCK = new ReentrantLock();

    @Override
    public void stop(final Supplier<Context> contextSub) {
        isReady.set(true, false, state -> {
            server.stop(0);
            logger.info(() -> "[{}] port [{}] stopped", name(), (server == null ? null : server.getAddress().getPort()));
            server = null;
        });
    }

    @Override
    public void start(final Supplier<Context> contextSub) {
        isReady.set(false, true, state -> {
            context = contextSub.get().newContext(HttpService.class, null);
            STARTUP_LOCK.lock();
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
                                () -> sendResponse(exchange, new HttpObject().statusCode(404).body("Page not found".getBytes()).headerMap(new HashMap<>()))
                            )
                        );
                    } catch (final Exception e) {
                        context.sendEventReturn(EVENT_APP_UNHANDLED, new Unhandled(context, httpRequest, e)).responseOpt(HttpObject.class).ifPresentOrElse(
                            response -> sendResponse(exchange, response),
                            () -> new HttpObject().statusCode(500).body("Internal Server Error".getBytes()).headerMap(new HashMap<>())
                        );
                    }
                });
                server.start();
                logger.info(() -> "[{}] starting on port [{}]", name(), port);
            } catch (final IOException e) {
                logger.error(e, () -> "[{}] failed to start with port [{}]", name(), port);
            } finally {
                STARTUP_LOCK.unlock();
            }
        });
    }

    @Override
    public void onEvent(final Event event) {
        event.ifPresent(EVENT_HTTP_REQUEST, HttpRequest.class, request -> {
            // Ignore incoming requests
            if (request instanceof final HttpObject httpObject && httpObject.exchange() != null)
                return;
            event.response(((HttpClient) context.computeIfAbsent(CONTEXT_HTTP_CLIENT_KEY, value -> new HttpClient())).send(request));
        });
        super.onEvent(event);
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
            final Map<String, List<String>> headers = response.computedHeaders(false);
            final int statusCode = response.statusCode() > -1 && response.statusCode() < 600 ? response.statusCode() : 200;
            response.headerMap().getMap(String.class, value -> collectionOf(value, String.class)).forEach((key, value) -> exchange.getResponseHeaders().put(key, value));
            headers.forEach((key, value) -> exchange.getResponseHeaders().put(key, value));
            final byte[] body = encodeBody(exchange, response.body());
            exchange.sendResponseHeaders(statusCode, body.length);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        } catch (final IOException ignored) {
            // Response was already sent
        }
    }

    protected byte[] encodeBody(final HttpExchange exchange, byte[] body) throws IOException {
        final String acceptEncoding = ofNullable(exchange.getRequestHeaders()
            .getFirst(ACCEPT_ENCODING))
            .map(String::toLowerCase)
            .map(encoding -> encoding.contains("gzip") ? "gzip" : encoding.contains("deflate") ? "deflate" : null)
            .orElse("identity");
        if (!"identity".equals(acceptEncoding)) {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (final FilterOutputStream gzipOutputStream = acceptEncoding.contains("gzip") ? new GZIPOutputStream(byteArrayOutputStream) : new DeflaterOutputStream(byteArrayOutputStream)) {
                gzipOutputStream.write(body);
            }
            body = byteArrayOutputStream.toByteArray();
        }
        exchange.getResponseHeaders().set(CONTENT_ENCODING, acceptEncoding);
        return body;
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

