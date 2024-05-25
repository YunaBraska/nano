package berlin.yuna.nano.services.http.logic;

import berlin.yuna.nano.core.Nano;
import berlin.yuna.nano.core.model.Context;
import berlin.yuna.nano.helper.NanoUtils;
import berlin.yuna.nano.helper.event.model.Event;
import berlin.yuna.nano.services.http.HttpService;
import berlin.yuna.nano.services.http.model.HttpObject;
import berlin.yuna.typemap.model.LinkedTypeMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static berlin.yuna.nano.core.config.TestConfig.TEST_LOG_LEVEL;
import static berlin.yuna.nano.core.config.TestConfig.TEST_REPEAT;
import static berlin.yuna.nano.core.model.Config.*;
import static berlin.yuna.nano.core.model.NanoThread.VIRTUAL_THREAD_POOL;
import static berlin.yuna.nano.helper.event.model.EventChannel.EVENT_HTTP_REQUEST;
import static berlin.yuna.nano.services.http.model.ContentType.*;
import static berlin.yuna.nano.services.http.model.HttpHeaders.*;
import static berlin.yuna.nano.services.http.model.HttpMethod.GET;
import static berlin.yuna.typemap.logic.TypeConverter.convertObj;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpClient.Version.HTTP_2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@Execution(ExecutionMode.CONCURRENT)
public class HttpClientTest {

    protected static String serverUrl;
    protected static Nano nano;

    @BeforeAll
    static void beforeAll() {
        final HttpService server = new HttpService();
        nano = new Nano(Map.of(CONFIG_LOG_LEVEL, TEST_LOG_LEVEL), server).subscribeEvent(EVENT_HTTP_REQUEST, HttpClientTest::mimicRequest);
        serverUrl = "http://localhost:" + server.port();
    }

    @AfterAll
    static void afterAll() {
        assertThat(nano.stop(HttpClientTest.class).waitForStop().isReady()).isFalse();
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_with_defaults() throws InterruptedException {
        final HttpClient httpClient = new HttpClient();
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.context()).isNull();
        assertThat(httpClient.client()).isNotNull();
        assertThat(httpClient.client().executor()).contains(VIRTUAL_THREAD_POOL);
        assertThat(httpClient.readTimeoutMs()).isEqualTo(10000);
        assertThat(httpClient.connectionTimeoutMs()).isEqualTo(5000L);
        assertThat(httpClient.followRedirects()).isTrue();
        assertThat(httpClient.version()).isEqualTo(HTTP_2);
        assertThat(httpClient.retries()).isEqualTo(3);
        assertWorkingHttpClient(httpClient);
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withCustomClient() throws InterruptedException {
        final java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(789)).build();
        final HttpClient httpClient = new HttpClient(null, client);
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.context()).isNull();
        assertThat(httpClient.client()).isEqualTo(client);
        assertThat(httpClient.client().executor()).isEmpty();
        assertThat(httpClient.readTimeoutMs()).isEqualTo(10000);
        assertThat(httpClient.connectionTimeoutMs()).isEqualTo(789L);
        assertThat(httpClient.followRedirects()).isFalse();
        assertThat(httpClient.version()).isEqualTo(HTTP_2);
        assertThat(httpClient.retries()).isEqualTo(3);
        assertWorkingHttpClient(httpClient);
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withContext() throws InterruptedException {
        final Context context = Context.createRootContext();
        final HttpClient httpClient = new HttpClient(context);
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.context()).isEqualTo(context);
        assertThat(httpClient.client()).isNotNull();
        assertThat(httpClient.client().executor()).contains(VIRTUAL_THREAD_POOL);
        assertThat(httpClient.readTimeoutMs()).isEqualTo(10000);
        assertThat(httpClient.connectionTimeoutMs()).isEqualTo(5000L);
        assertThat(httpClient.followRedirects()).isTrue();
        assertThat(httpClient.version()).isEqualTo(HTTP_2);
        assertThat(httpClient.retries()).isEqualTo(3);
        assertWorkingHttpClient(httpClient);
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_withContextAndClient() throws InterruptedException {
        final Context context = Context.createRootContext();
        final java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(789)).build();
        final HttpClient httpClient = new HttpClient(context, client);
        assertThat(httpClient).isNotNull();
        assertThat(httpClient.context()).isEqualTo(context);
        assertThat(httpClient.client()).isEqualTo(client);
        assertThat(httpClient.client().executor()).isEmpty();
        assertThat(httpClient.readTimeoutMs()).isEqualTo(10000);
        assertThat(httpClient.connectionTimeoutMs()).isEqualTo(789L);
        assertThat(httpClient.followRedirects()).isFalse();
        assertThat(httpClient.version()).isEqualTo(HTTP_2);
        assertThat(httpClient.retries()).isEqualTo(3);
        assertWorkingHttpClient(httpClient);
    }

    @RepeatedTest(TEST_REPEAT)
    void constructor_configTest() {
        final Context context = Context.createRootContext();
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_VERSION.id(), HTTP_1_1)).version()).isEqualTo(HTTP_1_1);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_VERSION.id(), 2)).version()).isEqualTo(HTTP_2);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_VERSION.id(), "1")).version()).isEqualTo(HTTP_1_1);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_MAX_RETRIES.id(), "1")).retries()).isEqualTo(1);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_MAX_RETRIES.id(), 2)).retries()).isEqualTo(2);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_CON_TIMEOUT_MS.id(), 128)).connectionTimeoutMs()).isEqualTo(128L);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_READ_TIMEOUT_MS.id(), 256)).readTimeoutMs()).isEqualTo(256);
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_FOLLOW_REDIRECTS.id(), false)).followRedirects()).isFalse();
        assertThat(new HttpClient(context.put(CONFIG_HTTP_CLIENT_FOLLOW_REDIRECTS.id(), true)).followRedirects()).isTrue();
        assertThat(new HttpClient()).hasToString("HttpClient{version=HTTP_2, retries=3, followRedirects=true, readTimeoutMs=10000, connectionTimeoutMs=5000}");
    }

    @RepeatedTest(TEST_REPEAT)
    void sendRequestViaEvent() {
        final HttpObject response = nano.newContext(HttpClientTest.class)
            .sendEventReturn(EVENT_HTTP_REQUEST, new HttpObject().path(serverUrl).body("{Hällo Wörld?!}"))
            .response(HttpObject.class);
        assertThat(response.failure()).isNull();
        assertThat(response.bodyAsString()).isEqualTo("{Hällo Wörld?!}");
        assertThat(response.header(CONTENT_LENGTH)).isEqualTo("17");
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.value());
        assertThat(response.header(CONTENT_RANGE)).isNull();
    }

    @Test
    void verifyInvalidUrl() {
        final HttpClient client = new HttpClient(Context.createRootContext()
            .put(CONFIG_HTTP_CLIENT_MAX_RETRIES.id(), 1)
            .put(CONFIG_HTTP_CLIENT_CON_TIMEOUT_MS.id(), 128)
            .put(CONFIG_HTTP_CLIENT_READ_TIMEOUT_MS.id(), 128)
        );
        if (client.connectionTimeoutMs() < 1000) {
            final HttpObject response = client.send(new HttpObject().path("http://localhost/invalid/url"));
            assertThat(response.failure()).isExactlyInstanceOf(ConnectException.class);
            assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_PROBLEM_JSON.value());
            assertThat((LinkedTypeMap) response.bodyAsJson()).contains(
                entry("instance", "http://localhost/invalid/url"),
                entry("status", -1L),
                entry("title", "ConnectException"),
                entry("type", "https://github.com/YunaBraska/nano")
            ).containsKey("id").containsKey("detail").containsKey("timestamp");
        }
    }

    public static void assertWorkingHttpClient(final HttpClient client) throws InterruptedException {
        HttpObject response;

        // verify header request
        response = client.send(new HttpObject().path(serverUrl + "/status/200/Content-Range/bytes.0-0_1234"));
        assertThat(response.failure()).isNull();
        assertThat(response.header(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(response.header(CONTENT_RANGE)).isEqualTo("bytes 0-0/1234");
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_OCTET_STREAM.value());
        assertThat(response.size()).isEqualTo(1234L);

        // verify invalid header range response
        response = client.send(new HttpObject().path(serverUrl + "/status/200/content-range/aa"));
        assertThat(response.failure()).isNull();
        assertThat(response.header(CONTENT_LENGTH)).isEqualTo("0");
        assertThat(response.header(CONTENT_RANGE)).isEqualTo("aa");
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_OCTET_STREAM.value());
        assertThat(response.size()).isZero();

        // verify body request
        response = client.send(new HttpObject().path(serverUrl).body("{Hällo Wörld?!}"));
        assertThat(response.failure()).isNull();
        assertThat(response.bodyAsString()).isEqualTo("{Hällo Wörld?!}");
        assertThat(response.header(CONTENT_LENGTH)).isEqualTo("17");
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.value());
        assertThat(response.header(CONTENT_RANGE)).isNull();

        // send HttpRequest request
        response = client.send(HttpRequest.newBuilder().uri(URI.create(serverUrl)).method(GET.name(), HttpRequest.BodyPublishers.ofString("{Hällo Wörld?!}")).build());
        assertThat(response.failure()).isNull();
        assertThat(response.bodyAsString()).isEqualTo("{Hällo Wörld?!}");
        assertThat(response.header(CONTENT_LENGTH)).isEqualTo("17");
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.value());
        assertThat(response.header(CONTENT_RANGE)).isNull();

        // verify user agent
        assertThat(new HttpObject().headers().firstValue(USER_AGENT).orElse(null)).doesNotStartWith("Java-http-client/");

        // send async request
        final CountDownLatch latch = new CountDownLatch(1);
        response = client.send(new HttpObject().path(serverUrl).body("{Hällo Wörld?!}"), callback -> latch.countDown());
        assertThat(latch.await(2000, TimeUnit.MILLISECONDS)).isTrue();
        assertThat(response.failure()).isNull();
        assertThat(response.bodyAsString()).isEqualTo("{Hällo Wörld?!}");
        assertThat(response.header(CONTENT_LENGTH)).isEqualTo("17");
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON.value());
        assertThat(response.header(CONTENT_RANGE)).isNull();

        // verify null url
        response = client.send(new HttpObject());
        assertThat(response.failure()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_PROBLEM_JSON.value());
        assertThat((LinkedTypeMap) response.bodyAsJson()).contains(
            entry("instance", ""),
            entry("status", -1L),
            entry("title", "URI with undefined scheme"),
            entry("type", "https://github.com/YunaBraska/nano")
        ).containsKey("id").containsKey("detail").containsKey("timestamp");

        // verify null request
        response = client.send(null);
        assertThat(response.failure()).isExactlyInstanceOf(IllegalArgumentException.class);
        assertThat(response.header(CONTENT_TYPE)).isEqualTo(APPLICATION_PROBLEM_JSON.value());
        assertThat((LinkedTypeMap) response.bodyAsJson()).contains(
            entry("instance", null),
            entry("status", 400L),
            entry("title", "Invalid request [null]"),
            entry("type", "https://github.com/YunaBraska/nano")
        ).containsKey("id").containsKey("detail").containsKey("timestamp");
    }

    public static void mimicRequest(final Event event) {
        event.payloadOpt(HttpObject.class).ifPresent(request -> {
            // Answer only to incoming requests
            if (request instanceof final HttpObject httpObject && httpObject.exchange() == null)
                return;

            final HttpObject response = request.response();
            final AtomicInteger status = new AtomicInteger(200);
            final String[] paths = Arrays.stream(request.path().split("/", -1)).filter(NanoUtils::hasText).toArray(String[]::new);
            for (int i = 1; i < paths.length; i += 2) {
                if ("status".equalsIgnoreCase(paths[i - 1])) {
                    status.set(convertObj(paths[i], Integer.class));
                } else {
                    response.header(paths[i - 1], paths[i].replace("_", "/").replace(".", " "));
                }
            }
            response.body(request.body()).statusCode(status.get()).send(event);
        });
    }
}
