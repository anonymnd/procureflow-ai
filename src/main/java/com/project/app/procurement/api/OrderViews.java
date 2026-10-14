package com.project.app.procurement.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.project.app.procurement.model.*;

public final class OrderViews {
    private OrderViews() { }
    public record OrderItemView(Long rfqItemId,String description,int quantity,String unit,String unitPrice,String lineTotal) { }
    public record OrderHistoryView(String status,Instant occurredAt) { }
    public record OrderView(Long id,Long rfqId,Long quotationId,Long buyerOrganizationId,String buyerDisplayName,
            Long supplierOrganizationId,String supplierDisplayName,String rfqTitle,String status,String currency,
            String terms,Instant validUntil,Instant acceptedAt,List<OrderItemView> items,String subtotal,
            List<OrderHistoryView> history,List<String> allowedActions) {
        public static OrderView from(PurchaseOrder order,List<PurchaseOrderItem> lines,List<OrderEvent> events,List<String> allowedActions) {
            BigDecimal subtotal=lines.stream().map(PurchaseOrderItem::getLineTotal).reduce(BigDecimal.ZERO,BigDecimal::add).setScale(2);
            var history=new java.util.ArrayList<OrderHistoryView>();history.add(new OrderHistoryView("CREATED",order.getAcceptedAt()));
            events.forEach(event->history.add(new OrderHistoryView(event.getTargetStatus(),event.getOccurredAt())));
            return new OrderView(order.getId(),order.getRfq().getId(),order.getQuotation().getId(),order.getBuyerOrganization().getId(),
                    order.getBuyerDisplayName(),order.getSupplierOrganization().getId(),order.getSupplierDisplayName(),order.getRfqTitle(),
                    order.getStatus(),order.getCurrency(),order.getTerms(),order.getValidUntil(),order.getAcceptedAt(),
                    lines.stream().map(i->new OrderItemView(i.getRfqItemId(),i.getDescription(),i.getQuantity(),i.getUnit(),i.getUnitPrice().setScale(2).toPlainString(),i.getLineTotal().setScale(2).toPlainString())).toList(),subtotal.toPlainString(),List.copyOf(history),allowedActions);
        }
    }
}
