package com.project.app.marketplace.model;

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
import jakarta.persistence.Version;

@Entity
@Table(name = "marketplace_offerings")
public class Offering {
    public enum Category { IT_EQUIPMENT, OFFICE_SUPPLIES, FURNITURE, INDUSTRIAL_SUPPLIES, OTHER }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private SupplierProfile profile;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Offering() { }

    public Offering(SupplierProfile profile, String title, String description, Category category) {
        this.profile = profile;
        this.title = title;
        this.description = description;
        this.category = category;
    }

    public Long getId() { return id; }
    public SupplierProfile getProfile() { return profile; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public boolean isPublished() { return published; }
    public Instant getPublishedAt() { return publishedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void setTitle(String value) { title = value; }
    public void setDescription(String value) { description = value; }
    public void setCategory(Category value) { category = value; }

    public void publish(Instant when) {
        published = true;
        publishedAt = when;
    }

    public void unpublish() {
        published = false;
    }
}
