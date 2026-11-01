package com.cia2.app;

/** Simple business logic, kept separate from HTTP code so it is easy to unit test. */
public class GreetingService {

    /** Headline shown on the home page. Change this text to demo the pipeline (Deliverable 4). */
    public static final String HEADLINE = "Hello from the CI/CD pipeline - version 1--aadhi";

    public String greet(String name) {
        if (name == null || name.isBlank()) {
            return "Hello, World!";
        }
        return "Hello, " + name.trim() + "!";
    }

    public String renderHomePage(String version, String podName) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>CIA 2 CI/CD Demo</title>"
                + "<style>body{font-family:Arial,sans-serif;background:#0f172a;color:#e2e8f0;"
                + "display:flex;justify-content:center;align-items:center;height:100vh;margin:0}"
                + ".card{background:#1e293b;padding:40px 60px;border-radius:12px;text-align:center}"
                + "h1{color:#38bdf8}</style></head><body><div class=\"card\">"
                + "<h1>" + HEADLINE + "</h1>"
                + "<p>Build number: <b>" + version + "</b></p>"
                + "<p>Served by pod: <b>" + podName + "</b></p>"
                + "<p>Git &rarr; Jenkins &rarr; Maven &rarr; Docker &rarr; ACR &rarr; Ansible &rarr; Kubernetes on Azure</p>"
                + "</div></body></html>";
    }

    public String healthJson(String version) {
        return "{\"status\":\"UP\",\"version\":\"" + version + "\"}";
    }
}
