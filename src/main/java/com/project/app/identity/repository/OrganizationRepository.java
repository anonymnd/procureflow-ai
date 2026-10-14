package com.project.app.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.project.app.identity.model.Organization;

import jakarta.persistence.LockModeType;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Organization o where o.id = :id")
    Optional<Organization> findLockedById(@Param("id") Long id);

    /** {@code likePattern} must already be lower-cased and have %, _ and \ escaped by the caller; null matches all. */
    @Query("""
            select distinct o from Organization o
            where exists (select 1 from Account a where a.organization = o
                          and a.supplierGrant = true and a.active = true and a.emailVerified = true)
              and (:likePattern is null or lower(o.displayName) like :likePattern escape '\\')
            order by o.displayName, o.id
            """)
    List<Organization> searchEligibleSupplierOrganizations(@Param("likePattern") String likePattern);
}
