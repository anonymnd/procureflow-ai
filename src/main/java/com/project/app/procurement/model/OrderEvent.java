package com.project.app.procurement.model;
import java.time.Instant;
import jakarta.persistence.*;
import com.project.app.identity.model.Account;
@Entity @Table(name="order_events")
public class OrderEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false) private PurchaseOrder order;
    @Column(name="target_status",nullable=false,length=20) private String targetStatus;
    @Column(name="occurred_at",nullable=false) private Instant occurredAt;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="acting_account_id",nullable=false) private Account actingAccount;
    protected OrderEvent() { }
    public OrderEvent(PurchaseOrder order,String targetStatus,Instant occurredAt,Account actingAccount){this.order=order;this.targetStatus=targetStatus;this.occurredAt=occurredAt;this.actingAccount=actingAccount;}
    public String getTargetStatus(){return targetStatus;}
    public Instant getOccurredAt(){return occurredAt;}
}
