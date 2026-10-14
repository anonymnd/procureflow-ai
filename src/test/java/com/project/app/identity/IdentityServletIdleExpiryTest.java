package com.project.app.identity;

import static org.junit.jupiter.api.Assertions.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

import org.apache.catalina.Context;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.project.app.support.Slice0DatabaseGuard;

/** TC-S1-010: actual Tomcat expiry, accelerated on the test session only. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "server.servlet.session.cookie.secure=false",
                "logging.level.org.springframework.web=INFO", "logging.level.org.hibernate.SQL=INFO"})
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class IdentityServletIdleExpiryTest {
    @Autowired ServletWebServerApplicationContext application;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean JavaMailSender mail;

    @Test
    void authenticatedSessionUsesThirtyMinutesAndContainerExpiresAfterElapsedIdleTime() throws Exception {
        String base = "http://127.0.0.1:" + application.getWebServer().getPort();
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(10)).build();
        var csrfResponse = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        var match = Pattern.compile("\"token\":\"([^\"]+)\"").matcher(csrfResponse.body());
        assertEquals(200, csrfResponse.statusCode());
        allowLoopbackCookies(cookies);
        assertTrue(match.find());
        String csrf = match.group(1);
        String email = "idle-" + System.nanoTime() + "@example.test";
        String registration = "{\"organizationName\":\"Idle test\",\"fullName\":\"Idle user\",\"email\":\"" + email + "\",\"password\":\"idle test passphrase\"}";
        assertEquals(201, post(client, base + "/api/auth/register", csrf, registration).statusCode());
        // This fixture isolates session expiry; real verification is covered by browser/API journeys.
        assertEquals(1, jdbc.update("UPDATE accounts SET email_verified=true WHERE email=?", email));
        assertEquals(200, post(client, base + "/api/auth/login", csrf,
                "{\"email\":\"" + email + "\",\"password\":\"idle test passphrase\"}").statusCode());
        allowLoopbackCookies(cookies);
        String sessionId = cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("JSESSIONID")).findFirst().orElseThrow().getValue();
        TomcatWebServer server = (TomcatWebServer) application.getWebServer();
        Context context = (Context) server.getTomcat().getHost().findChildren()[0];
        var session = context.getManager().findSession(sessionId);
        assertNotNull(session);
        assertEquals(1800, session.getMaxInactiveInterval(), "Actual authenticated session must use approved idle limit");
        assertEquals(200, get(client, base + "/api/auth/me").statusCode());
        // Shorten only this live container session, leaving production login/configuration untouched.
        session.setMaxInactiveInterval(2);
        Thread.sleep(3200);
        assertEquals(401, get(client, base + "/api/auth/me").statusCode());
        assertFalse(session.isValid(), "Tomcat must invalidate the idle session");
    }

    private HttpResponse<String> get(HttpClient client, String url) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private void allowLoopbackCookies(CookieManager cookies) {
        // Chromium permits Secure cookies on loopback; Java's HTTP cookie handler does not.
        // Adapt only the test client's in-memory cookies, never the application's cookie policy.
        cookies.getCookieStore().getCookies().forEach(cookie -> { cookie.setVersion(0); cookie.setSecure(false); });
    }
    private HttpResponse<String> post(HttpClient client, String url, String csrf, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json").header("X-XSRF-TOKEN", csrf)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
}
