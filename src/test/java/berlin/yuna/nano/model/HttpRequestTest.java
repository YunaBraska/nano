package berlin.yuna.nano.model;

import berlin.yuna.nano.services.http.model.ContentType;
import berlin.yuna.nano.services.http.model.HttpMethod;
import berlin.yuna.nano.services.http.model.HttpRequest;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestTest {

    private HttpRequest httpRequest;
    private final int PORT = 80;
    private final String HOST = "localhost";
    private final String PROTOCOL = "HTTP/1.1";
    private final String AGENT = "PostmanRuntime/7.36.3";
    private final String TOKEN = "Bearer 123";

    @BeforeEach
    void setUp() {
        Headers headers = new Headers();
        headers.add("Content-Type", "application/json");
        HttpExchange exchange = createMockHttpExchange("GET", "/test", headers);
        httpRequest = new HttpRequest(exchange);
    }

    @Test
    void testConstructor() {
        assertThat(httpRequest.method()).isEqualTo(HttpMethod.GET.toString());
        assertThat(httpRequest.path()).isEqualTo("/test");
        assertThat(httpRequest.getHeaders()).containsEntry("content-type", Collections.singletonList("application/json"));
        assertThat(httpRequest.exchange()).isNotNull();
    }

    @Test
    void testConvertHeaders() {
        Headers headers = new Headers();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        TypeMap typeMap = HttpRequest.convertHeaders(headers);
        assertThat(typeMap).containsEntry("content-type", Collections.singletonList("application/json"));
        assertThat(typeMap).containsEntry("accept", Collections.singletonList("application/json"));
    }

    @Test
    void testIsMethod() {
        assertThat(HttpRequest.isMethod(httpRequest, HttpMethod.GET)).isTrue();
        assertThat(HttpRequest.isMethod(httpRequest, HttpMethod.POST)).isFalse();
    }

    @Test
    void testIsMethodGet() {
        assertThat(httpRequest.isMethodGet()).isTrue();
        assertThat(httpRequest.isMethodPost()).isFalse();
        assertThat(httpRequest.isMethodPut()).isFalse();
        assertThat(httpRequest.isMethodHead()).isFalse();
    }

    @Test
    void testContentTypeMethods() {
        assertThat(httpRequest.hasContentType("application/json")).isTrue();
        assertThat(httpRequest.hasContentTypeJson()).isTrue();
        assertThat(httpRequest.hasContentTypeXml()).isFalse();
    }

    @Test
    void testBodyMethods() throws IOException {
        String testBody = "{\"key\": \"value\"}";
        InputStream bodyStream = new ByteArrayInputStream(testBody.getBytes(Charset.defaultCharset()));
        InputStream oldStream = httpRequest.exchange().getRequestBody();
        httpRequest.exchange().setStreams(bodyStream, null);

        assertThat(httpRequest.bodyAsString()).isEqualTo(testBody);
        assertThat(httpRequest.bodyAsJson().get(String.class, "key")).isEqualTo("value");

        httpRequest.exchange().setStreams(oldStream, null);
    }

    @Test
    void testGetRequestBody() throws IOException {
        String testBody = "{\"key\": \"value\"}";
        InputStream expectedStream = new ByteArrayInputStream(testBody.getBytes(Charset.defaultCharset()));
        InputStream oldStream = httpRequest.exchange().getRequestBody();
        httpRequest.exchange().setStreams(expectedStream, null);

//        byte[] actualBody = httpRequest.body();
//        assertThat((actualBody)).isEqualTo(inputStreamToString(expectedStream));
//        httpRequest.exchange().setStreams(oldStream, null);
    }

    private String inputStreamToString(InputStream inputStream) {
        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    @Test
    void testQueryParameters() {
        HttpExchange exchange = createMockHttpExchange("GET", "/test?key1=value1&key2=value2", new Headers());
        HttpRequest request = new HttpRequest(exchange);
        assertThat(request.queryParameters().size()).isEqualTo(2);
        assertThat(request.containsQueryParam("key1")).isTrue();
        assertThat(request.containsQueryParam("key2")).isTrue();
        assertThat(request.containsQueryParam("key3")).isFalse();
        assertThat(request.queryParam("key1")).isEqualTo("value1");
        assertThat(request.queryParam("key2")).isEqualTo("value2");
        assertThat(request.queryParam("key3")).isNull();
    }

    @Test
    void testExactMatch() {
        assertThat(httpRequest.exactMatch("/test")).isTrue();
        assertThat(httpRequest.exactMatch("/test/extra")).isFalse();
    }

    @Test
    void testMatch() {
        assertThat(httpRequest.match("/test")).isTrue();
        assertThat(httpRequest.match("/test/extra")).isFalse();
        assertThat(httpRequest.match("/test/{param}")).isFalse();
        assertThat(httpRequest.match("/test/{param}/extra")).isFalse();
    }

    @Test
    void testHeaderMethods() {
        assertThat(httpRequest.header("content-type")).isEqualTo("application/json");
        assertThat(httpRequest.getHeaders()).containsKey("content-type");
    }

    @Test
    void testPathParams() {
        HttpExchange exchange = createMockHttpExchange("GET", "/test/param1/param2", new Headers());
        HttpRequest request = new HttpRequest(exchange);

        request.match("/test/{param1}/{param2}");
        assertThat(request.pathParams().size()).isEqualTo(2);
        assertThat(request.pathParam("param1")).isEqualTo("param1");
        assertThat(request.pathParam("param2")).isEqualTo("param2");

    }

    @Test
    void testcontainsHeader() {
        assertThat(httpRequest.containsHeader("content-type")).isTrue();
        assertThat(httpRequest.containsHeader("accept")).isFalse();
    }

    @Test
    void testPort() {
        assertThat(httpRequest.port()).isEqualTo(PORT);
    }

    @Test
    void testHost() {
        assertThat(httpRequest.host()).isEqualTo(HOST);
    }

    @Test
    void testProtocol() {
        assertThat(httpRequest.protocol()).isEqualTo(PROTOCOL);
    }

    @Test
    void testUserAgent() {
        Headers headers = new Headers();
        headers.add("User-Agent", AGENT);
        HttpExchange exchange = createMockHttpExchange("GET", "/test", headers);
        HttpRequest request = new HttpRequest(exchange);
        assertThat(request.userAgent()).isEqualTo(AGENT);
    }

    @Test
    void testAuthorisationToken() {
        Headers headers = new Headers();
        headers.add("Authorization", TOKEN);
        HttpExchange exchange = createMockHttpExchange("GET", "/test", headers);
        HttpRequest request = new HttpRequest(exchange);
        assertThat(request.authToken()).isEqualTo("123");
    }

    @Test
    void testPreferredLanguages() {
        Headers headers = new Headers();
        headers.add("Accept-Language", "en-US,en;q=0.9,de;q=0.8");
        HttpExchange exchange = createMockHttpExchange("GET", "/test", headers);
        HttpRequest request = new HttpRequest(exchange);
        assertThat(request.acceptLanguages()).containsExactly(Locale.of("en", "us"), Locale.ENGLISH, Locale.GERMAN);
    }

    @Test
    void testContentType() {
        Headers headers = new Headers();
        headers.add("Content-Type", "application/json");
        HttpExchange exchange = createMockHttpExchange("GET", "/test", headers);
        HttpRequest request = new HttpRequest(exchange);
        assertThat(request.contentTypes()).containsExactly(ContentType.APPLICATION_JSON);
    }


    private HttpExchange createMockHttpExchange(String method, String path, Headers headers) {
        String testBody = "{\"key\": \"value\"}";
        InputStream bodyStream = new ByteArrayInputStream(testBody.getBytes(StandardCharsets.UTF_8));

        return new HttpExchange() {
            @Override
            public Headers getRequestHeaders() {
                return headers;
            }

            @Override
            public Headers getResponseHeaders() {
                return null;
            }

            @Override
            public URI getRequestURI() {
                return URI.create("http://localhost" + path);
            }

            @Override
            public String getRequestMethod() {
                return method;
            }

            @Override
            public HttpContext getHttpContext() {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getRequestBody() {
                return bodyStream;
            }

            @Override
            public OutputStream getResponseBody() {
                return null;
            }

            @Override
            public void sendResponseHeaders(final int rCode, final long responseLength) throws IOException {

            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public int getResponseCode() {
                return 0;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return new InetSocketAddress(HOST, PORT);
            }

            @Override
            public String getProtocol() {
                return PROTOCOL;
            }

            @Override
            public Object getAttribute(final String name) {
                return null;
            }

            @Override
            public void setAttribute(final String name, final Object value) {

            }

            @Override
            public void setStreams(final InputStream i, final OutputStream o) {

            }

            @Override
            public HttpPrincipal getPrincipal() {
                return null;
            }
        };
    }
}
