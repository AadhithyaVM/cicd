package com.cia2.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests for GreetingService.
 * They call the methods directly (no network, no server), so they are fast and
 * each one checks exactly one behaviour.
 */
public class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    // ---------------------------------------------------------------- greet()

    @Test
    public void greetsByName() {
        assertEquals("Hello, Aadhi!", service.greet("Aadhi"));
    }

    @Test
    public void trimsWhitespaceAroundName() {
        assertEquals("Hello, Aadhi!", service.greet("  Aadhi  "));
    }

    @Test
    public void keepsSpacesInsideName() {
        assertEquals("Hello, Ada Lovelace!", service.greet("Ada Lovelace"));
    }

    @Test
    public void defaultsToWorldWhenNameIsNull() {
        assertEquals("Hello, World!", service.greet(null));
    }

    @Test
    public void defaultsToWorldWhenNameIsBlank() {
        assertEquals("Hello, World!", service.greet("   "));
    }

    // ------------------------------------------------------- renderHomePage()

    @Test
    public void homePageShowsHeadlineBuildNumberAndPodName() {
        String html = service.renderHomePage("42", "pod-abc");
        assertTrue(html.contains(GreetingService.HEADLINE));
        assertTrue("footer shows the build number", html.contains("Build <b>#42</b>"));
        assertTrue("footer shows the pod name", html.contains("pod <b>pod-abc</b>"));
    }

    @Test
    public void homePageLeavesNoUnfilledPlaceholders() {
        String html = service.renderHomePage("42", "pod-abc");
        assertFalse("every @@...@@ placeholder must be replaced", html.contains("@@"));
    }

    @Test
    public void homePageHasTheAddTaskForm() {
        String html = service.renderHomePage("1", "pod");
        assertTrue(html.contains("id=\"form\""));
        assertTrue(html.contains("id=\"text\""));      // task text box
        assertTrue(html.contains("id=\"priority\""));  // Low / Medium / High
        assertTrue(html.contains("id=\"due\""));       // due date
        assertTrue(html.contains("Add task"));         // submit button
    }

    @Test
    public void homePageHasFilterTabsSearchAndStatistics() {
        String html = service.renderHomePage("1", "pod");
        assertTrue(html.contains("data-f=\"all\""));
        assertTrue(html.contains("data-f=\"active\""));
        assertTrue(html.contains("data-f=\"done\""));
        assertTrue(html.contains("id=\"search\""));
        assertTrue(html.contains("id=\"sTotal\""));
        assertTrue(html.contains("id=\"sActive\""));
        assertTrue(html.contains("id=\"sDone\""));
        assertTrue(html.contains("id=\"progress\""));
    }

    @Test
    public void homePageSavesTasksInTheBrowser() {
        String html = service.renderHomePage("1", "pod");
        assertTrue(html.contains("localStorage"));
        assertTrue(html.contains("taskflow.tasks.v1"));
    }

    @Test
    public void homePageLinksToTheHealthEndpoint() {
        assertTrue(service.renderHomePage("1", "pod").contains("href=\"/health\""));
    }

    @Test
    public void homePageEscapesInjectedValues() {
        // A hostile value must not become real HTML.
        String html = service.renderHomePage("1", "<script>alert(1)</script>");
        assertFalse(html.contains("<script>alert(1)"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    // ------------------------------------------------------------ healthJson()

    @Test
    public void healthJsonReportsUpAndVersion() {
        assertEquals("{\"status\":\"UP\",\"version\":\"7\"}", service.healthJson("7"));
    }

    @Test
    public void healthJsonUsesTheGivenVersion() {
        assertTrue(service.healthJson("local").contains("\"version\":\"local\""));
        assertTrue(service.healthJson("123").contains("\"version\":\"123\""));
    }
}