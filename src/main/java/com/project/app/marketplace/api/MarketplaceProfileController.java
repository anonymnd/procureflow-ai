package com.project.app.marketplace.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.marketplace.api.MarketplaceRequests.UpdateProfile;
import com.project.app.marketplace.api.MarketplaceViews.ProfileView;
import com.project.app.marketplace.service.MarketplaceService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/marketplace/profile")
public class MarketplaceProfileController {
    private final MarketplaceService marketplace;
    public MarketplaceProfileController(MarketplaceService marketplace) { this.marketplace = marketplace; }

    @GetMapping
    public ProfileView get(@AuthenticationPrincipal AccountPrincipal supplier) {
        return marketplace.getOwnProfile(supplier);
    }

    @PutMapping
    public ProfileView upsert(@AuthenticationPrincipal AccountPrincipal supplier, @Valid @RequestBody UpdateProfile request) {
        return marketplace.upsertProfile(supplier, request);
    }

    @PostMapping("/publish")
    public ProfileView publish(@AuthenticationPrincipal AccountPrincipal supplier) {
        return marketplace.publishProfile(supplier);
    }

    @PostMapping("/unpublish")
    public ProfileView unpublish(@AuthenticationPrincipal AccountPrincipal supplier) {
        return marketplace.unpublishProfile(supplier);
    }
}
