package berlin.yuna.nano.model;

import berlin.yuna.nano.services.http.model.ContentType;
import berlin.yuna.nano.services.http.model.HttpHeaders;
import berlin.yuna.nano.services.http.model.HttpMethod;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.typemap.logic.XmlDecoder;
import berlin.yuna.typemap.model.TypeContainer;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpRequestTest {

    private HttpObject httpObject;
    private final int PORT = 80;
    private final String HOST = "localhost";
    private final String PROTOCOL = "HTTP/1.1";
    private final String AGENT = "PostmanRuntime/7.36.3";
    private final String TOKEN = "Bearer 123";

    @BeforeEach
    void setUp() {
        final Headers headers = new Headers();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json");
        headers.add(HttpHeaders.ACCEPT, "application/json");
        headers.add(HttpHeaders.ACCEPT, "text/html");
        headers.add(HttpHeaders.ACCEPT_ENCODING, "gzip");
        headers.add(HttpHeaders.ACCEPT_ENCODING, "deflate");
        headers.add(HttpHeaders.ACCEPT_LANGUAGE, "en-US");
        headers.add(HttpHeaders.HOST, "example.com:1337");
        final String testBody = "{\"key\": \"value\"}";
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", headers, testBody);
        httpObject = new HttpObject(exchange);
    }

    @Test
    void testConstructor() {
        assertThat(httpObject.method()).isEqualTo(HttpMethod.GET.toString());
        assertThat(httpObject.path()).isEqualTo("/test");
        assertThat(httpObject.getHeaders()).containsEntry("content-type", Collections.singletonList("application/json"));
        assertThat(httpObject.exchange()).isNotNull();
    }

    @Test
    void testConvertHeaders() {
        final Headers headers = new Headers();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        final TypeMap typeMap = HttpObject.convertHeaders(headers);
        assertThat(typeMap)
            .containsEntry("content-type", Collections.singletonList("application/json"))
            .containsEntry("accept", Collections.singletonList("application/json"));
    }

    @Test
    void testIsMethod() {
        assertThat(HttpObject.isMethod(httpObject, HttpMethod.GET)).isTrue();
        assertThat(HttpObject.isMethod(httpObject, HttpMethod.POST)).isFalse();
    }

    @Test
    void testIsMethodGet() {
        assertThat(httpObject.isMethodGet()).isTrue();
        assertThat(httpObject.isMethodPost()).isFalse();
        assertThat(httpObject.isMethodPut()).isFalse();
        assertThat(httpObject.isMethodHead()).isFalse();
        assertThat(httpObject.isMethodPatch()).isFalse();
        assertThat(httpObject.isMethodDelete()).isFalse();
        assertThat(httpObject.isMethodOptions()).isFalse();
        assertThat(httpObject.isMethodTrace()).isFalse();
    }

    @Test
    void testContentTypeMethods() {
        assertThat(httpObject.hasContentType("application/json")).isTrue();
        assertThat(httpObject.hasContentTypeJson()).isTrue();
        assertThat(httpObject.hasAcceptJson()).isTrue();
        assertThat(httpObject.hasContentTypeHtml()).isFalse();
        assertThat(httpObject.hasContentTypeMp4()).isFalse();
        assertThat(httpObject.hasContentTypeGif()).isFalse();
        assertThat(httpObject.hasContentTypeJpeg()).isFalse();
        assertThat(httpObject.hasContentTypeMpeg()).isFalse();
        assertThat(httpObject.hasContentTypePdf()).isFalse();
        assertThat(httpObject.hasContentTypePng()).isFalse();
        assertThat(httpObject.hasContentTypeXml()).isFalse();
        assertThat(httpObject.hasContentTypeOctetStream()).isFalse();
        assertThat(httpObject.hasContentTypeMultiPartFormData()).isFalse();
        assertThat(httpObject.hasContentTypePlainText()).isFalse();
        assertThat(httpObject.hasContentTypeXmlSoap()).isFalse();
        assertThat(httpObject.hasContentTypeFormUrlEncoded()).isFalse();
        assertThat(httpObject.hasAcceptXml()).isFalse();
        assertThat(httpObject.hasAcceptXmlSoap()).isFalse();
        assertThat(httpObject.hasAcceptOctetStream()).isFalse();
        assertThat(httpObject.hasAcceptPdf()).isFalse();
        assertThat(httpObject.hasAcceptFormUrlEncoded()).isFalse();
        assertThat(httpObject.hasAcceptMultiPartFormData()).isFalse();
        assertThat(httpObject.hasAcceptPlainText()).isFalse();
        assertThat(httpObject.hasAcceptHtml()).isFalse();
        assertThat(httpObject.hasAcceptJpeg()).isFalse();
        assertThat(httpObject.hasAcceptPng()).isFalse();
        assertThat(httpObject.hasAcceptGif()).isFalse();
        assertThat(httpObject.hasAcceptMpeg()).isFalse();
        assertThat(httpObject.hasAcceptMp4()).isFalse();
    }

    @Test
    void testBodyMethods() {
        final String testBody = "{\"key\": \"value\"}";
        final InputStream bodyStream = new ByteArrayInputStream(testBody.getBytes(Charset.defaultCharset()));
        final InputStream oldStream = httpObject.exchange().getRequestBody();
        httpObject.exchange().setStreams(bodyStream, null);

        assertThat(httpObject.bodyAsString()).isEqualTo(testBody);
        assertThat(httpObject.bodyAsJson().get(String.class, "key")).isEqualTo("value");
//
//        HttpExchange exchange = createMockHttpExchange("GET", "/test?key1=value1&key2=value2", new Headers(), "\uD800");
//        HttpRequest request = new HttpRequest(exchange);
//        assertThat(request.bodyAsString()).isEqualTo(testBody);

        httpObject.exchange().setStreams(oldStream, null);
    }

    @Test
    void testGetRequestBody() throws IOException {
        final String testBody = "{\"key\": \"value\"}";
        final byte[] expectedStream = testBody.getBytes(Charset.defaultCharset());
        httpObject.bodyAsString();
        final byte[] actualBody = httpObject.body();
        assertThat((actualBody)).isEqualTo(expectedStream);
    }

//    private String inputStreamToString(InputStream inputStream) {
//        try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
//            return scanner.hasNext() ? scanner.next() : "";
//        }
//    }

    @Test
    void testQueryParameters() {
        final HttpExchange exchange = createMockHttpExchange("GET", "/test?key1=value1&key2=value2", new Headers(), "");
        final HttpObject request = new HttpObject(exchange);
        assertThat(request.queryParameters()).hasSize(2);
        assertThat(request.containsQueryParam("key1")).isTrue();
        assertThat(request.containsQueryParam("key2")).isTrue();
        assertThat(request.containsQueryParam("key3")).isFalse();
        assertThat(request.queryParam("key1")).isEqualTo("value1");
        assertThat(request.queryParam("key2")).isEqualTo("value2");
        assertThat(request.queryParam("key3")).isNull();
    }

    @Test
    void testExactMatch() {
        assertThat(httpObject.exactMatch("/test")).isTrue();
        assertThat(httpObject.exactMatch("/test/extra")).isFalse();
    }

    @Test
    void testMatch() {
        assertThat(httpObject.match("/test")).isTrue();
        assertThat(httpObject.match("/test/extra")).isFalse();
        assertThat(httpObject.match("/test/{param}")).isFalse();
        assertThat(httpObject.match("/test/{param}/extra")).isFalse();
    }

    @Test
    void testHeaderMethods() {
        assertThat(httpObject.header("content-type")).isEqualTo("application/json");
        assertThat(httpObject.getHeaders()).containsKey("content-type");
    }

    @Test
    void testPathParams() {
        final HttpExchange exchange = createMockHttpExchange("GET", "/test/param1/param2", new Headers(), "");
        final HttpObject request = new HttpObject(exchange);

        request.match("/test/{param1}/{param2}");
        assertThat(request.pathParams()).hasSize(2);
        assertThat(request.pathParam("param1")).isEqualTo("param1");
        assertThat(request.pathParam("param2")).isEqualTo("param2");

    }

    @Test
    void testcontainsHeader() {
        assertThat(httpObject.containsHeader("content-type")).isTrue();
        assertThat(httpObject.containsHeader("accept")).isTrue();
    }

    @Test
    void testPort() {
        assertThat(httpObject.port()).isEqualTo(1337);
    }

    @Test
    void testHost() {
        assertThat(httpObject.host()).isEqualTo("example.com");
    }

    @Test
    void testProtocol() {
        assertThat(httpObject.protocol()).isEqualTo(PROTOCOL);
    }

    @Test
    void testUserAgent() {
        final Headers headers = new Headers();
        headers.add("User-Agent", AGENT);
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", headers, "");
        final HttpObject request = new HttpObject(exchange);
        assertThat(request.userAgent()).isEqualTo(AGENT);
    }

    @Test
    void testAuthToken() {
        final Headers headers = new Headers();
        headers.add("Authorization", TOKEN);
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", headers, "");
        final HttpObject request = new HttpObject(exchange);
        assertThat(request.authToken()).isEqualTo("123");
    }

    @Test
    void testAuthBasic() {
        final Headers headers = new Headers();
        // todo : add basic token auth
    }

    @Test
    void testBasicAuth() {
        final Headers headers = new Headers();
        headers.add(HttpHeaders.AUTHORIZATION, "Basic QWxhZGRpbjpPcGVuU2VzYW1l");
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", headers, "");
        final HttpObject request = new HttpObject(exchange);
        final String[] credentials1 = request.basicAuth();
        assertThat(credentials1).containsExactly("Aladdin", "OpenSesame");
    }

    @Test
    void testPreferredLanguages() {
        final Headers headers = new Headers();
        headers.add("Accept-Language", "en-US,en;q=0.9,de;q=0.8");
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", headers, "");
        final HttpObject request = new HttpObject(exchange);
        assertThat(request.acceptLanguages()).containsExactly(Locale.of("en", "us"), Locale.ENGLISH, Locale.GERMAN);
    }

    @Test
    // todo : getList is not returning casted listed
    void testContentType() {
        final Headers headers = new Headers();
        headers.add("Content-Type", "application/json");
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", headers, "");
        final HttpObject request = new HttpObject(exchange);
        assertThat(request.contentTypes()).containsExactly(ContentType.APPLICATION_JSON);
        assertThat(httpObject.contentType()).isEqualTo(ContentType.APPLICATION_JSON);
    }

    @Test
    void testHasAccept_StringArray() {
        final boolean result = httpObject.hasAccept("application/json", "text/html");
        assertThat(result).isTrue();
    }

    @Test
    void testHasAccept_ContentType() {

        final boolean result = httpObject.hasAccept(ContentType.APPLICATION_JSON);
        assertThat(result).isTrue();
    }

    @Test
    void testAcceptEncoding() {

        final String result = httpObject.acceptEncoding();
        assertThat(result).isEqualTo("gzip");
    }

    @Test
    void testHasAcceptEncoding_StringArray() {
        final boolean result = httpObject.hasAcceptEncoding("gzip", "deflate");

        assertThat(result).isTrue();
    }

    @Test
    void testAccepts() {

        final List<ContentType> result = httpObject.accepts();
        assertThat(result).containsExactly(ContentType.APPLICATION_JSON, ContentType.TEXT_HTML);
    }

    @Test
    void testAccept() {

        final ContentType result = httpObject.accept();
        assertThat(result).isEqualTo(ContentType.APPLICATION_JSON);
    }

    @Test
    void testAcceptLanguage() {

        final List<Locale> result = httpObject.acceptLanguage();
        assertThat(result).containsExactly(Locale.US);
    }

    @Test
    void testBodyAsXml() {
        final String xmlBody = "<name>John</name>";
        final HttpExchange exchange = createMockHttpExchange("GET", "/test", new Headers(), xmlBody);
        httpObject = new HttpObject(exchange);

        final TypeContainer<?> result = httpObject.bodyAsXml();

        final TypeContainer<?> expected = XmlDecoder.xmlTypeOf(xmlBody);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void testHttpObjectMethods() {
        // Create a sample HTTP response
        final HttpObject httpObject = new HttpObject();
        httpObject.statusCode(200)
            .body("Sample body".getBytes())
            .headers(createSampleHeaders());

        assertEquals(200, httpObject.statusCode());
        assertArrayEquals("Sample body".getBytes(), httpObject.body());

//        assertEquals(createSampleHeaders(), httpObject.headers());

        httpObject.statusCode(404);
        assertEquals(404, httpObject.statusCode());

        final HttpObject httpObjectCopy = new HttpObject();
        httpObjectCopy.statusCode(404)
            .body("Sample body".getBytes())
            .headers(createSampleHeaders());
        assertEquals(httpObjectCopy, httpObject);

//        assertEquals(httpObjectCopy.hashCode(), httpObject.hashCode());
        final String expectedToString = "HttpObject[statusCode=404, body=[83, 97, 109, 112, 108, 101, 32, 98, 111, 100, 121], headers={Header1=Value1, Header2=Value2, NewHeader=Value}]";
//        assertEquals(expectedToString, httpObject.toString());
    }

    // Utility method to create sample headers
    private Map<String, String> createSampleHeaders() {
        return  Map.of("Content-Type", "application/json");
    }

    @Test
    void testHashCode() {
        final HttpObject httpObject1 = new HttpObject();
        httpObject1.statusCode(200)
            .body("Sample body".getBytes());

        final HttpObject httpObject2 = new HttpObject();
        httpObject2.statusCode(200)
            .body("Sample body".getBytes());

        assertEquals(httpObject1.hashCode(), httpObject2.hashCode());
    }

    @Test
    void testToString() {
        final HttpObject httpObject = new HttpObject();
        httpObject.statusCode(200)
            .body("Sample body".getBytes());

        final String expectedToString = "HttpObject[statusCode=200, body=[83, 97, 109, 112, 108, 101, 32, 98, 111, 100, 121], headers={}]";
        assertEquals(expectedToString, httpObject.toString());
    }


    private HttpExchange createMockHttpExchange(final String method, final String path, final Headers headers, final String testBody) {

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
                return new ByteArrayInputStream(testBody.getBytes(StandardCharsets.UTF_8));
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
