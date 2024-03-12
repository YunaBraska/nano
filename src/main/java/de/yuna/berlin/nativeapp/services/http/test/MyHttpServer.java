package de.yuna.berlin.nativeapp.services.http.test;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import de.yuna.berlin.nativeapp.services.http.model.HttpMethod;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import de.yuna.berlin.nativeapp.services.http.model.HttpRequest;

// TODO : # remove this class
public class MyHttpServer {

    private static final int PORT = 8000;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/hello", new HelloHandler());
        server.createContext("/echo", new EchoHandler());

        server.start();
        System.out.println("Server started on port " + PORT);
    }

    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "Hello, World!";
            sendResponse(exchange, 200, response);
        }
    }

    static class EchoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {

                HttpRequest request = new HttpRequest.Builder()
                    .method(HttpMethod.POST)
                    .path(exchange.getRequestURI().getPath())
                    .headers(exchange.getRequestHeaders())
                    .body(getRequestData(exchange))
                    .build();

                String response = EchoService.echo(request.getBody());
                sendResponse(exchange, 200, response);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static String getRequestData(HttpExchange exchange) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }
        return requestBody.toString();
    }

    class EchoService {
        public static String echo(String data) {
            return "Echo from service: " + data;
        }
    }
}
