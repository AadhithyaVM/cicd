package com.cia2.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Minimal Java web application using the JDK built-in HTTP server (no external runtime dependencies). */
public class App {

    private static final GreetingService SERVICE = new GreetingService();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = create(port);
        server.start();
        System.out.println("Server started on port " + server.getAddress().getPort());
    }

    public static HttpServer create(int port) throws IOException {
        String version = System.getenv().getOrDefault("APP_VERSION", "local");
        String pod = System.getenv().getOrDefault("HOSTNAME", "localhost");

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", ex -> {
            if (!"/".equals(ex.getRequestURI().getPath())) {
                send(ex, 404, "text/plain", "Not found");
                return;
            }
            send(ex, 200, "text/html; charset=UTF-8", SERVICE.renderHomePage(version, pod));
        });
        server.createContext("/health", ex ->
                send(ex, 200, "application/json", SERVICE.healthJson(version)));
        server.createContext("/api/greet", ex -> {
            String query = ex.getRequestURI().getRawQuery();
            String name = null;
            if (query != null && query.startsWith("name=")) {
                name = URLDecoder.decode(query.substring(5), StandardCharsets.UTF_8);
            }
            send(ex, 200, "text/plain; charset=UTF-8", SERVICE.greet(name));
        });
        return server;
    }

    private static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
