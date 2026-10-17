package com.project.app.common.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;
import com.project.app.identity.security.IdentitySecurityConfiguration;

@WebMvcTest(controllers=RateLimitSecurityTest.Endpoints.class,
    properties="procureflow.rate-limit.enabled=true")
@Import({IdentitySecurityConfiguration.class, RateLimitSecurityTest.SecuritySupport.class,
    RateLimitSecurityTest.Endpoints.class})
class RateLimitSecurityTest {
    @Configuration(proxyBeanMethods=false) @EnableWebSecurity
    static class SecuritySupport { }
    @Autowired MockMvc mvc;
    @MockitoBean UserDetailsService users;
    @RestController public static class Endpoints {
        @PostMapping("/api/auth/login") public String login(){return "ok";}
        @GetMapping("/api/procurement/orders") public String orders(){return "private";}
        @GetMapping("/api/public/categories") public String categories(){return "public";}
    }
    @Test void securityChainEnforcesLoginLimitBeforeCallingController() throws Exception {
        for(int i=0;i<10;i++)mvc.perform(post("/api/auth/login").with(csrf())
            .with(req->{req.setRemoteAddr("192.0.2.10");return req;})).andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").with(csrf())
            .with(req->{req.setRemoteAddr("192.0.2.10");return req;}))
            .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
            .andExpect(jsonPath("$.status").value(429));
    }
    @Test void csrfAndPrivateRouteAuthorizationStillApply() throws Exception {
        mvc.perform(post("/api/auth/login")).andExpect(status().isForbidden());
        mvc.perform(get("/api/procurement/orders")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/public/categories")).andExpect(status().isOk());
    }
}
