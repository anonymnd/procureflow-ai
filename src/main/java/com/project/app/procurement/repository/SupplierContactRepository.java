package com.project.app.procurement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.app.procurement.model.SupplierContact;

public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {
    List<SupplierContact> findByBuyerOrganizationIdOrderByIdDesc(Long buyerOrganizationId);

    Optional<SupplierContact> findByIdAndBuyerOrganizationId(Long id, Long buyerOrganizationId);
}
