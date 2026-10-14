package com.project.app.procurement.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity @Table(name="purchase_order_items")
public class PurchaseOrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private PurchaseOrder order;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="rfq_item_id",nullable=false) private RfqItem rfqItem;
    @Column(nullable=false,length=2000) private String description;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false,length=40) private String unit;
    @Column(name="unit_price",nullable=false,precision=11,scale=2) private BigDecimal unitPrice;
    @Column(name="line_total",nullable=false,precision=17,scale=2) private BigDecimal lineTotal;
    protected PurchaseOrderItem() { }
    public PurchaseOrderItem(PurchaseOrder order,QuotationItem source) {
        this.order=order;rfqItem=source.getRfqItem();description=rfqItem.getDescription();quantity=rfqItem.getQuantity();unit=rfqItem.getUnit();
        unitPrice=source.getUnitPrice();lineTotal=unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2);
    }
    public Long getRfqItemId(){return rfqItem.getId();}
    public String getDescription(){return description;}
    public int getQuantity(){return quantity;}
    public String getUnit(){return unit;}
    public BigDecimal getUnitPrice(){return unitPrice;}
    public BigDecimal getLineTotal(){return lineTotal;}
}
