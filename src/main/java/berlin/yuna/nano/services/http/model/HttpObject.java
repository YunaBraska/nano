package berlin.yuna.nano.services.http.model;

import berlin.yuna.typemap.logic.JsonDecoder;
import berlin.yuna.typemap.logic.TypeConverter;
import berlin.yuna.typemap.logic.XmlDecoder;
import berlin.yuna.typemap.model.TypeContainer;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

public class HttpObject {

    protected HttpMethod method;
    protected String path;
    protected final HttpExchange exchange;
    protected byte[] body;
    protected TypeMap headers;
    protected TypeMap queryParams;
    protected TypeMap pathParams;
    private int statusCode;

    public HttpObject(final HttpExchange exchange) {
        this.exchange = exchange;
        path(exchange.getRequestURI().getPath());
        method(exchange.getRequestMethod());
        headers(exchange.getRequestHeaders());
    }

    public HttpObject() {
        this.exchange = null;
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

    public HttpMethod method() {
        return method;
    }

    public HttpObject method(final String method) {
        this.method = HttpMethod.httpMethodOf(method);
        return this;
    }

    public HttpObject method(final HttpMethod method) {
        this.method = method;
        return this;
    }

    public ContentType contentType() {
        final List<ContentType> result = contentTypes();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<ContentType> contentTypes() {
        return contentSplitType(HttpHeaders.CONTENT_TYPE);
    }

    public HttpObject contentType(final String... contentType) {
        headers().put(HttpHeaders.CONTENT_TYPE, Arrays.stream(contentType).map(ContentType::fromValue).filter(Objects::nonNull).map(ContentType::value).collect(Collectors.joining(", ")));
        return this;
    }

    public HttpObject contentType(final ContentType... contentType) {
        headers().put(HttpHeaders.CONTENT_TYPE, Arrays.stream(contentType).filter(Objects::nonNull).map(ContentType::value).collect(Collectors.joining(", ")));
        return this;
    }

    public boolean hasContentType(final String... contentTypes) {
        final List<ContentType> result = contentTypes();
        return Arrays.stream(contentTypes).map(ContentType::fromValue).allMatch(result::contains);
    }

    public boolean hasContentType(final ContentType... contentTypes) {
        final List<ContentType> result = contentTypes();
        return Arrays.stream(contentTypes).allMatch(result::contains);
    }

    public boolean hasContentType(final ContentType contentType) {
        return contentTypes().contains(contentType);
    }

    public ContentType accept() {
        final List<ContentType> result = accepts();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<ContentType> accepts() {
        return contentSplitType(HttpHeaders.ACCEPT);
    }

    public HttpObject accept(final String... contentType) {
        headers().put(HttpHeaders.ACCEPT, Arrays.stream(contentType).map(ContentType::fromValue).filter(Objects::nonNull).map(ContentType::value).collect(Collectors.joining(", ")));
        return this;
    }

    public HttpObject accept(final ContentType... contentType) {
        headers().put(HttpHeaders.ACCEPT, Arrays.stream(contentType).filter(Objects::nonNull).map(ContentType::value).collect(Collectors.joining(", ")));
        return this;
    }

    public boolean hasAccept(final String... contentTypes) {
        final List<ContentType> result = accepts();
        return Arrays.stream(contentTypes).map(ContentType::fromValue).allMatch(result::contains);
    }

    public boolean hasAccept(final ContentType... contentTypes) {
        final List<ContentType> result = accepts();
        return Arrays.stream(contentTypes).allMatch(result::contains);
    }

    public boolean hasAccept(final ContentType contentType) {
        return accepts().contains(contentType);
    }

    public String acceptEncoding() {
        final List<String> result = acceptEncodings();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<String> acceptEncodings() {
        return splitHeaderValue(headers().getList(String.class, HttpHeaders.ACCEPT_ENCODING), v -> v);
    }

    public boolean hasAcceptEncoding(final String... contentTypes) {
        final List<String> result = splitHeaderValue(headers().getList(String.class, HttpHeaders.ACCEPT_ENCODING), v -> v);
        return Arrays.stream(contentTypes).allMatch(result::contains);
    }

    public Locale acceptLanguage() {
        final List<Locale> result = acceptLanguages();
        return result.isEmpty() ? null : result.getFirst();
    }

    public List<Locale> acceptLanguages() {
        return splitHeaderValue(headers().getList(String.class, HttpHeaders.ACCEPT_LANGUAGE), Locale::forLanguageTag);
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

    public HttpObject path(final String path) {
        final String[] parts = path == null ? new String[0] : split(path, "?");
        this.path = parts.length > 0 ? removeLast(parts[0], "/") : null;
        if (parts.length > 1) {
            queryParams = queryParamsOf(parts[1]);
        }
        return this;
    }

    public String bodyAsString() {
        return new String(body(), Charset.defaultCharset());
    }

    public TypeContainer<?> bodyAsJson() {
        return JsonDecoder.jsonTypeOf(bodyAsString());
    }

    public TypeContainer<?> bodyAsXml() {return XmlDecoder.xmlTypeOf(bodyAsString());}

    public byte[] body() {
        if (body == null && exchange != null) {
            try {
                body = exchange.getRequestBody().readAllBytes();
            } catch (final Exception ignored) {
                // ignored
            }
        }
        if (body == null)
            body = new byte[0];
        return body;
    }

    public HttpObject body(final TypeContainer<?> body) {
        this.body = body.toJson().getBytes(Charset.defaultCharset());
        return this;
    }

    public HttpObject body(final String body) {
        this.body = body.getBytes(Charset.defaultCharset());
        return this;
    }

    public HttpObject body(final byte[] body) {
        this.body = body;
        return this;
    }

    public TypeMap queryParams() {
        if (queryParams == null && exchange != null) {
            queryParams = ofNullable(fromExchange(httpExchange -> queryParamsOf(httpExchange.getRequestURI().getQuery()))).orElseGet(TypeMap::new);
        }
        if (queryParams == null) {
            queryParams = new TypeMap();
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

    public boolean pathMatch(final String expression) {
        if (this.path == null || expression == null)
            return false;

        final String pathToMatch = removeLast(expression, "/");
        final String[] partsToMatch = split(pathToMatch, "/");
        final String[] parts = split(this.path, "/");

        if (this.path.equals(pathToMatch) && pathToMatch.contains("{")) return true;

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
        return pathParams.get(String.class, key);
    }

    public String header(final String name) {
        return name == null ? null : headers.get(String.class, name.toLowerCase());
    }

    public boolean containsHeader(final String name) {
        return name != null && headers.containsKey(name.toLowerCase());
    }

    public String host() {
        return ofNullable(fromExchange(httpExchange -> httpExchange.getRemoteAddress().getHostName()))
            .or(() -> headers.getOpt(String.class, HttpHeaders.HOST).map(s -> split(s, ":")[0])).orElse(null);
    }

    public InetAddress address() {
        return fromExchange(httpExchange -> httpExchange.getRemoteAddress().getAddress());
    }

    public int port() {
        return ofNullable(fromExchange(httpExchange -> httpExchange.getRemoteAddress().getPort()))
            .or(() -> headers.getOpt(String.class, HttpHeaders.HOST).map(s -> split(s, ":"))
                .filter(a -> a.length > 1)
                .map(a -> a[1])
                .map(s -> TypeConverter.convertObj(s, Integer.class))
            ).orElse(-1);
    }

    public String protocol() {
        return fromExchange(HttpExchange::getProtocol);
    }

    public String userAgent() {
        return header(HttpHeaders.USER_AGENT);
    }

    public String[] authToken() {
        return ofNullable(header(HttpHeaders.AUTHORIZATION))
            .map(value -> {
                if (value.startsWith("Bearer ")) {
                    return new String[]{value.substring("Bearer ".length())};
                }
                if (value.startsWith("Basic ")) {
                    final String decode = new String(Base64.getDecoder().decode(value.substring("Basic ".length())));
                    return decode.contains(":") ? split(decode, ":") : new String[]{decode};
                }
                return new String[]{value};
            })
            .orElse(new String[0]);
    }

    public HttpExchange exchange() {
        return exchange;
    }

    protected List<ContentType> contentSplitType(final String key) {
        return splitHeaderValue(headers().getList(String.class, key), ContentType::fromValue);
    }

    protected static <R> List<R> splitHeaderValue(final Collection<String> value, final Function<String, R> mapper) {
        if (value == null)
            return emptyList();
        if (value.size() != 1)
            return value.stream().map(mapper).toList();
        return Arrays.stream(split(value.iterator().next(), ","))
            .map(s -> split(s, ";q="))
            .sorted(Comparator.comparing(parts -> parts.length > 1 ? Double.parseDouble(parts[1].trim()) : 1.0, Comparator.reverseOrder()))
            .map(parts -> mapper.apply(parts[0].trim()))
            .filter(Objects::nonNull)
            .toList();
    }

    public HttpObject statusCode(final int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public int statusCode() {
        return statusCode;
    }

    public TypeMap headers() {
        if (headers == null)
            headers = new TypeMap();
        return headers;
    }

    public HttpObject headers(final Headers headers) {
        this.headers = convertHeaders(headers);
        return this;
    }

    public HttpObject headers(final Map<String, Object> headers) {
        this.headers = convertHeaders(headers);
        return this;
    }

    public HttpObject header(final String key, final Object value) {
        if (key != null && value != null)
            headers().put(key.toLowerCase(), value);
        return this;
    }

    public <T> T fromExchange(final Function<HttpExchange, T> mapper) {
        return exchange != null ? mapper.apply(exchange) : null;
    }

    protected TypeMap queryParamsOf(final String query) {
        if (queryParams == null) {
            queryParams = ofNullable(query)
                .map(q -> {
                    final TypeMap result = new TypeMap();
                    Arrays.stream(split(q, "&"))
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof final HttpObject that)) return false;
        return statusCode == that.statusCode && method == that.method && Objects.equals(path, that.path) && Objects.deepEquals(body, that.body) && Objects.equals(headers, that.headers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, path, Arrays.hashCode(body), headers, statusCode);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", HttpObject.class.getSimpleName() + "[", "]")
            .add("statusCode=" + statusCode)
            .add("path=" + path)
            .add("method=" + method)
            .add("headers=" + headers)
            .add("body=" + bodyAsString())
            .toString();
    }

    // ########## STATICS ##########

    public static boolean isMethod(final HttpObject request, final HttpMethod method) {
        return request.method.name().equals(method.name());
    }

    public static TypeMap convertHeaders(final Map<String, ?> headers) {
        return headers.entrySet().stream()
            .filter(entry -> entry.getKey() != null && entry.getValue() != null)
            .collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                TypeMap::new
            ));
    }

    public static String removeLast(final String input, final String removable) {
        return input.length() > removable.length() && input.endsWith(removable) ? input.substring(0, input.length() - removable.length()) : input;
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
