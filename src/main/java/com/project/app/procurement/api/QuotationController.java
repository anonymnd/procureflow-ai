package com.project.app.procurement.api;

import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.QuotationRequests.DraftInput;
import com.project.app.procurement.api.QuotationViews.QuotationView;
import com.project.app.procurement.service.QuotationService;

@RestController
@RequestMapping("/api/procurement/invitations/{invitationId}/quotation")
public class QuotationController {
    private final QuotationService quotations;
    public QuotationController(QuotationService quotations) { this.quotations = quotations; }
    @PostMapping public ResponseEntity<QuotationView> create(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long invitationId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quotations.create(supplier, invitationId));
    }
    @GetMapping public QuotationView get(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long invitationId) {
        return quotations.get(supplier, invitationId);
    }
    @PutMapping public QuotationView replace(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long invitationId, @RequestBody DraftInput input) {
        return quotations.replace(supplier, invitationId, input);
    }
    @PostMapping("/submit") public QuotationView submit(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long invitationId) {
        return quotations.submit(supplier, invitationId);
    }
}
