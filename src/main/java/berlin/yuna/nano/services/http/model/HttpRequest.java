package berlin.yuna.nano.services.http.model;

import berlin.yuna.typemap.logic.JsonDecoder;
import berlin.yuna.typemap.logic.XmlDecoder;
import berlin.yuna.typemap.model.TypeContainer;
import berlin.yuna.typemap.model.TypeList;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

public class HttpRequest {

    protected final HttpMethod method;
    protected final String path;
    protected final TypeMap headers;
    protected final HttpExchange exchange;
    protected byte[] body;
    protected TypeMap queryParams;
    protected TypeMap pathParams;

    public HttpRequest(final HttpExchange exchange) {
        this.method = HttpMethod.httpMethodOf(exchange.getRequestMethod());
        this.path = exchange.getRequestURI().getPath();
        this.headers = convertHeaders(exchange.getRequestHeaders());
        this.exchange = exchange;
    }

    public static TypeMap convertHeaders(final Headers headers) {
        // handle all value as list if it have comma separated values
        return headers.entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                TypeMap::new
            ));
    }

    public static boolean isMethod(final HttpRequest request, final HttpMethod method) {
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
        try {
            if (body == null) {
                body = exchange.getRequestBody().readAllBytes();
            }
            return new String(body, Charset.defaultCharset());
        } catch (final Exception ignored) {
            return null;
        }
    }

    public TypeContainer<?> bodyAsJson() {
        return JsonDecoder.jsonTypeOf(bodyAsString());
    }

    public TypeContainer<?> bodyAsXml() {return XmlDecoder.xmlTypeOf(bodyAsString()); }

    public byte[] body() {
        return body;
    }

    public TypeMap queryParameters() {
        if (queryParams == null) {
            try {
                final String query = exchange.getRequestURI().getQuery();
                queryParams = new TypeMap();
                if (query != null) {
                    final String[] queryParamsArray = query.split("&");
                    for (final String param : queryParamsArray) {
                        final String[] keyValue = param.split("=");

                        if (keyValue.length == 2) {
                            final String key = java.net.URLDecoder.decode(keyValue[0], Charset.defaultCharset());
                            final String value = java.net.URLDecoder.decode(keyValue[1], Charset.defaultCharset());
                            queryParams.put(key, value);
                        } else if (keyValue.length == 1) {
                            final String key = java.net.URLDecoder.decode(keyValue[0], Charset.defaultCharset());
                            queryParams.put(key, "");
                        }
                    }
                }
            } catch (final Exception ignored) {
            }
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

        final String[] partsToMatch = pathToMatch.split("/");
        final String[] parts = path.split("/");

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
        String value = headers.get(String.class, name);
        if (value != null && value.startsWith("[")) {
           return value.substring(1, value.length() - 1);
        }else if(value != null)
            return value;
        else
            return null;
    }

    public TypeMap getHeaders() {
        return headers;
    }

    public boolean containsHeader(final String name) {
        return headers.containsKey(name.toLowerCase());
    }

    public String host() {
        return exchange.getLocalAddress().getHostName();
    }

    public int port() {
        return exchange.getLocalAddress().getPort();
    }

    public String protocol() {
        return exchange.getProtocol();
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

    private String decodeBasicAuth(String encodedCredentials) {
        byte[] decodedBytes = Base64.getDecoder().decode(encodedCredentials);
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

    public HttpResponse generateHttpResponse() {
        return new HttpResponse();
    }

    protected List<ContentType> contentSplitType(final String header) {
        return splitHeaderValue(headers.getList(String.class, header), ContentType::fromValue);
    }

    protected static <R> List<R> splitHeaderValue(final Collection<String> value, final Function<String, R> mapper) {
        if (value == null)
            return emptyList();
        if (value.size() != 1)
            return value.stream().map(mapper).toList();
        return Arrays.stream(value.iterator().next().split(","))
            .map(s -> s.split(";q="))
            .sorted(Comparator.comparing(parts -> parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0, Comparator.reverseOrder()))
            .map(parts -> mapper.apply(parts[0]))
            .filter(Objects::nonNull)
            .toList();
    }
}
