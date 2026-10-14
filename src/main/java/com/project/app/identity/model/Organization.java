package com.project.app.identity.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "organizations")
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected Organization() { }

    public Organization(String displayName) { this.displayName = displayName; }

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public Instant getCreatedAt() { return createdAt; }
}
