package com.project.app.procurement.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.project.app.procurement.model.Quotation;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quotation q where q.id = :id and q.invitation.rfq.id = :rfqId and q.invitation.rfq.organization.id = :organizationId")
    Optional<Quotation> findLockedBuyer(@Param("id") Long id,@Param("rfqId") Long rfqId,@Param("organizationId") Long organizationId);
    Optional<Quotation> findByInvitationIdAndInvitationSupplierOrganizationId(Long invitationId, Long organizationId);
    boolean existsByInvitationId(Long invitationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from Quotation q where q.invitation.id = :invitationId and q.invitation.supplierOrganization.id = :organizationId")
    Optional<Quotation> findLockedOwn(@Param("invitationId") Long invitationId, @Param("organizationId") Long organizationId);
    @Query("select q from Quotation q where q.invitation.rfq.id = :rfqId and q.invitation.rfq.organization.id = :organizationId and q.status in (com.project.app.procurement.model.Quotation.Status.SUBMITTED, com.project.app.procurement.model.Quotation.Status.ACCEPTED) order by q.id")
    List<Quotation> findSubmittedOwn(@Param("rfqId") Long rfqId, @Param("organizationId") Long organizationId);
    @Query("select q from Quotation q where q.id = :id and q.invitation.rfq.id = :rfqId and q.invitation.rfq.organization.id = :organizationId and q.status in (com.project.app.procurement.model.Quotation.Status.SUBMITTED, com.project.app.procurement.model.Quotation.Status.ACCEPTED)")
    Optional<Quotation> findSubmittedOwnById(@Param("id") Long id, @Param("rfqId") Long rfqId, @Param("organizationId") Long organizationId);
}
