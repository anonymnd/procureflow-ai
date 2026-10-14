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
@Table(name = "supplier_contacts")
public class SupplierContact {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_organization_id", nullable = false)
    private Organization buyerOrganization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_organization_id", nullable = false)
    private Organization supplierOrganization;

    @Column(length = 2000)
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected SupplierContact() { }

    public SupplierContact(Organization buyerOrganization, Organization supplierOrganization, String note) {
        this.buyerOrganization = buyerOrganization;
        this.supplierOrganization = supplierOrganization;
        this.note = note;
    }

    public Long getId() { return id; }
    public Organization getBuyerOrganization() { return buyerOrganization; }
    public Organization getSupplierOrganization() { return supplierOrganization; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}
