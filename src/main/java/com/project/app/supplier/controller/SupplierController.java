package com.project.app.supplier.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.app.supplier.DTO.SupplierDtoRequest;
import com.project.app.supplier.DTO.SupplierDtoResponse;
import com.project.app.supplier.service.SupplierService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@RestController
@Setter
@Getter
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping()
    public ResponseEntity<List<SupplierDtoResponse>> getAllSuppliers() {

        return ResponseEntity.ok(supplierService.getAllSuppliers());

    }

    @GetMapping("/{id}")
    public ResponseEntity<SupplierDtoResponse> getSupplierById(@PathVariable long id) {
        return ResponseEntity.ok(supplierService.getSupplierById(id));

    }

    @PostMapping
    public ResponseEntity<SupplierDtoResponse> createSupplier(@RequestBody @Valid SupplierDtoRequest supplier) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supplierService.createSupplier(supplier));

    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDtoResponse> updateSupplier(@PathVariable @NonNull Long id,
            @RequestBody @Valid SupplierDtoRequest updatedSupplier) {
        return ResponseEntity.ok(supplierService.updateSupplier(id, updatedSupplier));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable @NonNull Long id) {
        supplierService.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }

}