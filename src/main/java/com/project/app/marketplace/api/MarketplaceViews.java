package com.project.app.marketplace.api;

import java.time.Instant;

import com.project.app.marketplace.model.Offering;
import com.project.app.marketplace.model.SupplierProfile;

public final class MarketplaceViews {
    private MarketplaceViews() { }

    public record ProfileView(Long id, String description, boolean published, Instant publishedAt) {
        public static ProfileView from(SupplierProfile profile) {
            return new ProfileView(profile.getId(), profile.getDescription(), profile.isPublished(), profile.getPublishedAt());
        }
    }

    public record OfferingView(Long id, String title, String description, String category,
            boolean published, Instant publishedAt) {
        public static OfferingView from(Offering offering) {
            return new OfferingView(offering.getId(), offering.getTitle(), offering.getDescription(),
                    offering.getCategory().name(), offering.isPublished(), offering.getPublishedAt());
        }
    }
}
