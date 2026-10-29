package com.cia2.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/** Starts the real HTTP server on a random port and calls it. */
public class AppIntegrationTest {

    private static HttpServer server;
    private static String base;

    @BeforeClass
    public static void start() throws Exception {
        server = App.create(0);
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterClass
    public static void stop() {
        server.stop(0);
    }

    private static int status(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        return c.getResponseCode();
    }

    private static String body(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(base + path).openConnection();
        try (InputStream in = c.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void healthEndpointReturnsUp() throws Exception {
        assertEquals(200, status("/health"));
        assertTrue(body("/health").contains("\"status\":\"UP\""));
    }

    @Test
    public void homePageServesHeadline() throws Exception {
        assertTrue(body("/").contains(GreetingService.HEADLINE));
    }

    @Test
    public void greetEndpointUsesQueryParam() throws Exception {
        assertEquals("Hello, DevOps!", body("/api/greet?name=DevOps"));
    }

    @Test
    public void unknownPathReturns404() throws Exception {
        assertEquals(404, status("/nope"));
    }
}
