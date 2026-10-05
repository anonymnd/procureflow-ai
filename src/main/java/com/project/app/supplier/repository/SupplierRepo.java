package com.project.app.supplier.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.project.app.supplier.model.Supplier;

public interface SupplierRepo extends JpaRepository<Supplier, Long> {

}
