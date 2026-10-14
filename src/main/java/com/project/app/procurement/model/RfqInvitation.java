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

import com.project.app.identity.model.Organization;

@Entity
@Table(name = "rfq_invitations")
public class RfqInvitation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_organization_id", nullable = false)
    private Organization supplierOrganization;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected RfqInvitation() { }

    public RfqInvitation(Rfq rfq, Organization supplierOrganization) {
        this.rfq = rfq;
        this.supplierOrganization = supplierOrganization;
    }

    public Long getId() { return id; }
    public Rfq getRfq() { return rfq; }
    public Organization getSupplierOrganization() { return supplierOrganization; }
    public Instant getCreatedAt() { return createdAt; }
}
