package com.project.app.procurement.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.QuotationRequests.DraftInput;
import com.project.app.procurement.api.QuotationViews.QuotationView;
import com.project.app.procurement.model.*;
import com.project.app.procurement.repository.*;

@Service
public class QuotationService {
    private static final BigDecimal MAX_PRICE = new BigDecimal("999999999.99");
    private final QuotationRepository quotations;
    private final QuotationItemRepository lines;
    private final RfqInvitationRepository invitations;
    private final RfqRepository rfqs;
    private final RfqItemRepository rfqItems;
    private final Clock clock;
    public QuotationService(QuotationRepository quotations, QuotationItemRepository lines,
            RfqInvitationRepository invitations, RfqRepository rfqs, RfqItemRepository rfqItems, Clock clock) {
        this.quotations = quotations; this.lines = lines; this.invitations = invitations;
        this.rfqs = rfqs; this.rfqItems = rfqItems; this.clock = clock;
    }
    @Transactional
    public QuotationView create(AccountPrincipal supplier, Long invitationId) {
        lockSentRfq(supplier, invitationId);
        RfqInvitation invitation = invitations.findLockedOwn(invitationId, supplier.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
        if (invitation.getRfq().isDraft()) throw ProcurementException.conflict("The RFQ has not been sent.");
        if (quotations.existsByInvitationId(invitationId)) throw ProcurementException.conflict("A quotation already exists.");
        return view(quotations.saveAndFlush(new Quotation(invitation)));
    }
    @Transactional(readOnly = true)
    public QuotationView get(AccountPrincipal supplier, Long invitationId) {
        return view(quotations.findByInvitationIdAndInvitationSupplierOrganizationId(invitationId, supplier.getOrganizationId())
                .orElseThrow(ProcurementException::notFound));
    }
    @Transactional
    public QuotationView replace(AccountPrincipal supplier, Long invitationId, DraftInput input) {
        lockSentRfq(supplier, invitationId);
        Quotation quotation = locked(supplier, invitationId);
        requireDraft(quotation);
        if (input == null || input.currency() == null || !Set.of("MAD", "EUR", "USD").contains(input.currency()))
            throw ProcurementException.badRequest("Choose MAD, EUR or USD.");
        if (input.validUntil() == null || !input.validUntil().isAfter(clock.instant()))
            throw ProcurementException.badRequest("validUntil must be in the future.");
        String terms = input.terms() == null ? null : input.terms().strip();
        if (terms != null && terms.codePointCount(0, terms.length()) > 2000) throw ProcurementException.badRequest("terms must be at most 2000 characters.");
        if (terms != null && terms.isEmpty()) terms = null;
        if (input.items() == null) throw ProcurementException.badRequest("items is required.");
        Map<Long, RfqItem> source = new HashMap<>();
        rfqItems.findByRfqIdOrderByIdAsc(quotation.getInvitation().getRfq().getId()).forEach(item -> source.put(item.getId(), item));
        Set<Long> seen = new HashSet<>();
        List<QuotationItem> replacement = new ArrayList<>();
        for (var item : input.items()) {
            if (item == null || item.rfqItemId() == null || !seen.add(item.rfqItemId()) || !source.containsKey(item.rfqItemId()))
                throw ProcurementException.badRequest("Item must belong to this RFQ and occur once.");
            BigDecimal price;
            try {
                if (item.unitPrice() == null || !item.unitPrice().matches("[0-9]+(?:\\.[0-9]+)?")) throw new NumberFormatException();
                price = new BigDecimal(item.unitPrice()).setScale(2, RoundingMode.UNNECESSARY);
                if (price.signum() <= 0 || price.compareTo(MAX_PRICE) > 0) throw new NumberFormatException();
            } catch (ArithmeticException | NumberFormatException invalid) {
                throw ProcurementException.badRequest("unitPrice must be positive, at most 999999999.99, with at most two decimal digits.");
            }
            replacement.add(new QuotationItem(quotation, source.get(item.rfqItemId()), price));
        }
        // Validate the whole replacement before any row/header mutation.
        quotation.replaceTerms(input.currency(), input.validUntil(), terms);
        lines.deleteForQuotation(quotation.getId());
        lines.saveAllAndFlush(replacement);
        return view(quotation);
    }
    @Transactional
    public QuotationView submit(AccountPrincipal supplier, Long invitationId) {
        lockSentRfq(supplier, invitationId);
        Quotation quotation = locked(supplier, invitationId);
        requireDraft(quotation);
        Instant now = clock.instant();
        if (quotation.getCurrency() == null || quotation.getValidUntil() == null || !quotation.getValidUntil().isAfter(now))
            throw ProcurementException.conflict("Save a quotation with future validity before submitting.");
        List<QuotationItem> response = lines.findByQuotationIdOrderByRfqItemIdAsc(quotation.getId());
        List<RfqItem> required = rfqItems.findByRfqIdOrderByIdAsc(quotation.getInvitation().getRfq().getId());
        Set<Long> requiredIds = new HashSet<>(); required.forEach(item -> requiredIds.add(item.getId()));
        Set<Long> responseIds = new HashSet<>(); response.forEach(item -> responseIds.add(item.getRfqItem().getId()));
        if (requiredIds.isEmpty() || !requiredIds.equals(responseIds)) throw ProcurementException.conflict("Price every RFQ item before submitting.");
        quotation.submit(now);
        quotations.flush();
        return view(quotation);
    }
    @Transactional(readOnly = true)
    public List<QuotationView> listBuyer(AccountPrincipal buyer, Long rfqId) {
        requireOwnRfq(buyer, rfqId);
        return quotations.findSubmittedOwn(rfqId, buyer.getOrganizationId()).stream().map(this::view).toList();
    }
    @Transactional(readOnly = true)
    public QuotationView getBuyer(AccountPrincipal buyer, Long rfqId, Long quotationId) {
        return view(quotations.findSubmittedOwnById(quotationId, rfqId, buyer.getOrganizationId()).orElseThrow(ProcurementException::notFound));
    }
    private void requireOwnRfq(AccountPrincipal buyer, Long rfqId) {
        rfqs.findByIdAndOrganizationId(rfqId, buyer.getOrganizationId()).orElseThrow(ProcurementException::notFound);
    }
    private Quotation locked(AccountPrincipal supplier, Long invitationId) {
        return quotations.findLockedOwn(invitationId, supplier.getOrganizationId()).orElseThrow(ProcurementException::notFound);
    }
    private void lockSentRfq(AccountPrincipal supplier, Long invitationId) {
        Long rfqId = invitations.findOwnRfqId(invitationId, supplier.getOrganizationId())
                .orElseThrow(ProcurementException::notFound);
        // The ID comes only from the scoped invitation lookup; load state first under lock.
        Rfq rfq = rfqs.findLockedById(rfqId)
                .orElseThrow(ProcurementException::notFound);
        if (rfq.getStatus() != Rfq.Status.SENT) throw ProcurementException.conflict("This RFQ is closed to quotation changes.");
    }
    private static void requireDraft(Quotation quotation) {
        if (!quotation.isDraft()) throw ProcurementException.conflict("Submitted quotations cannot be changed.");
    }
    private QuotationView view(Quotation quotation) {
        return QuotationView.from(quotation, lines.findByQuotationIdOrderByRfqItemIdAsc(quotation.getId()), clock.instant());
    }
}
