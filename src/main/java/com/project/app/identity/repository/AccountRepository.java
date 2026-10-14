package com.project.app.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.app.identity.model.Account;

import jakarta.persistence.LockModeType;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByEmail(String email);

    Optional<Account> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Account> findByOrganizationIdOrderByFullNameAscIdAsc(Long organizationId);

    long countByOrganizationIdAndOrganizationAdminTrueAndActiveTrueAndEmailVerifiedTrue(Long organizationId);

    boolean existsByOrganizationIdAndSupplierGrantTrueAndActiveTrueAndEmailVerifiedTrue(Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id and a.organization.id = :organizationId")
    Optional<Account> findLockedInOrganization(@Param("id") Long id, @Param("organizationId") Long organizationId);
}
