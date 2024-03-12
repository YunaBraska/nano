package de.yuna.berlin.nativeapp.services.http.model;

import com.sun.net.httpserver.Headers;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class HttpRequest {
    private final HttpMethod method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;

    private HttpRequest(Builder builder) {
        this.method = builder.method;
        this.path = builder.path;
        this.headers = builder.headers;
        this.body = builder.body;
    }

    public HttpMethod getMethod() {return method;}

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }

    public static class Builder {
        private HttpMethod method;
        private String path;
        private Map<String, String> headers = new HashMap<>();
        private String body;

        public Builder method(HttpMethod method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Headers headers) {
            this.headers = convertHeadersToMap(headers);
            return this;
        }

        /**
         * convert HttpExchange headers into map
         *
         * @param headers Headers from HttpExchange
         * @return map of headers
         */
        public static Map<String, String> convertHeadersToMap(Headers headers) {

            return headers.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().get(0)
                ));
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        // Method to apply additional configurations using a lambda expression
        public Builder configure(Consumer<Builder> config) {
            config.accept(this);
            return this;
        }

        public HttpRequest build() {
            return new HttpRequest(this);
        }
    }

    // Remove
    public static void main(String[] args) {
        HttpRequest request = new HttpRequest.Builder().method(HttpMethod.POST)
            .path("/api/users")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.ACCEPT, "application/json")
            .body("{\"username\": \"john\"}").build();

        System.out.println("Method: " + request.getMethod());
        System.out.println("Path: " + request.getPath());
        System.out.println("Headers: " + request.getHeaders());
        System.out.println("Body: " + request.getBody());

        HttpRequest request2 = new HttpRequest.Builder()
            .method(HttpMethod.POST)
            .path("/api/users")
            .configure(builder -> {
                builder.header(HttpHeaders.CONTENT_TYPE, "application/json");
                builder.header(HttpHeaders.ACCEPT, "application/json");
                builder.body("{\"username\": \"john\"}");
            })
            .build();

        System.out.println("Method: " + request2.getMethod());
        System.out.println("Path: " + request2.getPath());
        System.out.println("Headers: " + request2.getHeaders());
        System.out.println("Body: " + request2.getBody());



    }
}
