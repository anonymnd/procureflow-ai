package com.project.app.supplier.service;

import com.project.app.supplier.DTO.*;
import com.project.app.supplier.model.Supplier;
import com.project.app.supplier.mapper.SupplierMapper;
import com.project.app.supplier.repository.SupplierRepo;
import com.project.app.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepo supplierRepository;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private SupplierService supplierService;

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void shouldCreateSupplier() {

        // Arrange
        SupplierDtoRequest request = new SupplierDtoRequest("Atlas Supplies");

        Supplier supplier = new Supplier();

        Supplier savedSupplier = new Supplier();
        savedSupplier.setId(1L);
        savedSupplier.setName("Atlas Supplies");

        SupplierDtoResponse response = new SupplierDtoResponse(
                1L,
                "Atlas Supplies");

        when(supplierMapper.toEntity(request))
                .thenReturn(supplier);

        when(supplierRepository.save(supplier))
                .thenReturn(savedSupplier);

        when(supplierMapper.toDto(savedSupplier))
                .thenReturn(response);

        // Act
        SupplierDtoResponse result = supplierService.createSupplier(request);

        // Assert
        assertNotNull(result);

        assertEquals(1L, result.id());
        assertEquals("Atlas Supplies", result.name());

        // Verify interactions
        verify(supplierMapper)
                .toEntity(request);

        verify(supplierRepository)
                .save(supplier);

        verify(supplierMapper)
                .toDto(savedSupplier);
    }

    // =========================================================
    // GET BY ID - SUCCESS
    // =========================================================

    @Test
    void shouldGetSupplierById() {

        // Arrange
        Long supplierId = 1L;

        Supplier supplier = new Supplier();
        supplier.setId(supplierId);
        supplier.setName("Atlas Supplies");

        SupplierDtoResponse response = new SupplierDtoResponse(
                supplierId,
                "Atlas Supplies");

        when(supplierRepository.findById(supplierId))
                .thenReturn(Optional.of(supplier));

        when(supplierMapper.toDto(supplier))
                .thenReturn(response);

        // Act
        SupplierDtoResponse result = supplierService.getSupplierById(supplierId);

        // Assert
        assertNotNull(result);

        assertEquals(1L, result.id());
        assertEquals(
                "Atlas Supplies",
                result.name());

        // Verify
        verify(supplierRepository)
                .findById(supplierId);

        verify(supplierMapper)
                .toDto(supplier);
    }

    // =========================================================
    // GET BY ID - NOT FOUND
    // =========================================================

    @Test
    void shouldThrowExceptionWhenSupplierDoesNotExist() {

        // Arrange
        Long supplierId = 999L;

        when(supplierRepository.findById(supplierId))
                .thenReturn(Optional.empty());

        // Act + Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.getSupplierById(supplierId));

        // Verify exception message
        assertEquals(
                "Supplier not found with id: " + supplierId,
                exception.getMessage());

        // Verify repository was called
        verify(supplierRepository)
                .findById(supplierId);

        // Mapper must NOT be called
        verify(supplierMapper, never())
                .toDto(any());
    }

    // =========================================================
    // GET ALL - SUCCESS
    // =========================================================

    @Test
    void shouldGetAllSuppliers() {

        // Arrange
        Supplier supplier1 = new Supplier();
        supplier1.setId(1L);
        supplier1.setName("Atlas Supplies");

        Supplier supplier2 = new Supplier();
        supplier2.setId(2L);
        supplier2.setName("Maroc Tech");

        SupplierDtoResponse response1 = new SupplierDtoResponse(
                1L,
                "Atlas Supplies");

        SupplierDtoResponse response2 = new SupplierDtoResponse(
                2L,
                "Maroc Tech");

        when(supplierRepository.findAll())
                .thenReturn(
                        List.of(
                                supplier1,
                                supplier2));

        when(supplierMapper.toDto(supplier1))
                .thenReturn(response1);

        when(supplierMapper.toDto(supplier2))
                .thenReturn(response2);

        // Act
        List<SupplierDtoResponse> result = supplierService.getAllSuppliers();

        // Assert
        assertNotNull(result);

        assertEquals(2, result.size());

        assertEquals(
                "Atlas Supplies",
                result.get(0).name());

        assertEquals(
                "Maroc Tech",
                result.get(1).name());

        // Verify
        verify(supplierRepository)
                .findAll();

        verify(supplierMapper)
                .toDto(supplier1);

        verify(supplierMapper)
                .toDto(supplier2);
    }

    // =========================================================
    // GET ALL - EMPTY
    // =========================================================

    @Test
    void shouldReturnEmptyListWhenNoSuppliersExist() {

        // Arrange
        when(supplierRepository.findAll())
                .thenReturn(List.of());

        // Act
        List<SupplierDtoResponse> result = supplierService.getAllSuppliers();

        // Assert
        assertNotNull(result);

        assertTrue(result.isEmpty());

        // Verify
        verify(supplierRepository)
                .findAll();
    }

    // =========================================================
    // UPDATE - SUCCESS
    // =========================================================

    @Test
    void shouldUpdateSupplier() {

        // Arrange
        Long supplierId = 1L;

        SupplierDtoRequest request = new SupplierDtoRequest(
                "Updated Supplies");

        Supplier existingSupplier = new Supplier();

        existingSupplier.setId(supplierId);
        existingSupplier.setName("Old Name");

        Supplier updatedSupplier = new Supplier();

        updatedSupplier.setId(supplierId);
        updatedSupplier.setName("Updated Supplies");

        SupplierDtoResponse response = new SupplierDtoResponse(
                supplierId,
                "Updated Supplies");

        when(supplierRepository.findById(supplierId))
                .thenReturn(
                        Optional.of(existingSupplier));

        /*
         * This assumes your mapper has an update method
         * similar to:
         *
         * updateEntity(existingSupplier, request)
         *
         * If your mapper does NOT have this method,
         * tell me your actual mapper code and we will
         * adapt this test.
         */

        when(supplierRepository.save(existingSupplier))
                .thenReturn(updatedSupplier);

        when(supplierMapper.toDto(updatedSupplier))
                .thenReturn(response);

        // Act
        SupplierDtoResponse result = supplierService.updateSupplier(
                supplierId,
                request);

        // Assert
        assertNotNull(result);

        assertEquals(
                supplierId,
                result.id());

        assertEquals(
                "Updated Supplies",
                result.name());

        // Verify
        verify(supplierRepository)
                .findById(supplierId);

        verify(supplierRepository)
                .save(existingSupplier);

        verify(supplierMapper)
                .toDto(updatedSupplier);
    }

    // =========================================================
    // UPDATE - NOT FOUND
    // =========================================================

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingSupplier() {

        // Arrange
        Long supplierId = 999L;

        SupplierDtoRequest request = new SupplierDtoRequest(
                "Test Supplier");

        when(supplierRepository.findById(supplierId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.updateSupplier(
                        supplierId,
                        request));

        // Verify
        verify(supplierRepository)
                .findById(supplierId);

        // Save must NOT happen
        verify(supplierRepository, never())
                .save(any());

        // Mapper should not convert anything
        verify(supplierMapper, never())
                .toDto(any());
    }

    // =========================================================
    // DELETE - SUCCESS
    // =========================================================

    @Test
    void shouldDeleteSupplier() {

        // Arrange
        Long supplierId = 1L;

        Supplier supplier = new Supplier();

        supplier.setId(supplierId);
        supplier.setName("Atlas Supplies");

        when(supplierRepository.findById(supplierId))
                .thenReturn(
                        Optional.of(supplier));

        // Act
        supplierService.deleteSupplier(supplierId);

        // Assert / Verify
        verify(supplierRepository)
                .findById(supplierId);

        verify(supplierRepository)
                .delete(supplier);
    }

    // =========================================================
    // DELETE - NOT FOUND
    // =========================================================

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingSupplier() {

        // Arrange
        Long supplierId = 999L;

        when(supplierRepository.findById(supplierId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> supplierService.deleteSupplier(supplierId));

        // Verify
        verify(supplierRepository)
                .findById(supplierId);

        // Delete must NOT happen
        verify(supplierRepository, never())
                .delete(any());
    }
}