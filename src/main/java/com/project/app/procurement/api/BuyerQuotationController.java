package com.project.app.procurement.api;

import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.QuotationViews.QuotationView;
import com.project.app.procurement.service.QuotationService;

@RestController
@RequestMapping("/api/procurement/rfqs/{rfqId}/quotations")
public class BuyerQuotationController {
    private final QuotationService quotations;
    public BuyerQuotationController(QuotationService quotations) { this.quotations = quotations; }
    @GetMapping public List<QuotationView> list(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long rfqId) {
        return quotations.listBuyer(buyer, rfqId);
    }
    @GetMapping("/{quotationId}") public QuotationView get(@AuthenticationPrincipal AccountPrincipal buyer, @PathVariable Long rfqId, @PathVariable Long quotationId) {
        return quotations.getBuyer(buyer, rfqId, quotationId);
    }
}
