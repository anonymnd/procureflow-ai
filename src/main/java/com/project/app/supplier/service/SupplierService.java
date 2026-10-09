package com.project.app.supplier.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.app.supplier.DTO.SupplierDtoRequest;
import com.project.app.supplier.DTO.SupplierDtoResponse;
import com.project.app.common.exception.ResourceNotFoundException;
import com.project.app.supplier.mapper.SupplierMapper;
import com.project.app.supplier.model.Supplier;
import com.project.app.supplier.repository.SupplierRepo;

@Service
public class SupplierService {

    private final SupplierRepo supplierRepo;
    private final SupplierMapper supplierMapper;

    public SupplierService(SupplierRepo supplierRepo, SupplierMapper supplierMapper) {
        this.supplierRepo = supplierRepo;
        this.supplierMapper = supplierMapper;
    }

    public List<SupplierDtoResponse> getAllSuppliers() {

        return supplierRepo.findAll()
                .stream()
                .map(supplierMapper::toDto)
                .toList();
    }

    public SupplierDtoResponse getSupplierById(Long id) {
        Supplier suplier = supplierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        return supplierMapper.toDto(suplier);
    }

    public SupplierDtoResponse createSupplier(SupplierDtoRequest supplier) {
        Supplier newSupplier = supplierMapper.toEntity(supplier);
        Supplier savedSupplier = supplierRepo.save(newSupplier);
        return supplierMapper.toDto(savedSupplier);
    }

    public SupplierDtoResponse updateSupplier(Long id, SupplierDtoRequest updatedSupplier) {
        return supplierRepo.findById(id)
                .map(supplier -> {
                    supplier.setName(updatedSupplier.name());
                    return supplierRepo.save(supplier);
                }).map(supplierMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
    }

    public void deleteSupplier(Long id) {
        Supplier supplier = supplierRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        supplierRepo.delete(supplier);
    }
}