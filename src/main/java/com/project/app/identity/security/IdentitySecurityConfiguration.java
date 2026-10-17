package com.project.app.identity.security;

import java.io.IOException;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.csrf.CsrfFilter;
import com.project.app.common.web.RateLimitFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class IdentitySecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }

    @Bean
    SessionRegistry sessionRegistry() { return new SessionRegistryImpl(); }

    @Bean
    Clock identityClock() { return Clock.systemUTC(); }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() { return new HttpSessionEventPublisher(); }

    @Bean
    SecurityFilterChain identitySecurity(HttpSecurity http, SessionRegistry sessionRegistry,
            @Value("${procureflow.rate-limit.enabled:true}") boolean rateLimitEnabled) throws Exception {
        CookieCsrfTokenRepository csrfTokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokens.setCookieCustomizer(cookie -> cookie.path("/").secure(true).sameSite("Lax"));

        http
            .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
            .authorizeHttpRequests(authorize -> authorize
                    .requestMatchers("/", "/index.html", "/assets/**", "/app.js", "/styles.css", "/favicon.ico").permitAll()
                    .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login",
                            "/api/auth/verify", "/api/auth/invitations/accept").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                    .requestMatchers(HttpMethod.HEAD, "/api/public/**").permitAll()
                    .requestMatchers("/api/employees/**").hasAuthority("ROLE_ORG_ADMIN")
                    .requestMatchers("/api/marketplace/**").hasAuthority("ROLE_SUPPLIER")
                    .requestMatchers("/api/procurement/invitations/**").hasAuthority("ROLE_SUPPLIER")
                    .requestMatchers("/api/procurement/orders/**").hasAnyAuthority("ROLE_BUYER", "ROLE_SUPPLIER")
                    .requestMatchers("/api/procurement/workspace/**").hasAnyAuthority("ROLE_BUYER", "ROLE_SUPPLIER")
                    .requestMatchers(HttpMethod.GET, "/api/procurement/analytics/**").hasAnyAuthority("ROLE_BUYER", "ROLE_SUPPLIER")
                    .requestMatchers("/api/procurement/**").hasAuthority("ROLE_BUYER")
                    .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, failure) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                    .accessDeniedHandler((request, response, failure) -> response.sendError(HttpServletResponse.SC_FORBIDDEN)))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .maximumSessions(-1)
                    .sessionRegistry(sessionRegistry)
                    .expiredSessionStrategy(event -> event.getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED)));

        if (rateLimitEnabled) http.addFilterBefore(new RateLimitFilter(), CsrfFilter.class);
        http.addFilterAfter(new SessionLifetimeFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
