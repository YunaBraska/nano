package berlin.yuna.nano.services.http.model;

import berlin.yuna.typemap.logic.JsonDecoder;
import berlin.yuna.typemap.logic.TypeConverter;
import berlin.yuna.typemap.logic.XmlDecoder;
import berlin.yuna.typemap.model.TypeContainer;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class HttpObject {

    protected final HttpMethod method;
    protected final String path;
    protected final HttpExchange exchange;
    protected byte[] body;
    protected TypeMap headers;
    protected TypeMap queryParams;
    protected TypeMap pathParams;
    private int statusCode;

    public HttpObject(final HttpExchange exchange) {
        this.method = HttpMethod.httpMethodOf(exchange.getRequestMethod());
        this.path = exchange.getRequestURI().getPath();
        this.headers = convertHeaders(exchange.getRequestHeaders());
        this.exchange = exchange;
    }

    public HttpObject() {
        this.method = null;
        this.path = null;
        this.headers = new TypeMap();
        this.exchange = null;
    }

    public static TypeMap convertHeaders(final Headers headers) {
        return headers.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                TypeMap::new
            ));
    }

    public static TypeMap convertSimpleHeaders(final Map<String, String> headers) {
        return headers.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                entry -> {
                    final String value = entry.getValue();
                    return value.split("\\s*,\\s*");
                },
                (v1, v2) -> v1,
                TypeMap::new
            ));
    }

    public static boolean isMethod(final HttpObject request, final HttpMethod method) {
        return request.method.name().equals(method.name());
    }

    public boolean isMethodGet() {
        return HttpMethod.GET.equals(method);
    }

    public boolean isMethodPost() {
        return HttpMethod.POST.equals(method);
    }

    public boolean isMethodPut() {
        return HttpMethod.PUT.equals(method);
    }

    public boolean isMethodHead() {
        return HttpMethod.HEAD.equals(method);
    }

    public boolean isMethodPatch() {
        return HttpMethod.PATCH.equals(method);
    }

    public boolean isMethodDelete() {
        return HttpMethod.DELETE.equals(method);
    }

    public boolean isMethodOptions() {
        return HttpMethod.OPTIONS.equals(method);
    }

    public boolean isMethodTrace() {
        return HttpMethod.TRACE.equals(method);
    }

    public String method() {
        return method.name();
    }

//    public boolean isContentType(final String contentType) {
//        return headers.getList(HttpHeaders.CONTENT_TYPE).contains(contentType);
//    }

    public ContentType contentType() {
        final List<ContentType> result = contentTypes();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<ContentType> contentTypes() {
        return contentSplitType(HttpHeaders.CONTENT_TYPE);
    }

    public ContentType accept() {
        final List<ContentType> result = accepts();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<ContentType> accepts() {
        return contentSplitType(HttpHeaders.ACCEPT);
    }

    public boolean hasContentType(final String... contentTypes) {
        final List<String> result = splitHeaderValue(headers.getList(String.class, HttpHeaders.CONTENT_TYPE), v -> v);
        return Arrays.stream(contentTypes).allMatch(result::contains);
    }

    public boolean hasContentType(final ContentType contentType) {
        return contentSplitType(HttpHeaders.CONTENT_TYPE).contains(contentType);
    }

    public boolean hasAccept(final String... contentTypes) {
        final List<String> result = splitHeaderValue(headers.getList(String.class, HttpHeaders.ACCEPT), v -> v);
        return Arrays.stream(contentTypes).allMatch(result::contains);
    }

    public boolean hasAccept(final ContentType contentType) {
        return contentSplitType(HttpHeaders.ACCEPT).contains(contentType);
    }

    public String acceptEncoding() {
        final List<String> result = acceptEncodings();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<String> acceptEncodings() {
        return splitHeaderValue(headers.getList(String.class, HttpHeaders.ACCEPT_ENCODING), v -> v);
    }

    public boolean hasAcceptEncoding(final String... contentTypes) {
        final List<String> result = splitHeaderValue(headers.getList(String.class, HttpHeaders.ACCEPT_ENCODING), v -> v);
        return Arrays.stream(contentTypes).allMatch(result::contains);
    }

    public List<Locale> acceptLanguage() {
        final List<Locale> result = acceptLanguages();
        return result.isEmpty() ? null : result;
    }

    public List<Locale> acceptLanguages() {
        return splitHeaderValue(headers.getList(String.class, HttpHeaders.ACCEPT_LANGUAGE), Locale::forLanguageTag);
    }

    public boolean hasContentTypeJson() {
        return hasContentType(ContentType.APPLICATION_JSON);
    }

    public boolean hasContentTypeXml() {
        return hasContentType(ContentType.APPLICATION_XML);
    }

    public boolean hasContentTypeXmlSoap() {
        return hasContentType(ContentType.APPLICATION_SOAP_XML);
    }

    public boolean hasContentTypeOctetStream() {
        return hasContentType(ContentType.APPLICATION_OCTET_STREAM);
    }

    public boolean hasContentTypePdf() {
        return hasContentType(ContentType.APPLICATION_PDF);
    }

    public boolean hasContentTypeFormUrlEncoded() {
        return hasContentType(ContentType.APPLICATION_FORM_URLENCODED);
    }

    public boolean hasContentTypeMultiPartFormData() {
        return hasContentType(ContentType.MULTIPART_FORM_DATA);
    }

    public boolean hasContentTypePlainText() {
        return hasContentType(ContentType.TEXT_PLAIN);
    }

    public boolean hasContentTypeHtml() {
        return hasContentType(ContentType.TEXT_HTML);
    }

    public boolean hasContentTypeJpeg() {
        return hasContentType(ContentType.IMAGE_JPEG);
    }

    public boolean hasContentTypePng() {
        return hasContentType(ContentType.IMAGE_PNG);
    }

    public boolean hasContentTypeGif() {
        return hasContentType(ContentType.IMAGE_GIF);
    }

    public boolean hasContentTypeMpeg() {
        return hasContentType(ContentType.AUDIO_MPEG);
    }

    public boolean hasContentTypeMp4() {
        return hasContentType(ContentType.VIDEO_MP4);
    }

    public boolean hasAcceptJson() {
        return hasContentType(ContentType.APPLICATION_JSON);
    }

    public boolean hasAcceptXml() {
        return hasContentType(ContentType.APPLICATION_XML);
    }

    public boolean hasAcceptXmlSoap() {
        return hasContentType(ContentType.APPLICATION_SOAP_XML);
    }

    public boolean hasAcceptOctetStream() {
        return hasContentType(ContentType.APPLICATION_OCTET_STREAM);
    }

    public boolean hasAcceptPdf() {
        return hasContentType(ContentType.APPLICATION_PDF);
    }

    public boolean hasAcceptFormUrlEncoded() {
        return hasContentType(ContentType.APPLICATION_FORM_URLENCODED);
    }

    public boolean hasAcceptMultiPartFormData() {
        return hasContentType(ContentType.MULTIPART_FORM_DATA);
    }

    public boolean hasAcceptPlainText() {
        return hasContentType(ContentType.TEXT_PLAIN);
    }

    public boolean hasAcceptHtml() {
        return hasContentType(ContentType.TEXT_HTML);
    }

    public boolean hasAcceptJpeg() {
        return hasContentType(ContentType.IMAGE_JPEG);
    }

    public boolean hasAcceptPng() {
        return hasContentType(ContentType.IMAGE_PNG);
    }

    public boolean hasAcceptGif() {
        return hasContentType(ContentType.IMAGE_GIF);
    }

    public boolean hasAcceptMpeg() {
        return hasContentType(ContentType.AUDIO_MPEG);
    }

    public boolean hasAcceptMp4() {
        return hasContentType(ContentType.VIDEO_MP4);
    }


    public String path() {
        return path;
    }

    public String bodyAsString() {

        if (body == null) {
            body = fromExchange(httpExchange -> {
                try {
                    return httpExchange.getRequestBody().readAllBytes();
                } catch (final Exception ignored) {
                    return null;
                }
            });
        }
        return new String(body, Charset.defaultCharset());
    }

    public TypeContainer<?> bodyAsJson() {
        return JsonDecoder.jsonTypeOf(bodyAsString());
    }

    public TypeContainer<?> bodyAsXml() {return XmlDecoder.xmlTypeOf(bodyAsString());}

    public byte[] body() {
        return body;
    }

    public TypeMap queryParameters() {
        if (queryParams == null) {
            queryParams = Optional.ofNullable(fromExchange(httpExchange -> httpExchange.getRequestURI().getQuery()))
                .map(query -> {
                    final TypeMap result = new TypeMap();
                    Arrays.stream(split(query, "&"))
                        .map(param -> split(param, "="))
                        .forEach(keyValue -> {
                            final String key = URLDecoder.decode(keyValue[0], Charset.defaultCharset());
                            final String value = keyValue.length > 1 ? URLDecoder.decode(keyValue[1], Charset.defaultCharset()) : "";
                            result.put(key, value);
                        });
                    return result;
                })
                .orElseGet(TypeMap::new);
        }
        return queryParams;
    }

    public boolean containsQueryParam(final String key) {
        return queryParams.containsKey(key);
    }

    public String queryParam(final String key) {
        final Object value = queryParams.get(key);
        return value != null ? value.toString() : null;
    }

    public boolean exactMatch(final String path) {
        return this.path.startsWith(path);
    }

    public boolean match(final String pathToMatch) {

        final String[] partsToMatch = split(pathToMatch, "/");
        final String[] parts = split(path, "/");

        if (exactMatch(pathToMatch) && pathToMatch.contains("{")) return true;

        if (partsToMatch.length != parts.length) return false;

        if (pathParams == null)
            pathParams = new TypeMap();
        else
            pathParams.clear();
        for (int i = 0; i < partsToMatch.length; i++) {
            if (!partsToMatch[i].equals(parts[i])) {
                if (partsToMatch[i].startsWith("{")) {
                    final String key = partsToMatch[i].substring(1, partsToMatch[i].length() - 1);
                    pathParams.put(key, parts[i]);
                } else {
                    return false;
                }
            }
        }

        return true;
    }


    public TypeMap pathParams() {
        return pathParams;
    }

    public String pathParam(final String key) {
        return pathParams.get(key).toString();
    }

    // todo: do functional
    public String header(final String name) {
        return Optional.ofNullable(headers.get(String.class, name))
            .map(value -> value.startsWith("[") ? value.substring(1, value.length() - 1) : value)
            .orElse(null);
    }

    public TypeMap getHeaders() {
        return headers;
    }

    public boolean containsHeader(final String name) {
        return headers.containsKey(name.toLowerCase());
    }

    public String host() {
        return headers.getOpt(String.class, HttpHeaders.HOST).map(s -> split(s, ":")[0])
            .orElseGet(() -> fromExchange(httpExchange -> httpExchange.getLocalAddress().getHostName()));
    }

    public String address() {
        return headers.getOpt(String.class, HttpHeaders.HOST).map(s -> split(s, ":")[0])
            .orElseGet(() -> fromExchange(httpExchange -> httpExchange.getLocalAddress().getAddress().getHostAddress()));
    }

    public int port() {
        return headers.getOpt(String.class, HttpHeaders.HOST)
            .map(s -> split(s, ":"))
            .filter(a -> a.length > 1)
            .map(a -> a[1])
            .map(s -> TypeConverter.convertObj(s, Integer.class))
            .orElseGet(() -> fromExchange(httpExchange -> httpExchange.getLocalAddress().getPort()));
    }

    public String protocol() {
        return fromExchange(HttpExchange::getProtocol);
    }

    public String userAgent() {
        return header(HttpHeaders.USER_AGENT);
    }

    public String authToken() {
        return Optional.ofNullable(header(HttpHeaders.AUTHORIZATION))
            .filter(value -> value.startsWith("Bearer "))
            .map(value -> value.substring("Bearer ".length()))
            .orElse(null);
    }

    public String[] basicAuth() {
        return Optional.ofNullable(header(HttpHeaders.AUTHORIZATION))
            .filter(value -> value.startsWith("Basic "))
            .map(value -> value.substring("Basic ".length()))
            .map(this::decodeBasicAuth)
            .map(decodedCredentials -> decodedCredentials.split(":"))
            .orElse(null);
    }

    private String decodeBasicAuth(final String encodedCredentials) {
        final byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
        return new String(decodedBytes);
    }

    public HttpExchange exchange() {
        return exchange;
    }

//    private HttpResponse generateHttpResponse(final int statusCode, final byte[] body, final Map<String, String> headers) {
//        return new HttpResponse(statusCode, body, headers);
//    }
//    public HttpResponse generateHttpResponse(final int statusCode, final String body, final Map<String, String> headers) {
//        return generateHttpResponse(statusCode, body.getBytes(), headers);
//    }

    protected List<ContentType> contentSplitType(final String header) {
        return splitHeaderValue(headers.getList(String.class, header), ContentType::fromValue);
    }

    protected static <R> List<R> splitHeaderValue(final Collection<String> value, final Function<String, R> mapper) {
        if (value == null)
            return emptyList();
        if (value.size() != 1)
            return value.stream().map(mapper).toList();
        return Arrays.stream(split(value.iterator().next(), ","))
            .map(s -> split(s, ";q="))
            .sorted(Comparator.comparing(parts -> parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0, Comparator.reverseOrder()))
            .map(parts -> mapper.apply(parts[0]))
            .filter(Objects::nonNull)
            .toList();
    }

    public HttpObject statusCode(final int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public HttpObject body(final byte[] body) {
        this.body = body;
        return this;
    }

    public HttpObject headers(final Map<String, String> headers) {
        this.headers = convertSimpleHeaders(headers);
        return this;
    }

    public int statusCode() {
        return statusCode;
    }


    public Map<String, String> headers() {
        // TODO : convert TypeMap to Map<String, String>
        return headers.getMap(String.class, String.class);
    }

    public <T> T fromExchange(final Function<HttpExchange, T> mapper) {
        return exchange != null ? mapper.apply(exchange) : null;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        return statusCode == this.statusCode && Arrays.equals(body, this.body) && Objects.equals(headers, this.headers);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(statusCode, headers);
        result = 31 * result + Arrays.hashCode(body);
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HttpObject.class.getSimpleName() + "[", "]")
            .add("statusCode=" + statusCode)
            .add("body=" + Arrays.toString(body))
            .add("headers=" + headers)
            .toString();
    }

    public static String[] split(final String input, final String delimiter) {
        if (!input.contains(delimiter)) {
            return new String[]{input};
        }
        final List<String> result = new ArrayList<>();
        int start = 0;
        int index;
        while ((index = input.indexOf(delimiter, start)) != -1) {
            result.add(input.substring(start, index));
            start = index + delimiter.length();
        }
        result.add(input.substring(start));
        return result.toArray(new String[0]);
    }

}