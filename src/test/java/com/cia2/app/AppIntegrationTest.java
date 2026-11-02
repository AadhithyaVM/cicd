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

/**
 * Integration tests: start the REAL HTTP server on a random free port and call it
 * over HTTP, like a browser or Kubernetes would. They check routing, status codes,
 * content types and response bodies.
 */
public class AppIntegrationTest {

    private static HttpServer server;
    private static String base;

    @BeforeClass
    public static void start() throws Exception {
        server = App.create(0);              // port 0 = the OS picks a free port (no clashes)
        server.start();
        base = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterClass
    public static void stop() {
        server.stop(0);
    }

    // ---------------------------------------------------------------- helpers

    private static HttpURLConnection open(String path) throws Exception {
        return (HttpURLConnection) new URL(base + path).openConnection();
    }

    private static int status(String path) throws Exception {
        return open(path).getResponseCode();
    }

    private static String contentType(String path) throws Exception {
        return open(path).getHeaderField("Content-Type");
    }

    private static String body(String path) throws Exception {
        try (InputStream in = open(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    // ---------------------------------------------------------------- /health

    @Test
    public void healthEndpointReturns200() throws Exception {
        assertEquals(200, status("/health"));
    }

    @Test
    public void healthBodyReportsUpWithAVersion() throws Exception {
        // The version is whatever APP_VERSION is set to ("local" when not set).
        assertTrue(body("/health").matches("\\{\"status\":\"UP\",\"version\":\"[^\"]+\"\\}"));
    }

    @Test
    public void healthIsServedAsJson() throws Exception {
        assertTrue(contentType("/health").startsWith("application/json"));
    }

    // ------------------------------------------------------------------- "/"

    @Test
    public void homePageReturns200() throws Exception {
        assertEquals(200, status("/"));
    }

    @Test
    public void homePageIsServedAsHtml() throws Exception {
        assertTrue(contentType("/").startsWith("text/html"));
    }

    @Test
    public void homePageContainsHeadlineAndTaskForm() throws Exception {
        String page = body("/");
        assertTrue(page.contains(GreetingService.HEADLINE));
        assertTrue(page.contains("Add task"));
    }

    @Test
    public void homePageShowsTheDefaultBuildNumber() throws Exception {
        // Without APP_VERSION the page must still render a build label.
        assertTrue(body("/").contains("Build <b>#"));
    }

    // ------------------------------------------------------------- /api/greet

    @Test
    public void greetUsesNameQueryParameter() throws Exception {
        assertEquals("Hello, DevOps!", body("/api/greet?name=DevOps"));
    }

    @Test
    public void greetDecodesUrlEncodedNames() throws Exception {
        assertEquals("Hello, Ada Lovelace!", body("/api/greet?name=Ada%20Lovelace"));
    }

    @Test
    public void greetWithoutNameSaysHelloWorld() throws Exception {
        assertEquals("Hello, World!", body("/api/greet"));
    }

    // ------------------------------------------------------------ not found

    @Test
    public void unknownPathReturns404() throws Exception {
        assertEquals(404, status("/nope"));
    }

    @Test
    public void unknownApiPathReturns404() throws Exception {
        assertEquals(404, status("/api/unknown"));
    }
}