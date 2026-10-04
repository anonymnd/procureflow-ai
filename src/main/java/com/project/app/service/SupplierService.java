package com.project.app.service;

import java.util.List;

import org.springframework.stereotype.Service;
import com.project.app.model.Supplier;
import com.project.app.repository.SupplierRepo;
import com.project.app.mapper.SupplierMapper;
import com.project.app.DTO.SupplierDtoRequest;
import com.project.app.DTO.SupplierDtoResponse;
import com.project.app.exceptions.ResourceNotFoundException;

@Service
public class SupplierService {

    private final SupplierRepo supplierRepo;

    public SupplierService(SupplierRepo supplierRepo) {
        this.supplierRepo = supplierRepo;
    }

    public List<SupplierDtoResponse> getAllSuppliers() {
        List<Supplier> suppliers = supplierRepo.findAll();
        if (suppliers.isEmpty()) {
            throw new ResourceNotFoundException("No suppliers found");
        }

        return suppliers.stream()
                .map(SupplierMapper::toDto)
                .toList();
    }

    public SupplierDtoResponse getSupplierById(Long id) {
        Supplier suplier = supplierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        return SupplierMapper.toDto(suplier);
    }

    public SupplierDtoResponse createSupplier(SupplierDtoRequest supplier) {
        Supplier newSupplier = SupplierMapper.toEntity(supplier);
        Supplier savedSupplier = supplierRepo.save(newSupplier);
        return SupplierMapper.toDto(savedSupplier);
    }

    public SupplierDtoResponse updateSupplier(Long id, SupplierDtoRequest updatedSupplier) {
        return supplierRepo.findById(id)
                .map(supplier -> {
                    supplier.setName(updatedSupplier.name());
                    return supplierRepo.save(supplier);
                }).map(SupplierMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        supplierRepo.delete(supplier);
    }

}
