package com.project.app.marketplace.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.project.app.identity.model.Organization;

@Entity
@Table(name = "marketplace_profiles")
public class SupplierProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected SupplierProfile() { }

    public SupplierProfile(Organization organization, String description) {
        this.organization = organization;
        this.description = description;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getDescription() { return description; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setDescription(String value) { description = value; }

    public void publish(Instant when) {
        published = true;
        publishedAt = when;
    }

    public void unpublish() {
        published = false;
    }
}
