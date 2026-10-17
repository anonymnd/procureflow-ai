package com.project.app.common.web;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTest {
    private MockHttpServletResponse call(RateLimitFilter filter, String method, String path, String ip,
            String spoofedIp) throws Exception {
        var request = new MockHttpServletRequest(method, path);
        request.setServletPath(path); request.setRemoteAddr(ip);
        if (spoofedIp != null) request.addHeader("X-Forwarded-For", spoofedIp);
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse)res).setStatus(204));
        return response;
    }
    @Test void loginLimitCannotBeResetBySpoofedHeadersAndOtherClientsRemainIndependent() throws Exception {
        var filter = new RateLimitFilter(() -> 0);
        for (int i=0;i<10;i++) assertEquals(204,call(filter,"POST","/api/auth/login","192.0.2.1","203.0.113."+i).getStatus());
        var denied = call(filter,"POST","/api/auth/login","192.0.2.1","203.0.113.200");
        assertEquals(429,denied.getStatus()); assertEquals("60",denied.getHeader("Retry-After"));
        assertEquals("no-store",denied.getHeader("Cache-Control"));
        assertTrue(denied.getContentAsString().contains("\"status\":429"));
        assertEquals(204,call(filter,"POST","/api/auth/login","192.0.2.2",null).getStatus());
    }
    @Test void generalApiAndCombinedOnboardingHaveIndependentLimits() throws Exception {
        var filter = new RateLimitFilter(() -> 0);
        for(int i=0;i<180;i++) assertEquals(204,call(filter,"GET","/api/public/offerings","192.0.2.1",null).getStatus());
        assertEquals(429,call(filter,"GET","/api/public/offerings","192.0.2.1",null).getStatus());
        for(int i=0;i<5;i++) assertEquals(204,call(filter,"POST","/api/auth/register","192.0.2.2",null).getStatus());
        var denied=call(filter,"POST","/api/auth/invitations/accept","192.0.2.2",null);
        assertEquals(429,denied.getStatus()); assertEquals("900",denied.getHeader("Retry-After"));
        assertEquals(204,call(filter,"GET","/api/auth/me","192.0.2.2",null).getStatus());
        for(int i=0;i<200;i++) assertEquals(204,call(filter,"GET","/assets/app.js","192.0.2.1",null).getStatus());
    }
    @Test void fixedWindowResetsAndRetryAfterRoundsUp() {
        var time=new AtomicLong(); var window=new RateLimitFilter.Window(1,60,2,time::get);
        assertEquals(0,window.acquire("a"));time.set(TimeUnit.MILLISECONDS.toNanos(59_500));
        assertEquals(1,window.acquire("a"));time.set(TimeUnit.SECONDS.toNanos(60));
        assertEquals(0,window.acquire("a"));
    }
    @Test void concurrentRequestsCannotExceedTheLimit() {
        var window=new RateLimitFilter.Window(10,60,100,()->0);
        var admitted=new AtomicInteger();
        IntStream.range(0,500).parallel().forEach(i->{if(window.acquire("same-client")==0)admitted.incrementAndGet();});
        assertEquals(10,admitted.get());
    }
    @Test void capacityIsBoundedWithoutEvictingActiveLimits() {
        var time=new AtomicLong();var window=new RateLimitFilter.Window(1,60,2,time::get);
        assertEquals(0,window.acquire("a"));assertEquals(0,window.acquire("b"));
        assertEquals(60,window.acquire("c"));assertEquals(60,window.acquire("a"));
        time.set(TimeUnit.SECONDS.toNanos(60));assertEquals(0,window.acquire("c"));
    }
}
