package com.project.app.marketplace.service;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.marketplace.api.PublicViews.CategoryView;
import com.project.app.marketplace.api.PublicViews.PageResponse;
import com.project.app.marketplace.api.PublicViews.PublicOfferingView;
import com.project.app.marketplace.api.PublicViews.PublicSupplierView;
import com.project.app.marketplace.model.Offering.Category;
import com.project.app.marketplace.model.SupplierProfile;
import com.project.app.marketplace.repository.OfferingRepository;
import com.project.app.marketplace.repository.SupplierProfileRepository;

@Service
public class PublicCatalogService {
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;
    private static final int MAX_QUERY_CODE_POINTS = 100;

    private final SupplierProfileRepository profiles;
    private final OfferingRepository offerings;

    public PublicCatalogService(SupplierProfileRepository profiles, OfferingRepository offerings) {
        this.profiles = profiles;
        this.offerings = offerings;
    }

    public List<CategoryView> categories() {
        return CategoryView.all();
    }

    @Transactional(readOnly = true)
    public PageResponse<PublicOfferingView> search(String rawText, String rawCategory, Integer rawPage, Integer rawSize) {
        String likePattern = likePattern(rawText);
        Category category = parseCategory(rawCategory);
        int page = rawPage == null ? 0 : rawPage;
        int size = rawSize == null ? DEFAULT_SIZE : rawSize;
        if (page < 0) throw MarketplaceException.badRequest("page must not be negative.");
        if (size < 1 || size > MAX_SIZE) throw MarketplaceException.badRequest("size must be 1-" + MAX_SIZE + ".");
        var result = offerings.searchPublished(category, likePattern, PageRequest.of(page, size));
        return PageResponse.ofOfferings(result);
    }

    @Transactional(readOnly = true)
    public PublicOfferingView getOffering(Long id) {
        return offerings.findPublishedById(id).map(PublicOfferingView::from).orElseThrow(MarketplaceException::notFound);
    }

    @Transactional(readOnly = true)
    public PublicSupplierView getSupplier(Long organizationId) {
        SupplierProfile profile = profiles.findByOrganizationIdAndPublishedTrue(organizationId)
                .orElseThrow(MarketplaceException::notFound);
        return PublicSupplierView.from(profile, offerings.findPublishedByProfileId(profile.getId()));
    }

    private static Category parseCategory(String raw) {
        if (raw == null) return null;
        if (raw.isEmpty()) throw MarketplaceException.badRequest("category must not be empty.");
        try {
            return Category.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw MarketplaceException.badRequest("Unknown category: " + raw);
        }
    }

    /** Null means no text filter; caller passes the trimmed query as-is (blank already means no filter upstream). */
    private static String likePattern(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) return null;
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length > MAX_QUERY_CODE_POINTS) {
            throw MarketplaceException.badRequest("q must be at most " + MAX_QUERY_CODE_POINTS + " characters.");
        }
        String escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}
