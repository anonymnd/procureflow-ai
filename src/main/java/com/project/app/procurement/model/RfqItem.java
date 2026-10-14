package com.project.app.procurement.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "rfq_items")
public class RfqItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 40)
    private String unit;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected RfqItem() { }

    public RfqItem(Rfq rfq, String description, int quantity, String unit) {
        this.rfq = rfq;
        this.description = description;
        this.quantity = quantity;
        this.unit = unit;
    }

    public Long getId() { return id; }
    public Rfq getRfq() { return rfq; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDescription(String value) { description = value; }
    public void setQuantity(int value) { quantity = value; }
    public void setUnit(String value) { unit = value; }
}
