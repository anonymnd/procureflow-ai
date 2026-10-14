package com.project.app.marketplace.api;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;

import com.project.app.marketplace.model.Offering;
import com.project.app.marketplace.model.Offering.Category;
import com.project.app.marketplace.model.SupplierProfile;

public final class PublicViews {
    private PublicViews() { }

    public record CategoryView(String code, String name) {
        public static List<CategoryView> all() {
            return List.of(
                    new CategoryView("IT_EQUIPMENT", "IT equipment"),
                    new CategoryView("OFFICE_SUPPLIES", "Office supplies"),
                    new CategoryView("FURNITURE", "Furniture"),
                    new CategoryView("INDUSTRIAL_SUPPLIES", "Industrial supplies"),
                    new CategoryView("OTHER", "Other"));
        }
        private static final java.util.Map<String, String> NAMES = all().stream()
                .collect(java.util.stream.Collectors.toMap(CategoryView::code, CategoryView::name));
        public static String nameOf(Category category) { return NAMES.get(category.name()); }
    }

    public record PublicOfferingView(Long id, String title, String description, String categoryCode,
            String categoryName, Instant publishedAt, Long supplierId, String supplierDisplayName) {
        public static PublicOfferingView from(Offering offering) {
            SupplierProfile profile = offering.getProfile();
            return new PublicOfferingView(offering.getId(), offering.getTitle(), offering.getDescription(),
                    offering.getCategory().name(), CategoryView.nameOf(offering.getCategory()), offering.getPublishedAt(),
                    profile.getOrganization().getId(), profile.getOrganization().getDisplayName());
        }
    }

    public record PublicSupplierView(Long id, String displayName, String description, List<PublicOfferingView> offerings) {
        public static PublicSupplierView from(SupplierProfile profile, List<Offering> publicOfferings) {
            return new PublicSupplierView(profile.getOrganization().getId(), profile.getOrganization().getDisplayName(),
                    profile.getDescription(), publicOfferings.stream().map(PublicOfferingView::from).toList());
        }
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
        public static PageResponse<PublicOfferingView> ofOfferings(Page<Offering> page) {
            return new PageResponse<>(page.getContent().stream().map(PublicOfferingView::from).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }
}
