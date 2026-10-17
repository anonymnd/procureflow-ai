package com.project.app.identity.security;

import java.io.IOException;
import java.time.Duration;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class SessionLifetimeFilter extends OncePerRequestFilter {
    public static final String AUTHENTICATED_AT = SessionLifetimeFilter.class.getName() + ".authenticatedAt";
    private static final long ABSOLUTE_LIFETIME_MILLIS = Duration.ofHours(12).toMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof AccountPrincipal) {
            HttpSession session = request.getSession(false);
            Object started = session == null ? null : session.getAttribute(AUTHENTICATED_AT);
            if (!(started instanceof Long timestamp) || System.currentTimeMillis() - timestamp >= ABSOLUTE_LIFETIME_MILLIS) {
                SecurityContextHolder.clearContext();
                if (session != null) session.invalidate();
            }
        }
        chain.doFilter(request, response);
    }
}
