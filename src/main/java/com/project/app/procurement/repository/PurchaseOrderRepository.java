package com.project.app.procurement.repository;

import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.project.app.procurement.model.PurchaseOrder;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id and (o.buyerOrganization.id = :organizationId or o.supplierOrganization.id = :organizationId)")
    Optional<PurchaseOrder> findLockedParticipant(@Param("id") Long id,@Param("organizationId") Long org);
    Optional<PurchaseOrder> findByRfqId(Long rfqId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.rfq.id = :rfqId")
    Optional<PurchaseOrder> findLockedByRfqId(@Param("rfqId") Long rfqId);
    @Query("select o from PurchaseOrder o where (:buyer = true and o.buyerOrganization.id = :organizationId) or (:supplier = true and o.supplierOrganization.id = :organizationId) order by o.id desc")
    List<PurchaseOrder> findParticipant(@Param("organizationId") Long org,@Param("buyer") boolean buyer,@Param("supplier") boolean supplier);
    @Query("select o from PurchaseOrder o where o.id = :id and ((:buyer = true and o.buyerOrganization.id = :organizationId) or (:supplier = true and o.supplierOrganization.id = :organizationId))")
    Optional<PurchaseOrder> findParticipantById(@Param("id") Long id,@Param("organizationId") Long org,@Param("buyer") boolean buyer,@Param("supplier") boolean supplier);
}
