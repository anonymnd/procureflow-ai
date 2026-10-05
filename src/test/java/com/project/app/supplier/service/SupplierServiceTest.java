package com.project.app.supplier.service;

import com.project.app.supplier.DTO.*;
import com.project.app.supplier.DTO.SupplierDtoResponse;
import com.project.app.supplier.model.Supplier;
import com.project.app.supplier.mapper.SupplierMapper;
import com.project.app.supplier.repository.SupplierRepo;
import com.project.app.common.exception.ResourceNotFoundException;

import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void shouldCreateSupplier() {

        SupplierDtoRequest request = new SupplierDtoRequest(
                "Atlas Supplies");

        Supplier supplier = new Supplier();

        Supplier savedSupplier = new Supplier();
        savedSupplier.setId(1L);
        savedSupplier.setName("Atlas Supplies");

        SupplierDtoResponse response = new SupplierDtoResponse(1, "Atlas Supplies");

        when(supplierMapper.toEntity(request))
                .thenReturn(supplier);

        when(supplierRepository.save(supplier))
                .thenReturn(savedSupplier);

        when(supplierMapper.toDto(savedSupplier))
                .thenReturn(response);

        SupplierDtoResponse result = supplierService.createSupplier(request);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Atlas Supplies", result.name());

        verify(supplierMapper).toEntity(request);
        verify(supplierRepository).save(supplier);
        verify(supplierMapper).toDto(savedSupplier);
    }

}
