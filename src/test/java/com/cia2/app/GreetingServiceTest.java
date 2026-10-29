package com.cia2.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    @Test
    public void greetsByName() {
        assertEquals("Hello, Aadhi!", service.greet("Aadhi"));
    }

    @Test
    public void trimsWhitespace() {
        assertEquals("Hello, Aadhi!", service.greet("  Aadhi  "));
    }

    @Test
    public void defaultsToWorldWhenNameMissing() {
        assertEquals("Hello, World!", service.greet(null));
        assertEquals("Hello, World!", service.greet("   "));
    }

    @Test
    public void homePageContainsHeadlineAndVersion() {
        String html = service.renderHomePage("42", "pod-abc");
        assertTrue(html.contains(GreetingService.HEADLINE));
        assertTrue(html.contains("42"));
        assertTrue(html.contains("pod-abc"));
    }

    @Test
    public void healthJsonReportsUp() {
        assertEquals("{\"status\":\"UP\",\"version\":\"7\"}", service.healthJson("7"));
    }
}
