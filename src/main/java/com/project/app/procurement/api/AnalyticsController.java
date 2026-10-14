package com.project.app.procurement.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.AnalyticsViews.CompletedOrdersPage;
import com.project.app.procurement.service.AnalyticsService;

@RestController
@RequestMapping("/api/procurement/analytics")
public class AnalyticsController {
    private final AnalyticsService service;
    public AnalyticsController(AnalyticsService service) { this.service = service; }
    @GetMapping("/completed-orders")
    public CompletedOrdersPage completedOrders(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam MultiValueMap<String, String> params) {
        return service.completedOrders(principal, params);
    }
}
