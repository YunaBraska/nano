package berlin.yuna.nano.services.http.model;

import berlin.yuna.typemap.logic.JsonDecoder;
import berlin.yuna.typemap.logic.XmlDecoder;
import berlin.yuna.typemap.model.TypeContainer;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

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

    public boolean isContentType(final String contentType) {
        return headers.getList(HttpHeaders.CONTENT_TYPE).contains(contentType);
    }

    public boolean isContentTypeJson() {
        return isContentType(ContentType.APPLICATION_JSON.value());
    }

    public boolean isContentTypeXml() {
        return isContentType(ContentType.APPLICATION_XML.value());
    }

    public boolean isContentTypeXmlSoap() {
        return isContentType(ContentType.APPLICATION_SOAP_XML.value());
    }

    public boolean isContentTypeOctetStream() {
        return isContentType(ContentType.APPLICATION_OCTET_STREAM.value());
    }

    public boolean isContentTypePdf() {
        return isContentType(ContentType.APPLICATION_PDF.value());
    }

    public boolean isContentTypeFormUrlEncoded() {
        return isContentType(ContentType.APPLICATION_FORM_URLENCODED.value());
    }

    public boolean isContentTypeMultiPartFormData() {
        return isContentType(ContentType.MULTIPART_FORM_DATA.value());
    }

    public boolean isContentTypePlainText() {
        return isContentType(ContentType.TEXT_PLAIN.value());
    }

    public boolean isContentTypeHtml() {
        return isContentType(ContentType.TEXT_HTML.value());
    }

    public boolean isContentTypeJpeg() {
        return isContentType(ContentType.IMAGE_JPEG.value());
    }

    public boolean isContentTypePng() {
        return isContentType(ContentType.IMAGE_PNG.value());
    }

    public boolean isContentTypeGif() {
        return isContentType(ContentType.IMAGE_GIF.value());
    }

    public boolean isContentTypeMpeg() {
        return isContentType(ContentType.AUDIO_MPEG.value());
    }

    public boolean isContentTypeMp4() {
        return isContentType(ContentType.VIDEO_MP4.value());
    }

    public List<ContentType> contentTypes() {
        return headers.getList(ContentType.class, HttpHeaders.CONTENT_TYPE);
    }

    public ContentType contentType() {
        return headers.get(ContentType.class, HttpHeaders.CONTENT_TYPE);
    }

    public List<ContentType> accept() {
        return headers.getList(ContentType.class, HttpHeaders.ACCEPT);
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

    public String header(final String name) {
        String value = headers.get(String.class, name);
        if (value != null && value.startsWith("[")) {
           return value.substring(1, value.length() - 1);
        }
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

    public String[] authBasic() {
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

    public List<Locale> locale() {
        final String acceptLanguageHeader = header(HttpHeaders.ACCEPT_LANGUAGE);
        if (acceptLanguageHeader == null || acceptLanguageHeader.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(acceptLanguageHeader.split(","))
            .map(s -> s.split(";q="))
            .sorted(Comparator.comparing(parts -> parts.length > 1 ? Double.parseDouble(parts[1]) : 1.0, Comparator.reverseOrder()))
            .map(parts -> Locale.forLanguageTag(parts[0]))
            .toList();
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
}
