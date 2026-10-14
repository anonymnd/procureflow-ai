package com.project.app.marketplace.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.identity.security.AccountPrincipal;
import com.project.app.marketplace.api.MarketplaceRequests.UpdateOffering;
import com.project.app.marketplace.api.MarketplaceRequests.UpdateProfile;
import com.project.app.marketplace.api.MarketplaceViews.OfferingView;
import com.project.app.marketplace.api.MarketplaceViews.ProfileView;
import com.project.app.marketplace.model.Offering;
import com.project.app.marketplace.model.Offering.Category;
import com.project.app.marketplace.model.SupplierProfile;
import com.project.app.marketplace.repository.OfferingRepository;
import com.project.app.marketplace.repository.SupplierProfileRepository;
import com.project.app.identity.repository.OrganizationRepository;

@Service
public class MarketplaceService {
    private final SupplierProfileRepository profiles;
    private final OfferingRepository offerings;
    private final OrganizationRepository organizations;
    private final Clock clock;

    public MarketplaceService(SupplierProfileRepository profiles, OfferingRepository offerings,
            OrganizationRepository organizations, Clock clock) {
        this.profiles = profiles;
        this.offerings = offerings;
        this.organizations = organizations;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ProfileView getOwnProfile(AccountPrincipal supplier) {
        return ProfileView.from(ownProfile(supplier));
    }

    @Transactional
    public ProfileView upsertProfile(AccountPrincipal supplier, UpdateProfile request) {
        String description = requireTrimmed(request.description(), 1, 2000, "description");
        SupplierProfile profile = profiles.findByOrganizationId(supplier.getOrganizationId()).orElseGet(() -> {
            var organization = organizations.findLockedById(supplier.getOrganizationId())
                    .orElseThrow(MarketplaceException::notFound);
            return new SupplierProfile(organization, description);
        });
        profile.setDescription(description);
        return ProfileView.from(profiles.save(profile));
    }

    @Transactional
    public ProfileView publishProfile(AccountPrincipal supplier) {
        SupplierProfile profile = ownProfile(supplier);
        profile.publish(Instant.now(clock));
        return ProfileView.from(profiles.save(profile));
    }

    @Transactional
    public ProfileView unpublishProfile(AccountPrincipal supplier) {
        SupplierProfile profile = ownProfile(supplier);
        profile.unpublish();
        return ProfileView.from(profiles.save(profile));
    }

    @Transactional(readOnly = true)
    public List<OfferingView> listOwnOfferings(AccountPrincipal supplier) {
        return offerings.findAllByOwningOrganizationId(supplier.getOrganizationId()).stream()
                .map(OfferingView::from).toList();
    }

    @Transactional(readOnly = true)
    public OfferingView getOwnOffering(AccountPrincipal supplier, Long id) {
        return OfferingView.from(ownOffering(supplier, id));
    }

    @Transactional
    public OfferingView createOffering(AccountPrincipal supplier, UpdateOffering request) {
        SupplierProfile profile = profiles.findByOrganizationId(supplier.getOrganizationId())
                .orElseThrow(() -> MarketplaceException.conflict("Create a supplier profile before adding offerings."));
        String title = requireTrimmed(request.title(), 1, 160, "title");
        String description = requireTrimmed(request.description(), 1, 4000, "description");
        Category category = parseCategory(request.category());
        Offering offering = new Offering(profile, title, description, category);
        return OfferingView.from(offerings.save(offering));
    }

    @Transactional
    public OfferingView updateOffering(AccountPrincipal supplier, Long id, UpdateOffering request) {
        Offering offering = ownOffering(supplier, id);
        offering.setTitle(requireTrimmed(request.title(), 1, 160, "title"));
        offering.setDescription(requireTrimmed(request.description(), 1, 4000, "description"));
        offering.setCategory(parseCategory(request.category()));
        return OfferingView.from(offerings.save(offering));
    }

    @Transactional
    public OfferingView publishOffering(AccountPrincipal supplier, Long id) {
        Offering offering = ownOffering(supplier, id);
        offering.publish(Instant.now(clock));
        return OfferingView.from(offerings.save(offering));
    }

    @Transactional
    public OfferingView unpublishOffering(AccountPrincipal supplier, Long id) {
        Offering offering = ownOffering(supplier, id);
        offering.unpublish();
        return OfferingView.from(offerings.save(offering));
    }

    private SupplierProfile ownProfile(AccountPrincipal supplier) {
        return profiles.findByOrganizationId(supplier.getOrganizationId()).orElseThrow(MarketplaceException::notFound);
    }

    private Offering ownOffering(AccountPrincipal supplier, Long id) {
        return offerings.findByIdAndOwningOrganizationId(id, supplier.getOrganizationId())
                .orElseThrow(MarketplaceException::notFound);
    }

    private static Category parseCategory(String raw) {
        try {
            return Category.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            throw MarketplaceException.badRequest("Unknown category: " + raw);
        }
    }

    private static String requireTrimmed(String raw, int minCodePoints, int maxCodePoints, String field) {
        String trimmed = raw.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < minCodePoints || length > maxCodePoints) {
            throw MarketplaceException.badRequest(field + " must be " + minCodePoints + "-" + maxCodePoints + " characters.");
        }
        return trimmed;
    }
}
