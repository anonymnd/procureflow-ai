package com.project.app.identity.model;

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

@Entity
@Table(name = "identity_tokens")
public class IdentityToken {
    public enum Purpose { VERIFICATION, INVITATION }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Purpose purpose;

    @Column(nullable = false, length = 320)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    protected IdentityToken() { }

    public IdentityToken(String tokenHash, Purpose purpose, String email, Organization organization,
            Account account, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.purpose = purpose;
        this.email = email;
        this.organization = organization;
        this.account = account;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public Purpose getPurpose() { return purpose; }
    public String getEmail() { return email; }
    public Organization getOrganization() { return organization; }
    public Account getAccount() { return account; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public void consume(Instant when) { consumedAt = when; }
}
