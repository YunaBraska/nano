package de.yuna.berlin.nativeapp.services.http.model;

import berlin.yuna.typemap.logic.JsonDecoder;
import berlin.yuna.typemap.model.TypeContainer;
import berlin.yuna.typemap.model.TypeMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpRequest {

    protected final HttpMethod method;
    protected final String path;
    protected final TypeMap headers;
    protected final HttpExchange exchange;
    protected String body;
    protected TypeMap queryParams;
    protected TypeMap pathParams;

    public HttpRequest(HttpExchange exchange) {
        this.method = HttpMethod.httpMethodOf(exchange.getRequestMethod());
        this.path = exchange.getRequestURI().getPath();
        this.headers = convertHeaders(exchange.getRequestHeaders());
        this.exchange = exchange;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public static TypeMap convertHeaders(Headers headers) {
        return headers.entrySet().stream()
            .collect(Collectors.toMap(
                entry-> entry.getKey().toLowerCase(),
                Map.Entry::getValue,
                (v1, v2) -> v1,
                TypeMap::new
            ));
    }

    public static boolean isMethod(HttpRequest request, HttpMethod method) {
        return request.getMethod().name().equals(method.name());
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

    public boolean isContentType(String contentType) {
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


    public String bodyAsString() {
        try {
            if (body == null)
                body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            return body;
        } catch (Exception ignored) {
            return null;
        }
    }

    public TypeContainer bodyAsJson() {
        return JsonDecoder.jsonTypeOf(bodyAsString());
    }

    public TypeMap queryParameters() {
        if (queryParams == null) {
            try {
                String query = exchange.getRequestURI().getQuery();
                queryParams = new TypeMap();
                if (query != null) {
                    String[] queryParamsArray = query.split("&");
                    for (String param : queryParamsArray) {
                        String[] keyValue = param.split("=");

                        if (keyValue.length == 2) {
                            String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                            String value = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            queryParams.put(key, value);
                        } else if (keyValue.length == 1) {
                            String key = java.net.URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                            queryParams.put(key, "");
                        }
                    }
                }
            }catch (Exception ignored) {
            }
        }
        return queryParams;
    }

    public boolean containsQueryParam(String key) {
        return queryParams.containsKey(key);
    }

    public String getQueryParam(String key) {
        return queryParams.get(key).toString();
    }


    public boolean exactMatch(String path) {
        return this.path.startsWith(path);
    }

    public boolean match(String pathToMatch) {

        String[] partsToMatch = pathToMatch.split("/");
        String[] parts = path.split("/");

        if (exactMatch(pathToMatch) && pathToMatch.contains("{"))
            return true;

        if (partsToMatch.length != parts.length)
            return false;

        if (pathParams == null) {
            pathParams = new TypeMap();
            for (int i = 0; i < partsToMatch.length; i++) {
                if (!partsToMatch[i].equals(parts[i])) {
                    if (partsToMatch[i].startsWith("{")) {
                        pathParams.put(partsToMatch[i], parts[i]);
                    }else {
                        return false;
                    }
                }
            }
        }

        return true;
    }


    public TypeMap getPathParams() {
        return pathParams;
    }

    public String getHeader(String name) {
        return headers.get(name).toString();
    }

    public TypeMap getHeaders() {
        return headers;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public void removeHeader(String name) {
        headers.remove(name);
    }

    public boolean containsHeader(String name) {
        return headers.containsKey(name.toLowerCase());
    }

}
