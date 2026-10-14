package com.project.app.procurement.model;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "quotations")
public class Quotation {
    public enum Status { DRAFT, SUBMITTED, ACCEPTED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invitation_id", nullable = false, unique = true)
    private RfqInvitation invitation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private Status status = Status.DRAFT;
    @Column(length = 3) private String currency;
    @Column(name = "valid_until") private Instant validUntil;
    @Column(length = 2000) private String terms;
    @Column(name = "submitted_at") private Instant submittedAt;
    protected Quotation() { }
    public Quotation(RfqInvitation invitation) { this.invitation = invitation; }
    public Long getId() { return id; }
    public RfqInvitation getInvitation() { return invitation; }
    public Status getStatus() { return status; }
    public String getCurrency() { return currency; }
    public Instant getValidUntil() { return validUntil; }
    public String getTerms() { return terms; }
    public Instant getSubmittedAt() { return submittedAt; }
    public boolean isDraft() { return status == Status.DRAFT; }
    public void replaceTerms(String currency, Instant validUntil, String terms) {
        this.currency = currency; this.validUntil = validUntil; this.terms = terms;
    }
    public void submit(Instant now) { status = Status.SUBMITTED; submittedAt = now; }
    public void accept() { status = Status.ACCEPTED; }
}
