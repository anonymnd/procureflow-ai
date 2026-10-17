package com.project.app.common.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/** Per-process request limits. Client addresses come from the container, never raw proxy headers. */
public final class RateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> ONBOARDING = Set.of(
        "/api/auth/register", "/api/auth/verify", "/api/auth/invitations/accept");
    private final Window api;
    private final Window login;
    private final Window onboarding;

    public RateLimitFilter() { this(System::nanoTime); }
    RateLimitFilter(LongSupplier clock) {
        api = new Window(180, 60, 10_000, clock);
        login = new Window(10, 60, 10_000, clock);
        onboarding = new Window(5, 900, 10_000, clock);
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getServletPath();
        if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/api/")) { chain.doFilter(request, response); return; }
        String client = request.getRemoteAddr();
        if (client == null || client.isBlank()) client = "unknown";
        long retry = api.acquire(client);
        if (retry == 0 && "POST".equals(request.getMethod())) {
            if ("/api/auth/login".equals(path)) retry = login.acquire(client);
            else if (ONBOARDING.contains(path)) retry = onboarding.acquire(client);
        }
        if (retry == 0) { chain.doFilter(request, response); return; }
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(retry));
        response.setHeader("Cache-Control", "no-store");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
            + "\"message\":\"Too many requests. Please wait before trying again.\"}");
    }

    /** Bounded fixed-window counters; saturation refuses new keys instead of evicting active limits. */
    static final class Window {
        private final int limit;
        private final int capacity;
        private final long duration;
        private final LongSupplier clock;
        private final Map<String, Integer> counts = new HashMap<>();
        private long started;
        Window(int limit, int seconds, int capacity, LongSupplier clock) {
            this.limit = limit; this.duration = TimeUnit.SECONDS.toNanos(seconds);
            this.capacity = capacity; this.clock = clock; this.started = clock.getAsLong();
        }
        synchronized long acquire(String key) {
            long now = clock.getAsLong();
            if (now - started >= duration) { counts.clear(); started = now; }
            int count = counts.getOrDefault(key, 0);
            if (count >= limit || (count == 0 && counts.size() >= capacity)) {
                return Math.max(1, TimeUnit.NANOSECONDS.toSeconds(duration - (now - started) - 1) + 1);
            }
            counts.put(key, count + 1);
            return 0;
        }
    }
}
