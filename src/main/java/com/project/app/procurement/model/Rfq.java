package com.project.app.procurement.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.project.app.identity.model.Organization;

@Entity
@Table(name = "rfqs")
public class Rfq {
    public enum Status { DRAFT, SENT, AWARDED }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winning_quotation_id", unique = true)
    private Quotation winningQuotation;
    @jakarta.persistence.OneToOne(mappedBy = "rfq", fetch = FetchType.LAZY)
    private PurchaseOrder order;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 160)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.DRAFT;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Rfq() { }

    public Rfq(Organization organization, String title) {
        this.organization = organization;
        this.title = title;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getTitle() { return title; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String value) { title = value; }
    public boolean isDraft() { return status == Status.DRAFT; }
    public void markSent() { status = Status.SENT; }
    public void award(Quotation quotation) { status = Status.AWARDED; winningQuotation = quotation; }
    public Long getWinningQuotationId() { return winningQuotation == null ? null : winningQuotation.getId(); }
    public Long getOrderId() { return order == null ? null : order.getId(); }
}
