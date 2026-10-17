package com.project.app.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.app.procurement.model.RfqInvitation;

public interface RfqInvitationRepository extends JpaRepository<RfqInvitation, Long> {
    // Scalar lookup avoids loading an unlocked RFQ into the persistence context.
    @org.springframework.data.jpa.repository.Query("select i.rfq.id from RfqInvitation i where i.id = :id and i.supplierOrganization.id = :organizationId")
    Optional<Long> findOwnRfqId(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("organizationId") Long organizationId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select i from RfqInvitation i where i.id = :id and i.supplierOrganization.id = :organizationId")
    Optional<RfqInvitation> findLockedOwn(@org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("organizationId") Long organizationId);
    List<RfqInvitation> findBySupplierOrganizationIdOrderByIdDesc(Long supplierOrganizationId);

    Optional<RfqInvitation> findByIdAndSupplierOrganizationId(Long id, Long supplierOrganizationId);

    List<RfqInvitation> findByRfqIdOrderByIdAsc(Long rfqId);
}
