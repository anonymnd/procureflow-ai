package com.project.app.procurement.model;

import java.time.Instant;
import jakarta.persistence.*;
import com.project.app.identity.model.Account;
import com.project.app.identity.model.Organization;

@Entity @Table(name="purchase_orders")
public class PurchaseOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="rfq_id",nullable=false,unique=true) private Rfq rfq;
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="quotation_id",nullable=false,unique=true) private Quotation quotation;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="buyer_organization_id",nullable=false) private Organization buyerOrganization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="supplier_organization_id",nullable=false) private Organization supplierOrganization;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="accepting_account_id",nullable=false) private Account acceptingAccount;
    @Column(name="rfq_title",nullable=false,length=160) private String rfqTitle;
    @Column(name="buyer_display_name",nullable=false,length=160) private String buyerDisplayName;
    @Column(name="supplier_display_name",nullable=false,length=160) private String supplierDisplayName;
    @Column(nullable=false,length=20) private String status="CREATED";
    @Column(nullable=false,length=3) private String currency;
    @Column(length=2000) private String terms;
    @Column(name="valid_until",nullable=false) private Instant validUntil;
    @Column(name="accepted_at",nullable=false) private Instant acceptedAt;
    protected PurchaseOrder() { }
    public PurchaseOrder(Rfq rfq,Quotation quotation,Account actor,Instant now) {
        this.rfq=rfq;this.quotation=quotation;buyerOrganization=rfq.getOrganization();
        supplierOrganization=quotation.getInvitation().getSupplierOrganization();acceptingAccount=actor;
        rfqTitle=rfq.getTitle();buyerDisplayName=buyerOrganization.getDisplayName();supplierDisplayName=supplierOrganization.getDisplayName();
        currency=quotation.getCurrency();terms=quotation.getTerms();validUntil=quotation.getValidUntil();acceptedAt=now;
    }
    public Long getId(){return id;}
    public Rfq getRfq(){return rfq;}
    public Quotation getQuotation(){return quotation;}
    public Organization getBuyerOrganization(){return buyerOrganization;}
    public Organization getSupplierOrganization(){return supplierOrganization;}
    public String getRfqTitle(){return rfqTitle;}
    public String getBuyerDisplayName(){return buyerDisplayName;}
    public String getSupplierDisplayName(){return supplierDisplayName;}
    public String getStatus(){return status;}
    public void transition(String predecessor,String target){
        if(!status.equals(predecessor))throw com.project.app.procurement.service.ProcurementException.conflict("This order is not ready for that action.");
        status=target;
    }
    public String getCurrency(){return currency;}
    public String getTerms(){return terms;}
    public Instant getValidUntil(){return validUntil;}
    public Instant getAcceptedAt(){return acceptedAt;}
}
