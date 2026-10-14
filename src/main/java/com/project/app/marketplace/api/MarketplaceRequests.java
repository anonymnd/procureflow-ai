package com.project.app.marketplace.api;

import jakarta.validation.constraints.NotNull;

public final class MarketplaceRequests {
    private MarketplaceRequests() { }

    public record UpdateProfile(@NotNull String description) { }

    public record UpdateOffering(@NotNull String title, @NotNull String description, @NotNull String category) { }
}
