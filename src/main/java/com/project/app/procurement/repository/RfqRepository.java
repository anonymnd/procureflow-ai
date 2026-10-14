package com.project.app.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.project.app.procurement.model.Rfq;

public interface RfqRepository extends JpaRepository<Rfq, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rfq r where r.id = :id")
    Optional<Rfq> findLockedById(@Param("id") Long id);
    Optional<Rfq> findByIdAndOrganizationId(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rfq r where r.id = :id and r.organization.id = :organizationId")
    Optional<Rfq> findLockedByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    List<Rfq> findByOrganizationIdOrderByIdDesc(Long organizationId);
}
