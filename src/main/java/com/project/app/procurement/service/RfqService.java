package com.project.app.procurement.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.app.identity.model.Organization;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.repository.OrganizationRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementRequests.CreateRfq;
import com.project.app.procurement.api.ProcurementRequests.RfqItemInput;
import com.project.app.procurement.api.ProcurementRequests.SendRfq;
import com.project.app.procurement.api.ProcurementRequests.UpdateRfq;
import com.project.app.procurement.api.ProcurementViews.OrganizationSummaryView;
import com.project.app.procurement.api.ProcurementViews.RfqItemView;
import com.project.app.procurement.api.ProcurementViews.RfqView;
import com.project.app.procurement.model.Rfq;
import com.project.app.procurement.model.RfqInvitation;
import com.project.app.procurement.model.RfqItem;
import com.project.app.procurement.repository.RfqInvitationRepository;
import com.project.app.procurement.repository.RfqItemRepository;
import com.project.app.procurement.repository.RfqRepository;

@Service
public class RfqService {
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 1_000_000;

    private final RfqRepository rfqs;
    private final RfqItemRepository items;
    private final RfqInvitationRepository invitations;
    private final OrganizationRepository organizations;
    private final AccountRepository accounts;

    public RfqService(RfqRepository rfqs, RfqItemRepository items, RfqInvitationRepository invitations,
            OrganizationRepository organizations, AccountRepository accounts) {
        this.rfqs = rfqs;
        this.items = items;
        this.invitations = invitations;
        this.organizations = organizations;
        this.accounts = accounts;
    }

    @Transactional(readOnly = true)
    public List<RfqView> listOwn(AccountPrincipal buyer) {
        return rfqs.findByOrganizationIdOrderByIdDesc(buyer.getOrganizationId()).stream()
                .map(rfq -> RfqView.from(rfq, items.findByRfqIdOrderByIdAsc(rfq.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public RfqView getOwn(AccountPrincipal buyer, Long rfqId) {
        Rfq rfq = ownRfq(buyer, rfqId);
        return RfqView.from(rfq, items.findByRfqIdOrderByIdAsc(rfq.getId()));
    }

    @Transactional
    public RfqView create(AccountPrincipal buyer, CreateRfq request) {
        String title = requireTitle(request.title());
        Organization organization = organizations.getReferenceById(buyer.getOrganizationId());
        Rfq rfq = rfqs.save(new Rfq(organization, title));
        return RfqView.from(rfq, List.of());
    }

    @Transactional
    public RfqView updateTitle(AccountPrincipal buyer, Long rfqId, UpdateRfq request) {
        Rfq rfq = ownLockedRfq(buyer, rfqId);
        requireDraft(rfq);
        rfq.setTitle(requireTitle(request.title()));
        return RfqView.from(rfqs.save(rfq), items.findByRfqIdOrderByIdAsc(rfq.getId()));
    }

    @Transactional
    public RfqItemView addItem(AccountPrincipal buyer, Long rfqId, RfqItemInput input) {
        Rfq rfq = ownLockedRfq(buyer, rfqId);
        requireDraft(rfq);
        RfqItem item = new RfqItem(rfq, requireDescription(input.description()), requireQuantity(input.quantity()),
                requireUnit(input.unit()));
        return RfqItemView.from(items.save(item));
    }

    @Transactional
    public RfqItemView updateItem(AccountPrincipal buyer, Long rfqId, Long itemId, RfqItemInput input) {
        Rfq rfq = ownLockedRfq(buyer, rfqId);
        requireDraft(rfq);
        RfqItem item = items.findByIdAndRfqIdAndOwningOrganizationId(itemId, rfqId, buyer.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
        item.setDescription(requireDescription(input.description()));
        item.setQuantity(requireQuantity(input.quantity()));
        item.setUnit(requireUnit(input.unit()));
        return RfqItemView.from(items.save(item));
    }

    @Transactional
    public void removeItem(AccountPrincipal buyer, Long rfqId, Long itemId) {
        Rfq rfq = ownLockedRfq(buyer, rfqId);
        requireDraft(rfq);
        RfqItem item = items.findByIdAndRfqIdAndOwningOrganizationId(itemId, rfqId, buyer.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
        items.delete(item);
    }

    @Transactional(readOnly = true)
    public List<OrganizationSummaryView> searchRecipients(String query) {
        String likePattern = likePattern(query);
        return organizations.searchEligibleSupplierOrganizations(likePattern).stream()
                .map(OrganizationSummaryView::from).toList();
    }

    @Transactional
    public RfqView send(AccountPrincipal buyer, Long rfqId, SendRfq request) {
        Rfq rfq = ownLockedRfq(buyer, rfqId);
        requireDraft(rfq);
        if (items.countByRfqId(rfqId) == 0) {
            throw ProcurementException.conflict("Add at least one item before sending.");
        }
        Set<Long> recipientIds = new LinkedHashSet<>(request.recipientOrganizationIds());
        if (recipientIds.isEmpty()) {
            throw ProcurementException.badRequest("Select at least one recipient.");
        }
        if (recipientIds.contains(buyer.getOrganizationId())) {
            throw ProcurementException.badRequest("Cannot send an RFQ to your own organization.");
        }
        for (Long recipientId : recipientIds) {
            if (!accounts.existsByOrganizationIdAndSupplierGrantTrueAndActiveTrueAndEmailVerifiedTrue(recipientId)) {
                throw ProcurementException.badRequest("Recipient " + recipientId + " has no active supplier-granted employee.");
            }
        }
        for (Long recipientId : recipientIds) {
            Organization supplier = organizations.getReferenceById(recipientId);
            invitations.save(new RfqInvitation(rfq, supplier));
        }
        rfq.markSent();
        return RfqView.from(rfqs.save(rfq), items.findByRfqIdOrderByIdAsc(rfqId));
    }

    private Rfq ownRfq(AccountPrincipal buyer, Long rfqId) {
        return rfqs.findByIdAndOrganizationId(rfqId, buyer.getOrganizationId()).orElseThrow(ProcurementException::notFound);
    }

    private Rfq ownLockedRfq(AccountPrincipal buyer, Long rfqId) {
        return rfqs.findLockedByIdAndOrganizationId(rfqId, buyer.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
    }

    private static void requireDraft(Rfq rfq) {
        if (!rfq.isDraft()) throw ProcurementException.conflict("This RFQ has already been sent and cannot be changed.");
    }

    private static String requireTitle(String raw) {
        return requireTrimmed(raw, 1, 160, "title");
    }

    private static String requireDescription(String raw) {
        return requireTrimmed(raw, 1, 2000, "description");
    }

    private static String requireUnit(String raw) {
        return requireTrimmed(raw, 1, 40, "unit");
    }

    private static int requireQuantity(Integer quantity) {
        if (quantity == null || quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw ProcurementException.badRequest("quantity must be " + MIN_QUANTITY + "-" + MAX_QUANTITY + ".");
        }
        return quantity;
    }

    private static String requireTrimmed(String raw, int minCodePoints, int maxCodePoints, String field) {
        if (raw == null) throw ProcurementException.badRequest(field + " is required.");
        String trimmed = raw.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < minCodePoints || length > maxCodePoints) {
            throw ProcurementException.badRequest(field + " must be " + minCodePoints + "-" + maxCodePoints + " characters.");
        }
        return trimmed;
    }

    private static String likePattern(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        if (trimmed.isEmpty()) return null;
        String escaped = trimmed.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped.toLowerCase(Locale.ROOT) + "%";
    }
}
