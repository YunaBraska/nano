package berlin.yuna.nano.model;

import berlin.yuna.nano.services.http.model.HttpHeaders;
import berlin.yuna.nano.services.http.model.HttpMethod;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.typemap.model.TypeList;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static berlin.yuna.nano.services.http.model.ContentType.*;
import static berlin.yuna.nano.services.http.model.HttpHeaders.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpRequestTest {

    @Test
    void testConstructor_withHttpExchange() {
        final Headers headers = new Headers();
        headers.add(CONTENT_TYPE, APPLICATION_JSON.value());
        final HttpObject httpObject = new HttpObject(createMockHttpExchange("GET", "/test", headers, "{\"key\": \"value\"}"));

        assertThat(httpObject.method()).isEqualTo(HttpMethod.GET);
        assertThat(httpObject.path()).isEqualTo("/test");
        assertThat(httpObject.headers()).containsEntry(CONTENT_TYPE, Collections.singletonList(APPLICATION_JSON.value()));
        assertThat(httpObject.exchange()).isNotNull();
    }

    @Test
    void testBuilder() {
        final HttpObject httpObject = new HttpObject()
            .method(HttpMethod.GET)
            .path("/test")
            .statusCode(-99)
            .contentType(APPLICATION_JSON);

        assertThat(httpObject.method()).isEqualTo(HttpMethod.GET);
        assertThat(httpObject.path()).isEqualTo("/test");
        assertThat(httpObject.headers()).containsEntry(CONTENT_TYPE, APPLICATION_JSON.value());
        assertThat(httpObject.statusCode()).isEqualTo(-99);
        assertThat(httpObject.exchange()).isNull();
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
        for (final HttpMethod testMethod : HttpMethod.values()) {
            final HttpObject httpObject = new HttpObject().method(testMethod);
            for (final HttpMethod otherMethod : HttpMethod.values()) {
                assertThat(HttpObject.isMethod(httpObject, otherMethod)).isEqualTo(otherMethod == testMethod);
            }
        }
    }

    @Test
    void testIsMethodGet() {
        final HttpObject httpObject = new HttpObject().method(HttpMethod.GET);
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
    void testSetMethod() {
        assertThat(new HttpObject().method(HttpMethod.GET).method()).isEqualTo(HttpMethod.GET);
        assertThat(new HttpObject().method("PUT").method()).isEqualTo(HttpMethod.PUT);
        assertThat(new HttpObject().method("pAtCh").method()).isEqualTo(HttpMethod.PATCH);
        assertThat(new HttpObject().method("unknown").method()).isNull();
    }

    @Test
    void testHeaderContentType() {
        //TODO: test use case "Content-Type: application/json; charset=utf-8"

        // ENUM
        assertThat(new HttpObject().contentType(APPLICATION_JSON).contentType()).isEqualTo(APPLICATION_JSON);
        assertThat(new HttpObject().contentType(APPLICATION_JSON).contentTypes()).containsExactly(APPLICATION_JSON);
        assertThat(new HttpObject().contentType(APPLICATION_JSON, TEXT_PLAIN).contentTypes()).containsExactly(APPLICATION_JSON, TEXT_PLAIN);
        assertThat(new HttpObject().contentType(APPLICATION_JSON, TEXT_PLAIN).header(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.value() + ", " + TEXT_PLAIN.value());
        assertThat(new HttpObject().contentType(APPLICATION_JSON, TEXT_PLAIN).hasContentType(APPLICATION_JSON)).isTrue();
        assertThat(new HttpObject().contentType(APPLICATION_JSON, TEXT_PLAIN).hasContentType(APPLICATION_JSON, TEXT_PLAIN)).isTrue();
        assertThat(new HttpObject().contentType(APPLICATION_JSON, TEXT_PLAIN).hasContentType(APPLICATION_JSON, TEXT_PLAIN, APPLICATION_PDF)).isFalse();

        // STRING
        assertThat(new HttpObject().contentType("application/json").contentType()).isEqualTo(APPLICATION_JSON);
        assertThat(new HttpObject().contentType("application/json").contentTypes()).containsExactly(APPLICATION_JSON);
        assertThat(new HttpObject().contentType("application/json", "text/plain").contentTypes()).containsExactly(APPLICATION_JSON, TEXT_PLAIN);
        assertThat(new HttpObject().contentType("application/json", "TexT/Plain").header(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.value() + ", " + TEXT_PLAIN.value());
        assertThat(new HttpObject().contentType("application/json", "TexT/Plain").hasContentType(APPLICATION_JSON.value())).isTrue();
        assertThat(new HttpObject().contentType("application/json", "TexT/Plain").hasContentType(APPLICATION_JSON.value(), TEXT_PLAIN.value())).isTrue();
        assertThat(new HttpObject().contentType("application/json", "TexT/Plain").hasContentType(APPLICATION_JSON.value(), TEXT_PLAIN.value(), APPLICATION_PDF.value())).isFalse();

        // General
        assertThat(new HttpObject().contentTypes()).isEmpty();
        assertThat(new HttpObject().contentType()).isNull();
    }

    @Test
    void testHasContentTypeMethods() {
        final HttpObject httpObject = new HttpObject().contentType(APPLICATION_JSON);
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
    void testHeaderAccept() {
        // ENUM
        assertThat(new HttpObject().accept(APPLICATION_JSON).accept()).isEqualTo(APPLICATION_JSON);
        assertThat(new HttpObject().accept(APPLICATION_JSON).accepts()).containsExactly(APPLICATION_JSON);
        assertThat(new HttpObject().accept(APPLICATION_JSON, TEXT_PLAIN).accepts()).containsExactly(APPLICATION_JSON, TEXT_PLAIN);
        assertThat(new HttpObject().accept(APPLICATION_JSON, TEXT_PLAIN).header(ACCEPT)).isEqualTo(APPLICATION_JSON.value() + ", " + TEXT_PLAIN.value());
        assertThat(new HttpObject().accept(APPLICATION_JSON, TEXT_PLAIN).hasAccept(APPLICATION_JSON)).isTrue();
        assertThat(new HttpObject().accept(APPLICATION_JSON, TEXT_PLAIN).hasAccept(APPLICATION_JSON, TEXT_PLAIN)).isTrue();
        assertThat(new HttpObject().accept(APPLICATION_JSON, TEXT_PLAIN).hasAccept(APPLICATION_JSON, TEXT_PLAIN, APPLICATION_PDF)).isFalse();

        // STRING
        assertThat(new HttpObject().accept("application/json").accept()).isEqualTo(APPLICATION_JSON);
        assertThat(new HttpObject().accept("application/json").accepts()).containsExactly(APPLICATION_JSON);
        assertThat(new HttpObject().accept("application/json", "text/plain").accepts()).containsExactly(APPLICATION_JSON, TEXT_PLAIN);
        assertThat(new HttpObject().accept("application/json", "TexT/Plain").header(ACCEPT)).isEqualTo(APPLICATION_JSON.value() + ", " + TEXT_PLAIN.value());
        assertThat(new HttpObject().accept("application/json", "text/plain").hasAccept(APPLICATION_JSON.value())).isTrue();
        assertThat(new HttpObject().accept("application/json", "text/plain").hasAccept(APPLICATION_JSON.value(), TEXT_PLAIN.value())).isTrue();
        assertThat(new HttpObject().accept("application/json", "text/plain").hasAccept(APPLICATION_JSON.value(), TEXT_PLAIN.value(), APPLICATION_PDF.value())).isFalse();

        // General
        assertThat(new HttpObject().accepts()).isEmpty();
        assertThat(new HttpObject().accept()).isNull();
    }

    @Test
    void testBody() {
        final String bodyString = "{\"key\":\"value\"}";
        final TypeMap bodyJson = new TypeMap().putReturn("key", "value");
        final byte[] bodyBytes = bodyString.getBytes(Charset.defaultCharset());
        //TODO: test bodyAsXml

        // null body
        final HttpObject nullTest = new HttpObject();
        assertThat(nullTest.body()).isEqualTo(new byte[0]);
        assertThat(nullTest.bodyAsString()).isEmpty();
        assertThat(nullTest.bodyAsJson()).isEqualTo(new TypeList().addReturn(""));
        assertThat(nullTest.bodyAsXml()).isNotNull();
        assertThat(nullTest.bodyAsJson().get(String.class, "key")).isNull();

        // Byte[] body
        final HttpObject byteTest = new HttpObject().body(bodyString.getBytes(Charset.defaultCharset()));
        assertThat(byteTest.body()).isEqualTo(bodyBytes);
        assertThat(byteTest.bodyAsString()).isEqualTo(bodyString);
        assertThat(byteTest.bodyAsJson()).isEqualTo(bodyJson);
        assertThat(byteTest.bodyAsXml()).isNotNull();
        assertThat(byteTest.bodyAsJson().get(String.class, "key")).isEqualTo("value");

        // String body
        final HttpObject stringTest = new HttpObject().body(bodyString);
        assertThat(stringTest.body()).isEqualTo(bodyBytes);
        assertThat(stringTest.bodyAsString()).isEqualTo(bodyString);
        assertThat(stringTest.bodyAsJson()).isEqualTo(bodyJson);
        assertThat(stringTest.bodyAsXml()).isNotNull();
        assertThat(stringTest.bodyAsJson().get(String.class, "key")).isEqualTo("value");

        // JSON body
        final HttpObject jsonTest = new HttpObject().body(bodyJson);
        assertThat(jsonTest.body()).isEqualTo(bodyBytes);
        assertThat(jsonTest.bodyAsString()).isEqualTo(bodyString);
        assertThat(jsonTest.bodyAsJson()).isEqualTo(bodyJson);
        assertThat(jsonTest.bodyAsXml()).isNotNull();
        assertThat(jsonTest.bodyAsJson().get(String.class, "key")).isEqualTo("value");

        // HttpExchange body
        final HttpObject exchangeTest = new HttpObject(createMockHttpExchange("GET", "/test", new Headers(), bodyString));
        assertThat(exchangeTest.body()).isEqualTo(bodyBytes);
        assertThat(exchangeTest.bodyAsString()).isEqualTo(bodyString);
        assertThat(exchangeTest.bodyAsJson()).isEqualTo(bodyJson);
        assertThat(exchangeTest.bodyAsXml()).isNotNull();
        assertThat(exchangeTest.bodyAsJson().get(String.class, "key")).isEqualTo("value");

        // General
        assertThat(new HttpObject().bodyAsJson()).isEqualTo(new TypeList().addReturn(""));
        assertThat(new HttpObject().bodyAsXml()).isEqualTo(new TypeList());
        assertThat(new HttpObject().bodyAsString()).isEmpty();
    }

    @Test
    void testQueryParameters() {
        final HttpObject httpObject = new HttpObject().path("/test?key1=value1&key2=value2");
        assertThat(httpObject.queryParams()).hasSize(2);
        assertThat(httpObject.containsQueryParam("key1")).isTrue();
        assertThat(httpObject.containsQueryParam("key2")).isTrue();
        assertThat(httpObject.containsQueryParam("key3")).isFalse();
        assertThat(httpObject.queryParam("key1")).isEqualTo("value1");
        assertThat(httpObject.queryParam("key2")).isEqualTo("value2");
        assertThat(httpObject.queryParam("key3")).isNull();

        // General
        assertThat(new HttpObject(createMockHttpExchange("GET", "/test?key1=value1&key2=value2", new Headers(), "{\"key\": \"value\"}")).queryParams()).hasSize(2);
        assertThat(new HttpObject().queryParams()).isEmpty();
    }

    @Test
    void testPathMatcher() {
        // no ending /
        final HttpObject httpObject1 = new HttpObject().path("/aa/bb/cc/dd?myNumber=2468");
        assertThat(httpObject1.pathMatch("/aa/bb/cc/dd")).isTrue();
        assertThat(httpObject1.pathMatch("/aa/bb/cc/dd/")).isTrue();
        assertThat(httpObject1.pathMatch("/aa/{value1}/cc/{value2}/ee")).isFalse();
        assertThat(httpObject1.pathMatch("/aa/{value1}/cc/ee")).isFalse();
        assertThat(httpObject1.pathMatch("/aa/{value1}/cc/d")).isFalse();
        assertThat(httpObject1.pathMatch("/aa/{value1}/cc/{value2}")).isTrue();
        assertThat(httpObject1.pathMatch("/aa/{value1}/cc/{value2}/")).isTrue();
        assertThat(httpObject1.pathParam("value1")).isEqualTo("bb");
        assertThat(httpObject1.pathParams().get(String.class, "value2")).isEqualTo("dd");
        assertThat(httpObject1.queryParams().get(Integer.class, "myNumber")).isEqualTo(2468);

        // with ending /
        final HttpObject httpObject2 = new HttpObject().path("/aa/bb/cc/dd/?myNumber=2468");
        assertThat(httpObject2.pathMatch("/aa/bb/cc/dd")).isTrue();
        assertThat(httpObject2.pathMatch("/aa/bb/cc/dd/")).isTrue();
        assertThat(httpObject2.pathMatch("/aa/{value1}/cc/{value2}/ee")).isFalse();
        assertThat(httpObject2.pathMatch("/aa/{value1}/cc/ee")).isFalse();
        assertThat(httpObject2.pathMatch("/aa/{value1}/cc/d")).isFalse();
        assertThat(httpObject2.pathMatch("/aa/{value1}/cc/{value2}")).isTrue();
        assertThat(httpObject2.pathMatch("/aa/{value1}/cc/{value2}/")).isTrue();
        assertThat(httpObject1.pathParam("value1")).isEqualTo("bb");
        assertThat(httpObject2.pathParams().get(String.class, "value2")).isEqualTo("dd");
        assertThat(httpObject2.queryParams().get(Integer.class, "myNumber")).isEqualTo(2468);

        // General
        assertThat(new HttpObject().path(null).pathMatch("/aa/bb/cc/dd")).isFalse();
        assertThat(new HttpObject().path(null).pathMatch(null)).isFalse();
    }

    @Test
    void testHeaders() {
        // set headers
        final Headers headers = new Headers();
        headers.add("Content-Type", "Application/Json, TexT/Plain");
        headers.put("Accept", List.of(APPLICATION_PDF.value(), APPLICATION_JSON.value()));
        headers.add("myNumber", "123");
        final HttpObject httpObject1 = new HttpObject().headers(headers);

        // set headers map
        final HttpObject httpObject2 = new HttpObject().headers(Map.of(
            "Content-Type", "Application/Json, TexT/Plain",
            "Accept", List.of(APPLICATION_PDF, APPLICATION_JSON.value()),
            "myNumber", "123"
        ));

        // add headers
        final HttpObject httpObject3 = new HttpObject()
            .header("Content-Type", "Application/Json, TexT/Plain")
            .header("Accept", List.of(APPLICATION_PDF, APPLICATION_JSON.value()))
            .header("myNumber", "123")
            .header(null, "aa")
            .header("bb", null);

        for (final HttpObject httpObject : List.of(httpObject1, httpObject2, httpObject3)) {
            assertThat(httpObject.headers()).hasSize(3);
            assertThat(httpObject.containsHeader("mynumber")).isTrue();
            assertThat(httpObject.containsHeader("myNumber")).isTrue();
            assertThat(httpObject.containsHeader("invalid")).isFalse();
            assertThat(httpObject.containsHeader(null)).isFalse();
            assertThat(httpObject.header("myNumber")).isEqualTo("123");
            assertThat(httpObject.header("mynumber")).isEqualTo("123");
            assertThat(httpObject.headers().get(Integer.class, "mynumber")).isEqualTo(123);
            assertThat(httpObject.contentTypes()).containsExactly(APPLICATION_JSON, TEXT_PLAIN);
            assertThat(httpObject.accepts()).containsExactly(APPLICATION_PDF, APPLICATION_JSON);
        }
    }

    @Test
    void testCaller() {
        final HttpObject httpObject1 = new HttpObject().header(HttpHeaders.HOST, "example.com:1337");
        assertThat(httpObject1.host()).isEqualTo("example.com");
        assertThat(httpObject1.address()).isNull();
        assertThat(httpObject1.port()).isEqualTo(1337);

        // with exchange
        final HttpObject httpObject2 = new HttpObject(createMockHttpExchange("GET", "/test", new Headers(), ""));
        assertThat(httpObject2.host()).isEqualTo("example.com");
        assertThat(httpObject2.address()).isNotNull();
        assertThat(httpObject2.port()).isEqualTo(1337);
    }

    @Test
    void testProtocol() {
        final HttpObject httpObject1 = new HttpObject().header(HttpHeaders.HOST, "example.com:1337");
        assertThat(httpObject1.protocol()).isNull();

        // with exchange
        final HttpObject httpObject2 = new HttpObject(createMockHttpExchange("GET", "/test", new Headers(), ""));
        assertThat(httpObject2.protocol()).isEqualTo("HTTP/1.1");
    }

    @Test
    void testHeaderUserAgent() {
        assertThat(new HttpObject().header(USER_AGENT, "PostmanRuntime/7.36.3").header(USER_AGENT)).isEqualTo("PostmanRuntime/7.36.3");
        assertThat(new HttpObject().header(USER_AGENT, "PostmanRuntime/7.36.3").userAgent()).isEqualTo("PostmanRuntime/7.36.3");
    }

    @Test
    void testAuthToken() {
        assertThat(new HttpObject().header(AUTHORIZATION, "Bearer 123").authToken()).containsExactly("123");
        assertThat(new HttpObject().header(AUTHORIZATION, "Basic QWxhZGRpbjpPcGVuU2VzYW1l").authToken()).containsExactly("Aladdin", "OpenSesame");
    }

    @Test
    void testHeaderLanguage() {
        final HttpObject httpObject = new HttpObject().header(ACCEPT_LANGUAGE, "en-US,en;q=0.9,de;q=0.8");
        assertThat(httpObject.acceptLanguage()).isEqualTo(Locale.of("en", "us"));
        assertThat(httpObject.acceptLanguages()).containsExactly(Locale.of("en", "us"), Locale.ENGLISH, Locale.GERMAN);

        // General
        assertThat(new HttpObject().acceptLanguages()).isEmpty();
        assertThat(new HttpObject().acceptLanguage()).isNull();
    }

    @Test
    void testHeaderAcceptEncoding() {
        final HttpObject httpObject = new HttpObject().header(ACCEPT_ENCODING, List.of("gzip", "deflate"));
        assertThat(httpObject.acceptEncoding()).isEqualTo("gzip");
        assertThat(httpObject.acceptEncodings()).containsExactly("gzip", "deflate");
        assertThat(httpObject.hasAcceptEncoding("gzip", "deflate")).isTrue();
        assertThat(httpObject.hasAcceptEncoding("gzip", "deflate", "unknown")).isFalse();

        // General
        assertThat(new HttpObject().acceptEncodings()).isEmpty();
        assertThat(new HttpObject().acceptEncoding()).isNull();
    }

    @Test
    void testHttpObjectMethods() {
        // Create a sample HTTP response
//        final HttpObject httpObject = new HttpObject();
//        httpObject.statusCode(200)
//            .body("Sample body".getBytes())
//            .headers(createSampleHeaders());
//
//        assertEquals(200, httpObject.statusCode());
//        assertArrayEquals("Sample body".getBytes(), httpObject.body());
//
////        assertEquals(createSampleHeaders(), httpObject.headers());
//
//        httpObject.statusCode(404);
//        assertEquals(404, httpObject.statusCode());
//
//        final HttpObject httpObjectCopy = new HttpObject();
//        httpObjectCopy.statusCode(404)
//            .body("Sample body".getBytes())
//            .headers(createSampleHeaders());
//        assertEquals(httpObjectCopy, httpObject);

//        assertEquals(httpObjectCopy.hashCode(), httpObject.hashCode());
//        final String expectedToString = "HttpObject[statusCode=404, body=[83, 97, 109, 112, 108, 101, 32, 98, 111, 100, 121], headers={Header1=Value1, Header2=Value2, NewHeader=Value}]";
//        assertEquals(expectedToString, httpObject.toString());
    }

    // Utility method to create sample headers
    private Map<String, String> createSampleHeaders() {
        return Map.of("Content-Type", "application/json");
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
                return new ByteArrayInputStream(testBody.getBytes(Charset.defaultCharset()));
            }

            @Override
            public OutputStream getResponseBody() {
                return null;
            }

            @Override
            public void sendResponseHeaders(final int rCode, final long responseLength) {

            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return new InetSocketAddress("example.com", 1337);
            }

            @Override
            public int getResponseCode() {
                return 0;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public String getProtocol() {
                return "HTTP/1.1";
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
