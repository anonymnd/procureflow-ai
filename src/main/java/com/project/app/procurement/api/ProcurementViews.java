package com.project.app.procurement.api;

import java.time.Instant;
import java.util.List;

import com.project.app.identity.model.Organization;
import com.project.app.procurement.model.Rfq;
import com.project.app.procurement.model.RfqInvitation;
import com.project.app.procurement.model.RfqItem;
import com.project.app.procurement.model.SupplierContact;

public final class ProcurementViews {
    private ProcurementViews() { }

    public record OrganizationSummaryView(Long id, String displayName) {
        public static OrganizationSummaryView from(Organization organization) {
            return new OrganizationSummaryView(organization.getId(), organization.getDisplayName());
        }
    }

    public record ContactView(Long id, Long supplierOrganizationId, String supplierDisplayName, String note, Instant createdAt) {
        public static ContactView from(SupplierContact contact) {
            return new ContactView(contact.getId(), contact.getSupplierOrganization().getId(),
                    contact.getSupplierOrganization().getDisplayName(), contact.getNote(), contact.getCreatedAt());
        }
    }

    public record RfqItemView(Long id, String description, int quantity, String unit) {
        public static RfqItemView from(RfqItem item) {
            return new RfqItemView(item.getId(), item.getDescription(), item.getQuantity(), item.getUnit());
        }
    }

    public record RfqView(Long id, String title, String status, Instant createdAt, List<RfqItemView> items, Long winningQuotationId, Long orderId) {
        public static RfqView from(Rfq rfq, List<RfqItem> rfqItems) {
            return new RfqView(rfq.getId(), rfq.getTitle(), rfq.getStatus().name(), rfq.getCreatedAt(),
                    rfqItems.stream().map(RfqItemView::from).toList(), rfq.getWinningQuotationId(), rfq.getOrderId());
        }
    }

    public record InvitationView(Long id, Long rfqId, String rfqTitle, String buyerDisplayName,
            Instant createdAt, List<RfqItemView> items) {
        public static InvitationView from(RfqInvitation invitation, List<RfqItem> rfqItems) {
            Rfq rfq = invitation.getRfq();
            return new InvitationView(invitation.getId(), rfq.getId(), rfq.getTitle(),
                    rfq.getOrganization().getDisplayName(), invitation.getCreatedAt(),
                    rfqItems.stream().map(RfqItemView::from).toList());
        }
    }
}
