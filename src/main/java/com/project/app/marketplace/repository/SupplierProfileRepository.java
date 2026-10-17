package com.project.app.marketplace.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.app.marketplace.model.SupplierProfile;

public interface SupplierProfileRepository extends JpaRepository<SupplierProfile, Long> {
    Optional<SupplierProfile> findByOrganizationId(Long organizationId);

    Optional<SupplierProfile> findByOrganizationIdAndPublishedTrue(Long organizationId);
}
