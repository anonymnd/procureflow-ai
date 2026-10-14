package com.project.app.procurement.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.project.app.procurement.model.Quotation;
import com.project.app.procurement.model.QuotationItem;

public final class QuotationViews {
    private QuotationViews() { }
    public record QuotationItemView(Long rfqItemId, String description, int quantity, String unit,
            String unitPrice, String lineTotal) { }
    public record QuotationView(Long id, Long invitationId, Long rfqId, Long supplierOrganizationId,
            String supplierDisplayName, String status, String currency, Instant validUntil,
            String terms, Instant submittedAt, boolean expired, List<QuotationItemView> items, String subtotal) {
        public static QuotationView from(Quotation quotation, List<QuotationItem> lines, Instant now) {
            var invitation = quotation.getInvitation();
            var supplier = invitation.getSupplierOrganization();
            BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
            for (QuotationItem line : lines) subtotal = subtotal.add(total(line));
            return new QuotationView(quotation.getId(), invitation.getId(), invitation.getRfq().getId(),
                    supplier.getId(), supplier.getDisplayName(), quotation.getStatus().name(), quotation.getCurrency(),
                    quotation.getValidUntil(), quotation.getTerms(), quotation.getSubmittedAt(),
                    quotation.getValidUntil() != null && !quotation.getValidUntil().isAfter(now),
                    lines.stream().map(line -> new QuotationItemView(line.getRfqItem().getId(),
                            line.getRfqItem().getDescription(), line.getRfqItem().getQuantity(), line.getRfqItem().getUnit(),
                            line.getUnitPrice().setScale(2).toPlainString(), total(line).toPlainString())).toList(),
                    quotation.getCurrency() == null ? null : subtotal.toPlainString());
        }
        private static BigDecimal total(QuotationItem line) {
            return line.getUnitPrice().multiply(BigDecimal.valueOf(line.getRfqItem().getQuantity())).setScale(2);
        }
    }
}
