package com.project.app.procurement.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "quotation_items")
public class QuotationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quotation_id", nullable = false) private Quotation quotation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_item_id", nullable = false) private RfqItem rfqItem;
    @Column(name = "unit_price", nullable = false, precision = 11, scale = 2) private BigDecimal unitPrice;
    protected QuotationItem() { }
    public QuotationItem(Quotation quotation, RfqItem rfqItem, BigDecimal unitPrice) {
        this.quotation = quotation; this.rfqItem = rfqItem; this.unitPrice = unitPrice;
    }
    public RfqItem getRfqItem() { return rfqItem; }
    public BigDecimal getUnitPrice() { return unitPrice; }
}
