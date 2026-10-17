package com.project.app.marketplace.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.marketplace.api.MarketplaceRequests.UpdateOffering;
import com.project.app.marketplace.api.MarketplaceViews.OfferingView;
import com.project.app.marketplace.service.MarketplaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/marketplace/offerings")
public class MarketplaceOfferingController {
    private final MarketplaceService marketplace;
    public MarketplaceOfferingController(MarketplaceService marketplace) { this.marketplace = marketplace; }

    @GetMapping
    public List<OfferingView> list(@AuthenticationPrincipal AccountPrincipal supplier) {
        return marketplace.listOwnOfferings(supplier);
    }

    @GetMapping("/{id}")
    public OfferingView get(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long id) {
        return marketplace.getOwnOffering(supplier, id);
    }

    @PostMapping
    public ResponseEntity<OfferingView> create(@AuthenticationPrincipal AccountPrincipal supplier,
            @Valid @RequestBody UpdateOffering request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(marketplace.createOffering(supplier, request));
    }

    @PutMapping("/{id}")
    public OfferingView update(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long id,
            @Valid @RequestBody UpdateOffering request) {
        return marketplace.updateOffering(supplier, id, request);
    }

    @PostMapping("/{id}/publish")
    public OfferingView publish(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long id) {
        return marketplace.publishOffering(supplier, id);
    }

    @PostMapping("/{id}/unpublish")
    public OfferingView unpublish(@AuthenticationPrincipal AccountPrincipal supplier, @PathVariable Long id) {
        return marketplace.unpublishOffering(supplier, id);
    }
}
