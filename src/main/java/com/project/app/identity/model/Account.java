package com.project.app.identity.model;

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
import jakarta.persistence.Version;

@Entity
@Table(name = "accounts")
public class Account {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "organization_admin", nullable = false)
    private boolean organizationAdmin;

    @Column(name = "buyer_grant", nullable = false)
    private boolean buyerGrant;

    @Column(name = "supplier_grant", nullable = false)
    private boolean supplierGrant;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Account() { }

    public Account(String fullName, String email, String passwordHash, Organization organization,
            boolean organizationAdmin, boolean emailVerified) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.organization = organization;
        this.organizationAdmin = organizationAdmin;
        this.emailVerified = emailVerified;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Organization getOrganization() { return organization; }
    public boolean isOrganizationAdmin() { return organizationAdmin; }
    public boolean isBuyerGrant() { return buyerGrant; }
    public boolean isSupplierGrant() { return supplierGrant; }
    public boolean isActive() { return active; }
    public boolean isEmailVerified() { return emailVerified; }
    public long getAuthVersion() { return authVersion; }
    public Instant getCreatedAt() { return createdAt; }

    public void setBuyerGrant(boolean value) { buyerGrant = value; }
    public void setFullName(String value) { fullName = value; }
    public void setSupplierGrant(boolean value) { supplierGrant = value; }
    public void setOrganizationAdmin(boolean value) { organizationAdmin = value; }
    public void setActive(boolean value) { active = value; }
    public void setEmailVerified(boolean value) { emailVerified = value; }
    public void setPasswordHash(String value) { passwordHash = value; }
    public void invalidateSessions() { authVersion++; }
}
